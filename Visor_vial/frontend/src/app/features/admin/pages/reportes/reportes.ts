import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReportesService, ReporteResumen, Estadisticas, NuevoReporteDto, ViaSegmento } from '../../../../core/services/reportes.service';
import { ToastService } from '../../../../core/services/toast.service';

/** Pipe helper: devuelve el menor de dos números (usado en la paginación del pie) */
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'min', standalone: true })
export class MinPipe implements PipeTransform {
  transform(values: number[]): number {
    return Math.min(...values);
  }
}

@Component({
  selector: 'app-reportes',
  imports: [CommonModule, FormsModule, DecimalPipe, MinPipe],
  templateUrl: './reportes.html',
  styleUrl: './reportes.scss',
})
export class Reportes implements OnInit {
  private svc   = inject(ReportesService);
  private toast = inject(ToastService);

  // Estado de la lista
  reportes       = signal<ReporteResumen[]>([]);
  totalElementos = signal(0);
  totalPaginas   = signal(0);
  paginaActual   = signal(0);
  cargando       = signal(false);
  errorCarga     = signal(false);

  // Filtros
  filtroEstado    = '';
  filtroBusqueda  = '';
  tamanio         = 10;

  // Estadísticas
  estadisticas = signal<Estadisticas | null>(null);

  // Vías disponibles
  vias = signal<ViaSegmento[]>([]);

  // Modal — ver detalle (solo lectura)
  reporteVista        = signal<ReporteResumen | null>(null);

  // Modal — editar estado
  reporteSeleccionado = signal<ReporteResumen | null>(null);
  nuevoEstado         = signal('');
  guardandoEstado     = signal(false);

  // Modal — eliminar
  reporteAEliminar    = signal<ReporteResumen | null>(null);
  eliminando          = signal(false);

  // Modal — nuevo reporte
  mostrarModalNuevo   = signal(false);
  guardandoNuevo      = signal(false);
  previewEstado       = signal<'vacio' | 'cargando' | 'ok' | 'error'>('vacio');
  nuevoForm = {
    idVia:       null as number | null,
    imagenUrl:   '',
    descripcion: '',
  };

  /** Debounce timer para no disparar la preview en cada tecla */
  private previewTimer: ReturnType<typeof setTimeout> | null = null;

  readonly estadosOpciones = [
    { valor: '',           etiqueta: 'Todos los estados' },
    { valor: 'PENDIENTE',  etiqueta: 'Pendiente'         },
    { valor: 'EN_PROCESO', etiqueta: 'En proceso'        },
    { valor: 'RESUELTO',   etiqueta: 'Resuelto'          },
    { valor: 'DUPLICADO',  etiqueta: 'Duplicado'         },
  ];

  readonly estadosCambio = [
    { valor: 'PENDIENTE',  etiqueta: 'Pendiente'  },
    { valor: 'EN_PROCESO', etiqueta: 'En proceso' },
    { valor: 'RESUELTO',   etiqueta: 'Resuelto'   },
    { valor: 'DUPLICADO',  etiqueta: 'Duplicado'  },
  ];

  ngOnInit(): void {
    this.cargarEstadisticas();
    this.cargarReportes();
    this.cargarVias();
  }

  cargarEstadisticas(): void {
    this.svc.estadisticas().subscribe({
      next: (e) => this.estadisticas.set(e),
      error: () => {},
    });
  }

  cargarVias(): void {
    this.svc.listarVias().subscribe({
      next: (v) => this.vias.set(v),
      error: () => {},
    });
  }

  cargarReportes(pagina = 0): void {
    this.cargando.set(true);
    this.errorCarga.set(false);
    const estado = this.filtroEstado || undefined;
    const busqueda = this.filtroBusqueda || undefined;
    this.svc.listar(pagina, this.tamanio, estado, busqueda).subscribe({
      next: (p) => {
        this.reportes.set(p.contenido);
        this.totalElementos.set(p.totalElementos);
        this.totalPaginas.set(p.totalPaginas);
        this.paginaActual.set(p.paginaActual);
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
        this.errorCarga.set(true);
      },
    });
  }

  aplicarFiltro(): void {
    this.cargarReportes(0);
  }

  limpiarFiltros(): void {
    this.filtroEstado   = '';
    this.filtroBusqueda = '';
    this.tamanio        = 10;
    this.cargarReportes(0);
  }

  irAPagina(p: number): void {
    if (p >= 0 && p < this.totalPaginas()) {
      this.cargarReportes(p);
    }
  }

  // ── Modal Ver ─────────────────────────────────────────────────────────────
  abrirVista(r: ReporteResumen): void {
    this.reporteVista.set(r);
  }

  cerrarVista(): void {
    this.reporteVista.set(null);
  }

  // ── Modal Editar ──────────────────────────────────────────────────────────
  abrirDetalle(r: ReporteResumen): void {
    this.reporteSeleccionado.set(r);
    this.nuevoEstado.set(r.estado);
  }

  cerrarDetalle(): void {
    this.reporteSeleccionado.set(null);
  }

