import { Component, Input, ElementRef, HostListener, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Icono de informacion que explica un dato al pasar el mouse o al pulsarlo.
 *
 * El atributo title nativo no servia: solo responde al hover, tarda cerca de un
 * segundo en aparecer y en pantallas tactiles no aparece nunca. Este componente
 * resuelve los tres casos con un panel posicionado al vuelo.
 */
@Component({
  selector: 'app-info-tip',
  imports: [CommonModule],
  templateUrl: './info-tip.html',
  styleUrl: './info-tip.scss',
})
export class InfoTip {

  /** Texto de la aclaracion. */
  @Input() texto = '';

  /** Lado del panel respecto al icono. */
  @Input() lado: 'arriba' | 'abajo' = 'abajo';

  readonly abierto = signal(false);

  constructor(private host: ElementRef<HTMLElement>) {}

  alternar(): void {
    this.abierto.update((v) => !v);
  }

  /** Cierra al pulsar fuera del icono. */
  @HostListener('document:click', ['$event'])
  alPulsarFuera(event: MouseEvent): void {
    if (!this.host.nativeElement.contains(event.target as Node)) {
      this.abierto.set(false);
    }
  }

  /** Cierra con Escape, que es lo que espera el teclado. */
  @HostListener('document:keydown.escape')
  alPulsarEscape(): void {
    this.abierto.set(false);
  }
}