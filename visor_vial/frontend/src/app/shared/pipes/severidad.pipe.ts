import { Pipe, PipeTransform } from '@angular/core';
import { SEVERIDAD_LABELS } from '../constants/severidad.constants';
@Pipe({
  name: 'severidad',
  standalone: true,
})
export class SeveridadPipe implements PipeTransform {
  transform(value: number | null | undefined): string {
    if(value == null){
      return 'Sin clasificar';
    }
    return SEVERIDAD_LABELS[value] ?? 'Sin clasificar';
  }
}


