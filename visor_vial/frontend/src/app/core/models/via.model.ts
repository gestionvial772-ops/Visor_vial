import { PuntoGeografico } from "./reporte.model";

export interface ViaSegmento {
  idVia: number;
  codigoSegmento: string;
  nombreVia: string;
  geom: PuntoGeografico[]; 
  fechaRegistro: string; 
}