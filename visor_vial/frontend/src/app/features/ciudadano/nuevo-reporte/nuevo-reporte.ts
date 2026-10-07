import { CommonModule } from '@angular/common';
import { Component, OnInit, AfterViewInit, OnDestroy, inject, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NuevoReporteService, ViaCercana } from './nuevo-reporte.service';
import { CrearReporteRequest } from '../../../core/models/reporte.model';
import { finalize } from 'rxjs';

// Declarar mapboxgl como variable global (viene del CDN)
declare var mapboxgl: any;

@Component({
  imports: [CommonModule, ReactiveFormsModule],
  selector: 'app-nuevo-reporte',
  styleUrl: './nuevo-reporte.scss',
  templateUrl: './nuevo-reporte.html',
})
export class NuevoReporte implements OnInit, AfterViewInit, OnDestroy {
  private readonly cdr = inject(ChangeDetectorRef);

  private readonly fb = inject(FormBuilder);

  private readonly nuevoReporteService = inject(NuevoReporteService);

  readonly formulario = this.fb.group({
    descripcion: ['', [Validators.maxLength(1000)]],
  });

  instanteInicioCaptura = '';

  latitud: number | null = null;
  longitud: number | null = null;
  precisionReportadaMetros: number | null = null;

  archivoSeleccionado: File | null = null;
  imagenPrevisualizacion: string | null = null;

  obteniendoUbicacion = false;
  enviando = false;
  reporteEnviado = false; // Nueva bandera

  mensajeError = '';
  mensajeExito = '';
  errorUbicacion = '';

  // Nuevas propiedades para vías cercanas
  viasCercanas: ViaCercana[] = [];
  viaSeleccionada: ViaCercana | null = null;
  buscandoVias = false;
  errorVias = '';

  // Propiedades del mapa
  private map: any = null;
  private readonly mapboxToken = 'pk.eyJ1IjoiaGFsZm9uc28xMjM0NSIsImEiOiJjbW8wYnJ1dncwNm5lMndweWttbTNhYmg2In0.LC3mGJP8ib3exVL-VGZyyg';
  private marcadorUsuario: any = null;

  ngOnInit(): void {
    this.instanteInicioCaptura = new Date().toISOString();
    this.obtenerUbicacion();
  }

