import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ViasService {
  private apiUrl = `${environment.apiUrl}/admin/vias`;

  constructor(private http: HttpClient) {}

  obtenerViasGeoJson(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/geojson`);
  }
}