import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UsuarioLogin } from '../models/usuario.model';

export interface RespuestaLogin {
  exito: boolean;
  mensaje: string;
  nombre: string | null;
  rol: string | null;
}

@Injectable({ providedIn: 'root' })
export class AuthService {

  private http = inject(HttpClient);
  private readonly API_URL = 'http://localhost:8080/public';

  iniciarSesion(credenciales: UsuarioLogin): Observable<RespuestaLogin> {
    return this.http.post<RespuestaLogin>(`${this.API_URL}/iniciar-sesion`, credenciales);
  }
}
