import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'severidad',
})
export class SeveridadPipe implements PipeTransform {
  transform(value: unknown, ...args: unknown[]): unknown {
    return null;
  }
}
