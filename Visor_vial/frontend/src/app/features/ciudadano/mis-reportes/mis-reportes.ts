import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReportesService, ReporteResumen } from '../../../core/services/reportes.service';

@Component({
  selector: 'app-mis-reportes',
  imports: [CommonModule],
  templateUrl: './mis-reportes.html',
  styleUrl: './mis-reportes.scss',
})
export class MisReportes implements OnInit {
  private reportesService = inject(ReportesService);

  reportes = signal<ReporteResumen[]>([]);
  cargando = signal(false);
  errorCarga = signal(false);
  imagenes = signal<Record<number, string>>({});

  // Modal ver detalle
  reporteSeleccionado = signal<ReporteResumen | null>(null);
  imagenUrl = signal<string | null>(null);
  cargandoDetalle = signal(false);

  ngOnInit(): void {
    this.cargarMisReportes();
  }

  cargarMisReportes(): void {
    this.cargando.set(true);
    this.errorCarga.set(false);

    this.reportesService.listarMios().subscribe({
      next: (pagina) => {
        this.reportes.set(pagina.contenido);
        this.imagenes.set({});
        pagina.contenido.forEach(reporte => {
          this.reportesService.imagenPropia(reporte.idReporte).subscribe({
            next: resultado => this.imagenes.update(actual => ({
              ...actual, [reporte.idReporte]: resultado.url,
            })),
            error: () => {},
          });
        });
        this.cargando.set(false);
      },
      error: () => {
        this.errorCarga.set(true);
        this.cargando.set(false);
      },
    });
  }

  abrirDetalle(reporte: ReporteResumen): void {
    this.reporteSeleccionado.set(reporte);
    this.imagenUrl.set(null);
    this.cargandoDetalle.set(true);

    // Cargar imagen
    this.reportesService.imagenPropia(reporte.idReporte).subscribe({
      next: (resultado) => {
        if (this.reporteSeleccionado()?.idReporte === reporte.idReporte) {
          this.imagenUrl.set(resultado.url);
          this.cargandoDetalle.set(false);
        }
      },
      error: () => {
        if (this.reporteSeleccionado()?.idReporte === reporte.idReporte) {
          this.imagenUrl.set(null);
          this.cargandoDetalle.set(false);
        }
      },
    });

  }

  cerrarDetalle(): void {
    this.reporteSeleccionado.set(null);
    this.imagenUrl.set(null);
  }

  etiquetaEstado(estado: string): string {
    const mapa: Record<string, string> = {
      PENDIENTE: 'Pendiente',
      EN_PROCESO: 'En proceso',
      RESUELTO: 'Resuelto',
      DUPLICADO: 'Duplicado',
    };
    return mapa[estado] ?? estado;
  }

  colorEstado(estado: string): string {
    const colores: Record<string, string> = {
      PENDIENTE: '#fbbf24',
      EN_PROCESO: '#3b82f6',
      RESUELTO: '#22c55e',
      DUPLICADO: '#9ca3af',
    };
    return colores[estado] ?? '#6b7280';
  }

}
