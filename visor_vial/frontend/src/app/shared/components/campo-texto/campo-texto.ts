import { Component, Input, forwardRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';//preguntar

@Component({
  imports: [CommonModule],
  selector: 'app-campo-texto',
  styleUrl: './campo-texto.scss',
  templateUrl: './campo-texto.html',
  providers:[
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(()=> CampoTexto), //se ejecuta cuando toda la clase se ha definido
      multi: true //avisa que pueden haber varios ng-value-accessor en el mismo componente, y que no se sobreescriba
    }

  ]
})
export class CampoTexto implements ControlValueAccessor{
  @Input() label = '';
  @Input() placeholder = '';
  @Input() tipo: 'text' | 'email' = 'text';
  @Input() mensajeError = '';
  @Input() mostrarError = false;


  valor = '';
  deshabilitado = false;

  private onChange: (valor: string) => void = () => {};
  private onTouched: () => void = () => {}

  writeValue(valor: string): void {
      this.valor = valor ?? '';
  }

  registerOnChange(fn: (valor:string)=> void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
      this.onTouched = fn;
  }

  setDisabledState(estaDeshabilitado: boolean): void {
    this.deshabilitado = estaDeshabilitado
  }

  alCambiar(valor:string):void{
    this.valor = valor;
    this.onChange(valor)
  }

  alTocar():void{
    this.onTouched()
  }

}
