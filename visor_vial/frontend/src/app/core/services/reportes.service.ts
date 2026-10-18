import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ReporteResumen {
  idReporte: number;
  idUsuario: number;
  idVia: number | null;
  nombreVia?: string | null;
  codigoSegmento?: string | null;
  imagenUrl: string;
  descripcion: string | null;
  estado: 'PENDIENTE' | 'EN_PROCESO' | 'RESUELTO' | 'DUPLICADO';
  fechaCreacion: string;
}

export interface PaginaDto<T> {
  contenido: T[];
  paginaActual: number;
  totalPaginas: number;
  totalElementos: number;
}

export interface Estadisticas {
  total: number;
  pendientes: number;
  enProceso: number;
  resueltos: number;
  duplicados: number;
}

export interface NuevoReporteDto {
  idUsuario?: number;
  idVia?: number | null;
  imagenUrl: string;
  descripcion?: string;
}

export interface ViaSegmento {
  idVia: number;
  codigoSegmento: string;
  nombreVia: string;
  fechaRegistro: string;
}

@Injectable({ providedIn: 'root' })
export class ReportesService {
  private http = inject(HttpClient);
  private readonly BASE = `${environment.apiUrl}/admin/reportes`;

  listarMios(pagina = 0, tamanio = 100): Observable<PaginaDto<ReporteResumen>> {
    return this.http.get<PaginaDto<ReporteResumen>>(`${environment.apiUrl}/reportes/mios`, { params: { pagina, tamanio } });
  }

  imagenPropia(id: number): Observable<{ url: string }> {
    return this.http.get<{ url: string }>(`${environment.apiUrl}/reportes/${id}/imagen`);
  }

  listar(
    pagina = 0,
    tamanio = 10,
    estado?: string,
    busqueda?: string,
  ): Observable<PaginaDto<ReporteResumen>> {
    let params = new HttpParams().set('pagina', pagina).set('tamanio', tamanio);
    if (estado) params = params.set('estado', estado);
    if (busqueda && busqueda.trim()) params = params.set('busqueda', busqueda.trim());
    return this.http.get<PaginaDto<ReporteResumen>>(this.BASE, { params });
  }

  obtener(id: number): Observable<ReporteResumen> {
    return this.http.get<ReporteResumen>(`${this.BASE}/${id}`);
  }

  crear(dto: NuevoReporteDto): Observable<ReporteResumen> {
    return this.http.post<ReporteResumen>(this.BASE, dto);
  }

  actualizarEstado(id: number, estado: string): Observable<ReporteResumen> {
    return this.http.patch<ReporteResumen>(`${this.BASE}/${id}/estado`, { estado });
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.BASE}/${id}`);
  }

  estadisticas(): Observable<Estadisticas> {
    return this.http.get<Estadisticas>(`${this.BASE}/estadisticas`);
  }

  listarVias(): Observable<ViaSegmento[]> {
    return this.http.get<ViaSegmento[]>(`${environment.apiUrl}/admin/vias`);
  }
}
