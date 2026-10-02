import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { AnalisisIa, ReporteDetalle } from '../models/reporte.model';
import { environment } from '../../../environments/environment';

/**
 * Endpoints del analisis de danos con IA. Exclusivos del panel del
 * administrador: el ciudadano nunca consulta el resultado del modelo.
 */
@Injectable({ providedIn: 'root' })
export class AnalisisService {
  private readonly http = inject(HttpClient);

  private readonly base = `${environment.apiUrl}/admin/reportes`;

  obtenerDetalle(idReporte: number): Observable<ReporteDetalle> {
    return this.http.get<ReporteDetalle>(`${this.base}/${idReporte}/detalle`);
  }

  obtenerAnalisis(idReporte: number): Observable<AnalisisIa> {
    return this.http.get<AnalisisIa>(`${this.base}/${idReporte}/analisis`);
  }

  /**
   * URL firmada de la evidencia. El bucket de Supabase Storage es privado, asi
   * que la ruta guardada en imagen_url no la puede cargar el navegador.
   */
  obtenerImagen(idReporte: number): Observable<{ url: string }> {
    return this.http.get<{ url: string }>(`${this.base}/${idReporte}/imagen`);
  }

  reanalizar(idReporte: number): Observable<{ mensaje: string; detalle?: string }> {
    return this.http.post<{ mensaje: string; detalle?: string }>(
      `${this.base}/${idReporte}/reanalizar`,
      {},
    );
  }
}
