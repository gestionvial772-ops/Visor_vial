import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReportesService, ReporteResumen, Estadisticas, NuevoReporteDto, ViaSegmento } from '../../../../core/services/reportes.service';
import { AnalisisService } from '../../../../core/services/analisis.service';
import { AnalisisIa, CoordenadaReporte } from '../../../../core/models/reporte.model';
import { ToastService } from '../../../../core/services/toast.service';
import { InfoTip } from '../../../../shared/components/info-tip/info-tip';

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
  imports: [CommonModule, FormsModule, DecimalPipe, MinPipe, InfoTip],
  templateUrl: './reportes.html',
  styleUrl: './reportes.scss',
})
export class Reportes implements OnInit {
  private svc   = inject(ReportesService);
  private toast = inject(ToastService);
  private analisisSvc = inject(AnalisisService);

  // Estado de la lista
  reportes       = signal<ReporteResumen[]>([]);
  totalElementos = signal(0);
  totalPaginas   = signal(0);
  paginaActual   = signal(0);
  cargando       = signal(false);
  errorCarga     = signal(false);
  mensajeErrorCarga = signal('');

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

  // Panel de análisis de IA
  analisis            = signal<AnalisisIa | null>(null);
  coordenadaDetalle   = signal<CoordenadaReporte | null>(null);
  cargandoAnalisis    = signal(false);
  reanalizando        = signal(false);
  errorAnalisis       = signal(false);

  /**
   * URLs firmadas de las imágenes indexadas por idReporte. El bucket de
   * Supabase Storage es privado, asi que la ruta guardada en imagen_url no la
   * puede cargar el navegador. Debe ser un signal: si fuera un Map comun,
   * Angular no detectaria el cambio cuando llega la URL y las miniaturas
   * quedarian en blanco hasta que otra accion dispare el render.
   */
  private readonly imagenes = signal<Record<number, string>>({});

