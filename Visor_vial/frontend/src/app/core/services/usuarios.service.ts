import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Rol } from '../models/usuario.model';
import { PaginaDto } from './reportes.service';

/** Conteos del panel de usuarios. */
export interface EstadisticasUsuarios {
  total: number;
  ciudadanos: number;
  administradores: number;
  activosHoy: number;
}

export interface ActualizarUsuario {
  nombre: string;
  apellidos: string;
  correoElectronico: string;
}

/**
 * Endpoints de gestion de cuentas del panel. El backend es el unico que ve el
 * hash de contrasena y el DNI, asi que aqui no se editan.
 */
@Injectable({ providedIn: 'root' })
export class UsuariosService {
  private http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/admin/usuarios`;

  listar(
    pagina = 0,
    tamanio = 10,
    rol?: string,
    busqueda?: string,
  ): Observable<PaginaDto<UsuarioFila>> {
    let params = new HttpParams().set('pagina', pagina).set('tamanio', tamanio);

    if (rol) params = params.set('rol', rol);
    if (busqueda && busqueda.trim()) params = params.set('busqueda', busqueda.trim());

    return this.http.get<PaginaDto<UsuarioFila>>(this.base, { params });
  }

  estadisticas(): Observable<EstadisticasUsuarios> {
    return this.http.get<EstadisticasUsuarios>(`${this.base}/estadisticas`);
  }

  actualizar(id: number, datos: ActualizarUsuario): Observable<UsuarioFila> {
    return this.http.patch<UsuarioFila>(`${this.base}/${id}`, datos);
  }

  cambiarRol(id: number, rol: Rol): Observable<UsuarioFila> {
    return this.http.patch<UsuarioFila>(`${this.base}/${id}/rol`, { rol });
  }
}

/** Fila del listado. Reutiliza el modelo Usuario y le agrega el conteo de reportes. */
export interface UsuarioFila {
  idUsuario: number;
  dni: string;
  nombre: string;
  apellidos: string;
  correoElectronico: string;
  rol: Rol;
  reportesHoy: number;
  fechaUltimoReporte: string | null;
  fechaCreacion: string;
  totalReportes: number;
}