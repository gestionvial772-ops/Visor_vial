import { Injectable, signal } from '@angular/core';

export interface ToastState {
  visible: boolean;
  mensaje: string;
  exito: boolean;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private timer: ReturnType<typeof setTimeout> | null = null;

  estado = signal<ToastState>({ visible: false, mensaje: '', exito: true });

  mostrar(mensaje: string, exito: boolean, duracion = 4000): void {
    if (this.timer) {
      clearTimeout(this.timer);
    }
    this.estado.set({ visible: true, mensaje, exito });
    this.timer = setTimeout(() => {
      this.estado.set({ visible: false, mensaje: '', exito: true });
    }, duracion);
  }
}
