import { Injectable, signal, computed, inject } from '@angular/core';

import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface SesionUsuario {
  nombre: string;
  rol: string;
}

@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly http = inject(HttpClient);
  private readonly CLAVE = 'synthevia_sesion';

  private _usuario = signal<SesionUsuario | null>(this.cargarDesdeStorage());

  readonly usuario = this._usuario.asReadonly();
  readonly estaAutenticado = computed(() => this._usuario() !== null);
  readonly esAdmin = computed(() => this._usuario()?.rol === 'ADMIN');

  iniciar(usuario: SesionUsuario): void {
    this._usuario.set(usuario);
    sessionStorage.setItem(this.CLAVE, JSON.stringify(usuario));
  }

  cerrar() {
    return this.http.post<void>(environment.apiUrl + '/public/cerrar-sesion', {}).pipe(tap(() => {
      this.limpiarSesionLocal();
    }));
  }

  limpiarSesionLocal(): void {
    this._usuario.set(null);
    sessionStorage.removeItem(this.CLAVE);
  }

  private cargarDesdeStorage(): SesionUsuario | null {
    try {
      const raw = sessionStorage.getItem(this.CLAVE);
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }
}
