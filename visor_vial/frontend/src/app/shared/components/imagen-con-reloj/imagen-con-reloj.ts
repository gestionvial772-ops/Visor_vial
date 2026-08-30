import { Component, Input } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';

@Component({
  imports: [CommonModule, DatePipe],
  selector: 'app-imagen-con-reloj',
  styleUrl: './imagen-con-reloj.scss',
  templateUrl: './imagen-con-reloj.html',
})
export class ImagenConReloj {

  @Input() imagenUrl = '';
  @Input() fechaCaptura: Date = new Date()


}
