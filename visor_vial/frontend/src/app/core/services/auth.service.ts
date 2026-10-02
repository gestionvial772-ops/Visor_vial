import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UsuarioLogin, UsuarioRegistro } from '../models/usuario.model';
import { environment } from '../../../environments/environment';

export interface RespuestaLogin {
  exito: boolean;
  mensaje: string;
  nombre: string | null;
  rol: string | null;
}

export interface RespuestaRegistro{
  exito: boolean;
  mensaje: string;
}


@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly API_URL = `${environment.apiUrl}/public`;

  iniciarSesion(credenciales: UsuarioLogin): Observable<RespuestaLogin> {
    return this.http.post<RespuestaLogin>(`${this.API_URL}/iniciar-sesion`, credenciales);
  }

  registrarse(datos: UsuarioRegistro): Observable<RespuestaRegistro> {
    return this.http.post<RespuestaRegistro>(`${this.API_URL}/registrarse`, datos);
  }
}