  /** URL lista para el <img> de un reporte, o null si aún no se resolvió. */
  urlImagen(idReporte: number): string | null {
    return this.imagenes()[idReporte] ?? null;
  }

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
        p.contenido.forEach((r) => this.resolverImagen(r.idReporte));
      },
      error: (error) => {
        this.cargando.set(false);
        this.errorCarga.set(true);
        this.mensajeErrorCarga.set(
          error.status === 401 ? 'Tu sesión venció. Vuelve a iniciar sesión.' :
          error.status === 403 ? 'Tu cuenta no tiene permiso para consultar todos los reportes. Se requiere el rol ADMIN.' :
          error.status === 0 ? 'No se pudo conectar con el servidor. Comprueba que el backend esté encendido.' :
          'El servidor no pudo cargar los reportes. Intenta nuevamente.'
        );
      },
    });
  }

  /**
   * Pide al backend la URL firmada de cada imagen. Los reportes creados con una
   * URL publica ya funcionan sin esto, pero los subida a Storage no.
   */
  private resolverImagen(idReporte: number): void {
    if (idReporte in this.imagenes()) {
      return;
    }

    this.analisisSvc.obtenerImagen(idReporte).subscribe({
      next: (r) =>
        this.imagenes.update((mapa) => ({ ...mapa, [idReporte]: r.url })),
      error: () =>
        this.imagenes.update((mapa) => ({ ...mapa, [idReporte]: '' })),
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
    this.cargarAnalisis(r.idReporte);
    this.resolverImagen(r.idReporte);
  }

  cerrarVista(): void {
    this.reporteVista.set(null);
    this.analisis.set(null);
    this.coordenadaDetalle.set(null);
    this.errorAnalisis.set(false);
  }

  /** URL a usar en el <img>: la firmada si se pudo resolver, si no la original. */
  srcImagen(reporte: ReporteResumen | null): string | null {
    if (!reporte) {
      return null;
    }

    const firmadas = this.imagenes();

    if (reporte.idReporte in firmadas) {
      const firmada = firmadas[reporte.idReporte];
      return firmada ? firmada : reporte.imagenUrl;
    }

    // los reportes con URL publica funcionan sin pedir la firma
    return reporte.imagenUrl?.startsWith('http') ? reporte.imagenUrl : null;
  }

  private cargarAnalisis(idReporte: number): void {
    this.analisis.set(null);
    this.coordenadaDetalle.set(null);
    this.errorAnalisis.set(false);
    this.cargandoAnalisis.set(true);

    this.analisisSvc.obtenerDetalle(idReporte).subscribe({
      next: (detalle) => {
        this.analisis.set(detalle.analisis);
        this.coordenadaDetalle.set(detalle.coordenada);
        this.cargandoAnalisis.set(false);
      },
      error: (err) => {
        console.error('No se pudo cargar el detalle del reporte:', err);

        // Sin esto el panel cae en el estado "Sin analizar", que miente:
        // el reporte puede estar analizado y solo fallo la lectura.
        this.analisis.set(null);
        this.errorAnalisis.set(true);
        this.cargandoAnalisis.set(false);
      },
    });
  }

  reanalizar(): void {
    const r = this.reporteVista();
    if (!r || this.reanalizando()) {
      return;
    }

    this.reanalizando.set(true);

    this.analisisSvc.reanalizar(r.idReporte).subscribe({
      next: () => {
        this.reanalizando.set(false);
        this.toast.mostrar(
          `Reporte #${r.idReporte} analizado`,
          true,
          'Análisis completado',
        );
        this.cargarAnalisis(r.idReporte);
      },
      error: (err) => {
        this.reanalizando.set(false);

        // La causa tecnica (respuesta cruda de Gemini, finishReason, cuerpo JSON)
        // se queda en consola: en pantalla solo va un mensaje accionable.
        console.error('Fallo el reanálisis con IA:', err);

        this.toast.mostrar(
          this.mensajeErrorAnalisis(err),
          false,
          'Falló el análisis',
        );
      },
    });
  }

  claseSeveridad(severidad: number): string {
    return `sev-${severidad}`;
  }

  /**
   * Traduce el fallo del reanálisis a un mensaje que el admin pueda actuar.
   * El detalle técnico va a consola; mostrarlo en el toast solo confunde.
   */
  private mensajeErrorAnalisis(err: unknown): string {
    const status = (err as { status?: number })?.status;
    const backend = (err as { error?: { mensaje?: string } })?.error?.mensaje;

    switch (status) {
      case 0:
        return 'No hay conexión con el servidor. Revisa tu red e inténtalo de nuevo.';

      case 404:
        return 'El reporte ya no existe. Cierra esta ventana y actualiza la lista.';

      case 400:
        return backend || 'El reporte no tiene una imagen válida para analizar.';

      case 429:
        return 'Se alcanzó la cuota de análisis del día. Vuelve a intentarlo mañana.';

      case 502:
      case 503:
      case 504:
        return 'El servicio de análisis no responde. Inténtalo en unos minutos.';

      default:
        return backend || 'No se pudo completar el análisis con IA.';
    }
  }

  // ── Modal Editar ──────────────────────────────────────────────────────────
  abrirDetalle(r: ReporteResumen): void {
    this.reporteSeleccionado.set(r);
    this.nuevoEstado.set(r.estado);
    this.resolverImagen(r.idReporte);
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
          true,
          'Estado actualizado'
        );
        this.cerrarDetalle();
        this.cargarReportes(this.paginaActual());
        this.cargarEstadisticas();
      },
      error: () => {
        this.guardandoEstado.set(false);
        this.toast.mostrar('No se pudo actualizar el estado', false, 'No se pudo guardar');
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
        this.toast.mostrar(`Reporte #${r.idReporte} anulado`, true, 'Reporte anulado');
        this.cancelarEliminar();
        this.cargarReportes(this.paginaActual());
        this.cargarEstadisticas();
      },
      error: (err) => {
        this.eliminando.set(false);
        this.toast.mostrar(
          err?.error?.mensaje || 'No se pudo anular el reporte',
          false,
          'No se pudo anular',
        );
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
      this.toast.mostrar('La URL de imagen es obligatoria', false, 'Faltan datos');
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
        this.toast.mostrar(
          `Reporte #${r.idReporte} creado correctamente`,
          true,
          'Reporte creado'
        );
        this.cerrarModalNuevo();
        this.cargarReportes(0);
        this.cargarEstadisticas();
      },
      error: () => {
        this.guardandoNuevo.set(false);
        this.toast.mostrar('No se pudo crear el reporte', false, 'No se pudo crear');
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
