import { Component, Input, OnInit, OnDestroy, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';

@Component({
  imports: [CommonModule, DatePipe],
  selector: 'app-imagen-con-reloj',
  styleUrl: './imagen-con-reloj.scss',
  templateUrl: './imagen-con-reloj.html',
})
export class ImagenConReloj implements OnInit, OnDestroy{

  @Input() imagenUrl = 'img/vista_aerea.jpg';
  @Input() fechaInicial?: Date;
  fechaCaptura: Date = new Date()

  private timerId: ReturnType<typeof setInterval> | null = null
  private cdr = inject(ChangeDetectorRef)

  ngOnInit(): void{
   if(this.fechaInicial){
    this.fechaCaptura = new Date(this.fechaInicial)
   }
   this.timerId = setInterval(()=>{
    this.fechaCaptura = new Date (this.fechaCaptura.getTime() + 1000);
    this.cdr.detectChanges()
   }, 1000)
  }

  ngOnDestroy(): void {
      if(this.timerId){
        clearInterval(this.timerId)
      }
  }


}
