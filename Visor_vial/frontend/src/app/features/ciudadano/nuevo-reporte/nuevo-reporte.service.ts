import { Injectable, inject } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";

import { CrearReporteRequest } from "../../../core/models/reporte.model";
import { environment } from "../../../../environments/environment";
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
}