  guardarEstado(): void {
    const r = this.reporteSeleccionado();
    if (!r) return;
    this.guardandoEstado.set(true);
    this.svc.actualizarEstado(r.idReporte, this.nuevoEstado()).subscribe({
      next: (actualizado) => {
        this.guardandoEstado.set(false);
        this.toast.mostrar(
          `Reporte #${r.idReporte} actualizado a "${this.etiquetaEstado(actualizado.estado)}"`,
          true
        );
        this.cerrarDetalle();
        this.cargarReportes(this.paginaActual());
        this.cargarEstadisticas();
      },
      error: () => {
        this.guardandoEstado.set(false);
        this.toast.mostrar('No se pudo actualizar el estado', false);
      },
    });
  }

  // ── Modal Eliminar ────────────────────────────────────────────────────────
  confirmarEliminar(r: ReporteResumen): void {
    this.reporteAEliminar.set(r);
  }

  cancelarEliminar(): void {
    this.reporteAEliminar.set(null);
  }

  ejecutarEliminar(): void {
    const r = this.reporteAEliminar();
    if (!r) return;
    this.eliminando.set(true);
    this.svc.eliminar(r.idReporte).subscribe({
      next: () => {
        this.eliminando.set(false);
        this.toast.mostrar(`Reporte #${r.idReporte} eliminado`, true);
        this.cancelarEliminar();
        this.cargarReportes(this.paginaActual());
        this.cargarEstadisticas();
      },
      error: () => {
        this.eliminando.set(false);
        this.toast.mostrar('No se pudo eliminar el reporte', false);
      },
    });
  }

  // ── Modal Nuevo Reporte ───────────────────────────────────────────────────
  abrirModalNuevo(): void {
    this.nuevoForm = { idVia: null, imagenUrl: '', descripcion: '' };
    this.previewEstado.set('vacio');
    this.mostrarModalNuevo.set(true);
  }

  cerrarModalNuevo(): void {
    if (this.previewTimer) clearTimeout(this.previewTimer);
    this.mostrarModalNuevo.set(false);
  }

  onUrlChange(): void {
    if (this.previewTimer) clearTimeout(this.previewTimer);
    const url = this.nuevoForm.imagenUrl.trim();
    if (!url) { this.previewEstado.set('vacio'); return; }
    this.previewEstado.set('cargando');
    // Espera 600ms desde que dejó de escribir para cargar
    this.previewTimer = setTimeout(() => {
      const img = new Image();
      img.onload  = () => this.previewEstado.set('ok');
      img.onerror = () => this.previewEstado.set('error');
      img.src = url;
    }, 600);
  }

  limpiarUrlImagen(): void {
    this.nuevoForm.imagenUrl = '';
    this.previewEstado.set('vacio');
  }

  guardarNuevo(): void {
    if (!this.nuevoForm.imagenUrl.trim()) {
      this.toast.mostrar('La URL de imagen es obligatoria', false);
      return;
    }
    this.guardandoNuevo.set(true);
    const dto: NuevoReporteDto = {
      idUsuario:   1,
      idVia:       this.nuevoForm.idVia ?? undefined,
      imagenUrl:   this.nuevoForm.imagenUrl.trim(),
      descripcion: this.nuevoForm.descripcion.trim() || undefined,
    };
    this.svc.crear(dto).subscribe({
      next: (r) => {
        this.guardandoNuevo.set(false);
        this.toast.mostrar(`Reporte #${r.idReporte} creado correctamente`, true);
        this.cerrarModalNuevo();
        this.cargarReportes(0);
        this.cargarEstadisticas();
      },
      error: () => {
        this.guardandoNuevo.set(false);
        this.toast.mostrar('No se pudo crear el reporte', false);
      },
    });
  }

  exportarCsv(): void {
    const headers = ['ID', 'Descripcion', 'Estado', 'Via', 'Fecha'];
    const filas = this.reportes().map(r => [
      r.idReporte,
      `"${(r.descripcion ?? '').replace(/"/g, '""')}"`,
      r.estado,
      r.idVia ?? '',
      r.fechaCreacion,
    ]);
    const csv = [headers, ...filas].map(f => f.join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url  = URL.createObjectURL(blob);
    const a    = document.createElement('a');
    a.href     = url;
    a.download = `reportes_${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }

  onImgError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.style.display = 'none';
    const placeholder = document.createElement('div');
    placeholder.className = 'thumb-placeholder';
    placeholder.innerHTML = '<i class="bi bi-image text-muted"></i>';
    img.parentNode?.replaceChild(placeholder, img);
  }

  etiquetaEstado(estado: string): string {
    const mapa: Record<string, string> = {
      PENDIENTE:  'Pendiente',
      EN_PROCESO: 'En proceso',
      RESUELTO:   'Resuelto',
      DUPLICADO:  'Duplicado',
    };
    return mapa[estado] ?? estado;
  }

  paginasVisibles(): number[] {
    const total   = this.totalPaginas();
    const actual  = this.paginaActual();
    const ventana = 2;
    const inicio  = Math.max(0, actual - ventana);
    const fin     = Math.min(total - 1, actual + ventana);
    const arr: number[] = [];
    for (let i = inicio; i <= fin; i++) arr.push(i);
    return arr;
  }
}
