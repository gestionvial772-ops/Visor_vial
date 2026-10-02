import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ViasService } from '../../../../core/services/vias.service';

// Declarar mapboxgl como variable global (viene del CDN)
declare var mapboxgl: any;

@Component({
  selector: 'app-vias',
  imports: [CommonModule],
  templateUrl: './vias.html',
  styleUrl: './vias.scss',
})
export class Vias implements OnInit, OnDestroy {
  private map: any;
  private mapboxToken = 'pk.eyJ1IjoiaGFsZm9uc28xMjM0NSIsImEiOiJjbW8wYnJ1dncwNm5lMndweWttbTNhYmg2In0.LC3mGJP8ib3exVL-VGZyyg';

  constructor(private viasService: ViasService) {}

  ngOnInit() {
    // Esperar un momento para que el DOM esté listo
    setTimeout(() => {
      this.inicializarMapa();
    }, 100);
  }

  ngOnDestroy() {
    // Limpiar el mapa al destruir el componente
    if (this.map) {
      this.map.remove();
    }
  }

  inicializarMapa() {
    // Verificar que mapboxgl esté disponible
    if (typeof mapboxgl === 'undefined') {
      console.error('❌ Mapbox GL JS no está cargado');
      return;
    }

    // Configurar token
    mapboxgl.accessToken = this.mapboxToken;

    // Crear el mapa
    this.map = new mapboxgl.Map({
      container: 'mapbox-container',
      style: 'mapbox://styles/mapbox/satellite-v9',
      center: [-79.029, -8.112], // Trujillo, Perú
      zoom: 12,
      pitch: 0,
      bearing: 0
    });

    // Agregar controles de navegación
    this.map.addControl(new mapboxgl.NavigationControl(), 'top-left');
    this.map.addControl(new mapboxgl.ScaleControl({ maxWidth: 100, unit: 'metric' }), 'bottom-left');
    this.map.addControl(new mapboxgl.FullscreenControl(), 'top-left');

    // Actualizar coordenadas al mover el mouse
    this.map.on('mousemove', (e: any) => {
      const coordsDisplay = document.getElementById('coords-display');
      if (coordsDisplay) {
        coordsDisplay.textContent = `Lat: ${e.lngLat.lat.toFixed(6)}, Lng: ${e.lngLat.lng.toFixed(6)}`;
      }
    });

    // Evento cuando el mapa se carga
    this.map.on('load', () => {
      console.log('✅ Mapa Mapbox cargado correctamente');
      this.cargarViasDesdeBackend();
    });

    console.log('🗺️ Mapa inicializado');
  }

  cambiarMapaBase(event: any) {
    const value = event.target.value;
    
    const estilos: any = {
      satellite: 'mapbox://styles/mapbox/satellite-v9',
      streets: 'mapbox://styles/mapbox/streets-v12',
      light: 'mapbox://styles/mapbox/light-v11'
    };

    if (this.map && estilos[value]) {
      // Guardar las fuentes actuales antes de cambiar el estilo
      const viasSource = this.map.getSource('vias-source');
      const viasData = viasSource ? viasSource._data : null;

      this.map.setStyle(estilos[value]);
      
      // Restaurar las capas después de que el estilo se cargue
      this.map.once('style.load', () => {
        if (viasData) {
          this.agregarCapaVias(viasData);
        }
      });

      console.log(`🗺️ Mapa cambiado a: ${value}`);
    }
  }

  cargarViasDesdeBackend() {
    console.log('📍 Cargando vías desde la base de datos...');

    this.viasService.obtenerViasGeoJson().subscribe({
      next: (geoJson) => {
        console.log('✅ Vías cargadas:', geoJson);
        
        if (geoJson && geoJson.features && geoJson.features.length > 0) {
          this.agregarCapaVias(geoJson);
          
          // Hacer zoom a la primera vía
          const primeraVia = geoJson.features[0];
          if (primeraVia.geometry && primeraVia.geometry.coordinates) {
            const coords = primeraVia.geometry.coordinates;
            // Si es LineString, tomar el primer punto
            if (Array.isArray(coords[0])) {
              this.map.flyTo({
                center: coords[0],
                zoom: 14,
                duration: 2000
              });
            }
          }

          // Actualizar el texto de "Cargando vías..."
          const listElement = document.querySelector('.vias-grid p');
          if (listElement) {
            listElement.textContent = `${geoJson.features.length} vía(s) encontrada(s)`;
          }
        } else {
          console.warn('⚠️ No se encontraron vías con geometría');
          const listElement = document.querySelector('.vias-grid p');
          if (listElement) {
            listElement.textContent = 'No hay vías con geometría en la base de datos';
          }
        }
      },
      error: (error) => {
        console.error('❌ Error al cargar vías:', error);
        const listElement = document.querySelector('.vias-grid p');
        if (listElement) {
          listElement.textContent = 'Error al cargar vías';
        }
      }
    });
  }

  agregarCapaVias(geoJson: any) {
    // Si ya existe la capa, eliminarla primero
    if (this.map.getLayer('vias-layer')) {
      this.map.removeLayer('vias-layer');
    }
    if (this.map.getLayer('vias-layer-highlight')) {
      this.map.removeLayer('vias-layer-highlight');
    }
    if (this.map.getSource('vias-source')) {
      this.map.removeSource('vias-source');
    }

    // Agregar source
    this.map.addSource('vias-source', {
      type: 'geojson',
      data: geoJson
    });

    // Agregar capa de línea principal
    this.map.addLayer({
      id: 'vias-layer',
      type: 'line',
      source: 'vias-source',
      paint: {
        'line-color': '#FF5733',
        'line-width': 4
      }
    });

    // Agregar capa de resaltado (borde)
    this.map.addLayer({
      id: 'vias-layer-highlight',
      type: 'line',
      source: 'vias-source',
      paint: {
        'line-color': '#FFFFFF',
        'line-width': 6,
        'line-opacity': 0.5
      }
    });

    // Agregar popup al hacer clic
    this.map.on('click', 'vias-layer', (e: any) => {
      const feature = e.features[0];
      const props = feature.properties;

      new mapboxgl.Popup()
        .setLngLat(e.lngLat)
        .setHTML(`
          <div style="padding: 8px;">
            <h4 style="margin: 0 0 8px 0; color: #2c5f2d;">${props.nombreVia || 'Sin nombre'}</h4>
            <p style="margin: 4px 0; font-size: 0.9em;"><strong>Código:</strong> ${props.codigoSegmento || 'N/A'}</p>
            <p style="margin: 4px 0; font-size: 0.9em;"><strong>ID:</strong> ${props.idVia || 'N/A'}</p>
          </div>
        `)
        .addTo(this.map);
    });

    // Cambiar cursor al pasar por encima
    this.map.on('mouseenter', 'vias-layer', () => {
      this.map.getCanvas().style.cursor = 'pointer';
    });

    this.map.on('mouseleave', 'vias-layer', () => {
      this.map.getCanvas().style.cursor = '';
    });

    console.log('✅ Capa de vías agregada al mapa');
  }

  agregarNuevaVia() {
    console.log('➕ Agregar nueva vía');
    alert('Funcionalidad en desarrollo: Agregar nueva vía');
  }
}
