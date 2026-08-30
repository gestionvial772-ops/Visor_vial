import { Component, Input, forwardRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
@Component({
  imports: [CommonModule],
  selector: 'app-campo-password',
  styleUrl: './campo-password.scss',
  templateUrl: './campo-password.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CampoPassword),
      multi: true
    }
  ]

})
export class CampoPassword {
  @Input() label = 'Contraseña';
  @Input() icono = 'lock'
  @Input() mensajeError = '';
  @Input() mostrarError = false;

  valor = '';
  deshabilitado = false;
  mostrarTexto = false; // controla si el input muestra texto plano o puntos

  private onChange: (valor: string) => void = () => {};
  private onTouched: () => void = () => {};

  writeValue(valor: string): void {
    this.valor = valor ?? '';
  }
  registerOnChange(fn: (valor: string) => void): void {
    this.onChange = fn;
  }
  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }
  setDisabledState(estaDeshabilitado: boolean): void {
    this.deshabilitado = estaDeshabilitado;
  }
  alCambiar(valor: string): void {
    this.valor = valor;
    this.onChange(valor);
  }
  alTocar(): void {
    this.onTouched();
  }
  alternarVisibilidad(): void {
    this.mostrarTexto = !this.mostrarTexto;
  }
}
