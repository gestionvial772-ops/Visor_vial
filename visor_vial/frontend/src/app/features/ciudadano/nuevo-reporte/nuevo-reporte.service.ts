import { Injectable, inject } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";

import { CrearReporteRequest } from "../../../core/models/reporte.model";
import { environment } from "../../../../environments/environment";

export interface ViaCercana {
    idVia: number;
    codigoSegmento: string;
    nombreVia: string;
    distanciaMetros: number;
}

@Injectable({
    providedIn: 'root'
})

export class NuevoReporteService{

    private readonly http = inject(HttpClient)

    private readonly apiUrl = `${environment.apiUrl}/reportes`

    crearReporte(datos: CrearReporteRequest, imagen: File):Observable<unknown>{
        const formData = new FormData()

        const dtoJson = new Blob(
            [
                JSON.stringify(datos)
            ],
            {
                 type: 'application/json'
            }
        )
        formData.append('dto', dtoJson);
        formData.append('imagen',imagen, imagen.name);

        return this.http.post(this.apiUrl, formData);
        
    }

    obtenerViasCercanas(latitud: number, longitud: number, radio: number = 20, limite: number = 5): Observable<ViaCercana[]> {
        const url = `${this.apiUrl}/vias-cercanas`;
        const params = {
            latitud: latitud.toString(),
            longitud: longitud.toString(),
            radio: radio.toString(),
            limite: limite.toString()
        };
        
        return this.http.get<ViaCercana[]>(url, { params });
    }

    obtenerViasGeoJson(): Observable<any> {
        const url = `${environment.apiUrl}/admin/vias/geojson`;
        return this.http.get(url);
    }
}