  ngAfterViewInit(): void {
    // El mapa se inicializará después de obtener ubicación y vías
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
    }
  }

  obtenerUbicacion(): void {
    this.errorUbicacion = '';

    if (!navigator.geolocation) {
      this.errorUbicacion = 'Tu navegador no permite obtener la ubicacion';
      return;
    }

    this.obteniendoUbicacion = true;
    console.log('Solicitando ubicación al navegador...');

    navigator.geolocation.getCurrentPosition(
      (position) => {
        console.log('UBICACION OBTENIDA: ', position);
        this.latitud = position.coords.latitude;
        this.longitud = position.coords.longitude;
        this.precisionReportadaMetros = position.coords.accuracy;
        this.obteniendoUbicacion = false;
        this.errorUbicacion = '';

        // Buscar vías cercanas automáticamente
        this.buscarViasCercanas();

        this.cdr.detectChanges();
      },
      (error) => {
        console.error('Error de geolocalizacion: ', error);

        this.obteniendoUbicacion = false;
        this.errorUbicacion = this.obtenerMensajeErrorUbicacion(error);
        this.cdr.detectChanges();
      },

      {
        enableHighAccuracy: true,
        timeout: 15000,
        maximumAge: 0,
      },
    );
  }

  buscarViasCercanas(): void {
    if (this.latitud === null || this.longitud === null) {
      return;
    }

    this.buscandoVias = true;
    this.errorVias = '';
    this.viasCercanas = [];
    this.viaSeleccionada = null;

    console.log('Buscando vías cercanas...');

    this.nuevoReporteService
      .obtenerViasCercanas(this.latitud, this.longitud)
      .pipe(
        finalize(() => {
          this.buscandoVias = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: (vias) => {
          console.log('Vías cercanas encontradas:', vias);
          this.viasCercanas = vias;
          
          // Si solo hay una vía, seleccionarla automáticamente
          if (vias.length === 1) {
            this.seleccionarVia(vias[0]);
          }
          
          this.errorVias = '';

          // Inicializar mapa después de obtener las vías
          setTimeout(() => this.inicializarMapa(), 100);
          
          this.cdr.detectChanges();
        },
        error: (error) => {
          console.error('Error al buscar vías cercanas:', error);
          this.errorVias = 'No se pudieron cargar las vías cercanas. Puedes continuar sin seleccionar una vía.';
          this.cdr.detectChanges();
        },
      });
  }

  seleccionarVia(via: ViaCercana): void {
    this.viaSeleccionada = via;
    console.log('Vía seleccionada:', via);

    // Actualizar el mapa si existe
    if (this.map && this.map.getLayer('via-seleccionada')) {
      this.map.setFilter('via-seleccionada', ['==', ['get', 'idVia'], via.idVia]);
    }

    this.cdr.detectChanges();
  }

  inicializarMapa(): void {
    if (!this.latitud || !this.longitud || this.viasCercanas.length === 0) {
      console.log('No se puede inicializar mapa: faltan datos');
      return;
    }

    if (typeof mapboxgl === 'undefined') {
      console.error('❌ Mapbox GL JS no está cargado');
      return;
    }

    // Si el mapa ya existe, destruirlo primero
    if (this.map) {
      this.map.remove();
    }

    console.log('🗺️ Inicializando mapa...');
    mapboxgl.accessToken = this.mapboxToken;

    this.map = new mapboxgl.Map({
      container: 'mapa-vias-cercanas',
      style: 'mapbox://styles/mapbox/streets-v12',
      center: [this.longitud, this.latitud],
      zoom: 17,
    });

    // Agregar controles de navegación
    this.map.addControl(new mapboxgl.NavigationControl(), 'top-right');

    // Cuando el mapa cargue
    this.map.on('load', () => {
      console.log('✅ Mapa cargado');

      // Marcador rojo para el usuario
      this.marcadorUsuario = new mapboxgl.Marker({ color: '#FF0000' })
        .setLngLat([this.longitud!, this.latitud!])
        .setPopup(new mapboxgl.Popup().setHTML('<strong>Tu ubicación</strong>'))
        .addTo(this.map);

      // Cargar vías en el mapa
      this.cargarViasEnMapa();
    });
  }

  cargarViasEnMapa(): void {
    const idsVias = this.viasCercanas.map(v => v.idVia);

    console.log('📍 Cargando geometrías de vías:', idsVias);

    this.nuevoReporteService.obtenerViasGeoJson().subscribe({
      next: (geoJson: any) => {
        // Filtrar solo las vías cercanas
        const viasFiltradas = {
          type: 'FeatureCollection',
          features: geoJson.features.filter((f: any) =>
            idsVias.includes(f.properties.idVia)
          ),
        };

        console.log('✅ Vías filtradas:', viasFiltradas.features.length);

        // Agregar source
        this.map.addSource('vias-cercanas', {
          type: 'geojson',
          data: viasFiltradas,
        });

        // Capa de vías normales (azul)
        this.map.addLayer({
          id: 'vias-layer',
          type: 'line',
          source: 'vias-cercanas',
          paint: {
            'line-color': '#3b82f6',
            'line-width': 6,
            'line-opacity': 0.8,
          },
        });

        // Capa de vía seleccionada (verde)
        this.map.addLayer({
          id: 'via-seleccionada',
          type: 'line',
          source: 'vias-cercanas',
          paint: {
            'line-color': '#22c55e',
            'line-width': 8,
            'line-opacity': 1,
          },
          filter: ['==', ['get', 'idVia'], -1], // Inicialmente ninguna
        });

        // Click en vía
        this.map.on('click', 'vias-layer', (e: any) => {
          const feature = e.features[0];
          const idVia = feature.properties.idVia;

          const via = this.viasCercanas.find(v => v.idVia === idVia);
          if (via) {
            this.seleccionarVia(via);
          }
        });

        // Cursor pointer al hover
        this.map.on('mouseenter', 'vias-layer', () => {
          this.map.getCanvas().style.cursor = 'pointer';
        });

        this.map.on('mouseleave', 'vias-layer', () => {
          this.map.getCanvas().style.cursor = '';
        });
      },
      error: (error) => {
        console.error('❌ Error al cargar GeoJSON:', error);
      },
    });
  }

  seleccionarImagen(event: Event): void {
    const input = event.target as HTMLInputElement;
    const archivo = input.files?.[0];

    if (!archivo) {
      return;
    }

    if (!archivo.type.startsWith('image/')) {
      this.mensajeError = 'El archivo debe ser una imagen';
      input.value = '';
      this.cdr.detectChanges();
      return;
    }

    this.archivoSeleccionado = archivo;
    this.mensajeError = '';

    const reader = new FileReader();
    reader.onload = () => {
      this.imagenPrevisualizacion = reader.result as string;

      this.cdr.detectChanges();
    };
    reader.readAsDataURL(archivo);
  }

  enviarReporte(): void {
    this.mensajeError = '';
    this.mensajeExito = '';

    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }

    if (!this.archivoSeleccionado) {
      this.mensajeError = 'Debes seleccionar una fotografia del deterioro';
      return;
    }
    if (this.latitud === null || this.longitud === null) {
      this.mensajeError = 'No se pudo obtener la ubicacion. Intenta nuevamente';
      return;
    }

    const request: CrearReporteRequest = {
      idVia: this.viaSeleccionada?.idVia ?? null,
      descripcion: this.formulario.value.descripcion ?? '',
      latitud: this.latitud,
      longitud: this.longitud,
      precisionReportadaMetros: this.precisionReportadaMetros,
      instanteInicioCaptura: this.instanteInicioCaptura,
    };

    this.enviando = true;
    this.cdr.detectChanges();
    console.log('Enviando reporte', request);

    this.nuevoReporteService
      .crearReporte(request, this.archivoSeleccionado)
      .pipe(
        finalize(() => {
          this.enviando = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: (respuesta) => {
          console.log('Reporte registrado correctamente:', respuesta);

          this.mensajeExito =
            'Reporte registrado. El personal municipal lo revisará.';
          this.mensajeError = '';
          this.reporteEnviado = true; // Marcar como enviado
          this.cdr.detectChanges();
        },

        error: (error) => {
          console.error('Error al registrar el reporte:', error);

          this.mensajeError =
            'No fue posible registrar el reporte. Inténtalo nuevamente.';
          this.mensajeExito = '';
          this.cdr.detectChanges();
        },
      });
  }

  reiniciarFormulario(): void {
    // Limpiar formulario
    this.formulario.reset();
    this.archivoSeleccionado = null;
    this.imagenPrevisualizacion = null;

    // Limpiar mensajes
    this.mensajeExito = '';
    this.mensajeError = '';
    this.reporteEnviado = false;

    // Limpiar vías
    this.viasCercanas = [];
    this.viaSeleccionada = null;

    // Destruir mapa si existe
    if (this.map) {
      this.map.remove();
      this.map = null;
    }

    // Reiniciar timestamp
    this.instanteInicioCaptura = new Date().toISOString();

    // Obtener nueva ubicación
    this.obtenerUbicacion();

    this.cdr.detectChanges();
  }

  private obtenerMensajeErrorUbicacion(error: GeolocationPositionError): string {
    switch (error.code) {
      case error.PERMISSION_DENIED:
        return 'El permiso de ubicación fue rechazado. Habilítalo en la configuración del navegador.';

      case error.POSITION_UNAVAILABLE:
        return 'El dispositivo no pudo determinar tu ubicación actual.';

      case error.TIMEOUT:
        return 'La obtención de la ubicación tardó demasiado. Inténtalo nuevamente.';

      default:
        return 'Ocurrió un problema al obtener tu ubicación.';
    }
  }
}
