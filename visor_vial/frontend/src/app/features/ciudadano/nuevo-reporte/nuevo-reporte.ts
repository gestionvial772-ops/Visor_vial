import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NuevoReporteService } from './nuevo-reporte.service';
import { CrearReporteRequest } from '../../../core/models/reporte.model';
import { finalize } from 'rxjs';

@Component({
  imports: [CommonModule, ReactiveFormsModule],
  selector: 'app-nuevo-reporte',
  styleUrl: './nuevo-reporte.scss',
  templateUrl: './nuevo-reporte.html',
})
export class NuevoReporte implements OnInit {
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

  mensajeError = '';
  mensajeExito = '';
  errorUbicacion = '';

  ngOnInit(): void {
    this.instanteInicioCaptura = new Date().toISOString();
    this.obtenerUbicacion();
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
      idVia: null,
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

          this.mensajeExito = 'El reporte fue registrado correctamente.';

          this.mensajeError = '';

          this.cdr.detectChanges();
        },

        error: (error) => {
          console.error('Error al registrar el reporte:', error);

          this.mensajeError = 'No fue posible registrar el reporte. Inténtalo nuevamente.';

          this.mensajeExito = '';

          this.cdr.detectChanges();
        },
      });
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
