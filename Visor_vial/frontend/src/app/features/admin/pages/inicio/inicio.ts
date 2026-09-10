import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ReportesService, Estadisticas, ReporteResumen } from '../../../../core/services/reportes.service';
import { SessionService } from '../../../../core/services/session.service';

@Component({
  selector: 'app-inicio',
  imports: [CommonModule, RouterLink, DatePipe],
  templateUrl: './inicio.html',
  styleUrl: './inicio.scss',
})
export class Inicio implements OnInit {
  private svc     = inject(ReportesService);
  session         = inject(SessionService);

  estadisticas    = signal<Estadisticas | null>(null);
  recientes       = signal<ReporteResumen[]>([]);
  cargando        = signal(true);
  horaActual      = signal(new Date());

  readonly modulos = [
    {
      ruta: '/admin/reportes',
      icono: 'bi-exclamation-triangle-fill',
      titulo: 'Reportes',
      desc: 'Gestioná y actualizá el estado de las incidencias viales.',
      color: 'reportes',
    },
    {
      ruta: '/admin/vias',
      icono: 'bi-signpost-2-fill',
      titulo: 'Vías',
      desc: 'Consultá el catálogo de segmentos viales registrados.',
      color: 'vias',
    },
    {
      ruta: '/admin/usuarios',
      icono: 'bi-people-fill',
      titulo: 'Usuarios',
      desc: 'Administrá las cuentas y roles del sistema.',
      color: 'usuarios',
    },
  ];

  readonly accesosRapidos = [
    { ruta: '/admin/reportes', icono: 'bi-plus-circle', label: 'Nuevo reporte', color: 'acceso--verde' },
    { ruta: '/admin/reportes', icono: 'bi-funnel', label: 'Filtrar reportes', color: 'acceso--azul' },
    { ruta: '/admin/vias',     icono: 'bi-map', label: 'Ver mapa de vías', color: 'acceso--naranja' },
  ];

  ngOnInit(): void {
    this.svc.estadisticas().subscribe({
      next: (e) => { this.estadisticas.set(e); },
      error: () => {},
    });
    this.svc.listar(0, 4).subscribe({
      next: (p) => { this.recientes.set(p.contenido); this.cargando.set(false); },
      error: () => { this.cargando.set(false); },
    });

    // Reloj en vivo
    setInterval(() => this.horaActual.set(new Date()), 60000);
  }

  get saludo(): string {
    const h = this.horaActual().getHours();
    if (h < 12) return 'Buenos días';
    if (h < 19) return 'Buenas tardes';
    return 'Buenas noches';
  }

  get porcentajeAtencion(): number {
    const e = this.estadisticas();
    if (!e || e.total === 0) return 0;
    return Math.round(((e.resueltos + e.enProceso) / e.total) * 100);
  }

  get reporteMasReciente(): ReporteResumen | null {
    const r = this.recientes();
    return r.length > 0 ? r[0] : null;
  }

  get tiempoUltimoReporte(): string {
    const r = this.reporteMasReciente;
    if (!r) return 'Sin actividad';
    const diff = Date.now() - new Date(r.fechaCreacion).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 1) return 'Hace un momento';
    if (mins < 60) return `Hace ${mins} min`;
    const hrs = Math.floor(mins / 60);
    if (hrs < 24) return `Hace ${hrs}h`;
    const dias = Math.floor(hrs / 24);
    return `Hace ${dias}d`;
  }

  etiquetaEstado(estado: string): string {
    const m: Record<string, string> = {
      PENDIENTE: 'Pendiente', EN_PROCESO: 'En proceso', RESUELTO: 'Resuelto', DUPLICADO: 'Duplicado',
    };
    return m[estado] ?? estado;
  }
}
