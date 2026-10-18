import { Injectable, signal } from '@angular/core';

export interface ToastState {
  visible: boolean;
  mensaje: string;
  titulo: string;
  exito: boolean;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private timer: ReturnType<typeof setTimeout> | null = null;

  estado = signal<ToastState>({ visible: false, mensaje: '', titulo: '', exito: true });

  /**
   * Muestra una notificacion. El titulo es opcional: si no se indica se deriva
   * del exito, para no tener que repetirlo en cada llamada.
   */
  mostrar(mensaje: string, exito: boolean, titulo?: string, duracion = 4000): void {
    if (this.timer) {
      clearTimeout(this.timer);
    }
    this.estado.set({
      visible: true,
      mensaje,
      titulo: titulo ?? (exito ? 'Operación exitosa' : 'Error'),
      exito,
    });
    this.timer = setTimeout(() => {
      this.estado.set({ visible: false, mensaje: '', titulo: '', exito: true });
    }, duracion);
  }

  exito(mensaje: string, titulo = 'Operación exitosa', duracion = 4000): void {
    this.mostrar(mensaje, true, titulo, duracion);
  }

  error(mensaje: string, titulo = 'Error', duracion = 4000): void {
    this.mostrar(mensaje, false, titulo, duracion);
  }
}
