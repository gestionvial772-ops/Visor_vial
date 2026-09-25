export type Estado = 'PENDIENTE' | 'EN_PROCESO' | 'RESUELTO' | 'DUPLICADO';


export interface Reporte{
    idReporte: number,
    idUsuario: number,
    idVia: number | null,
    reporteOriginalId: number | null,
    imagenUrl: string,
    descripcion: string | null,
    estado: Estado;
  fechaCreacion: string; 
}


export interface PuntoGeografico{
    lat: number;
    lng: number;
}

export interface Coordenada {
  idCoordenada: number;
  idReporte: number;
  puntoCapturado: PuntoGeografico;
  puntoControl: PuntoGeografico | null; 
  errorMetros: number | null;
}

export interface Clasificacion {
  idClasificacion: number;
  idReporte: number;
  severidad: 1 | 2 | 3;
  etiquetaDetectada: string | null;
  nivelConfianza: number; 
  modeloIaUtilizado: string;
}

export interface ReporteDetalle extends Reporte {
  coordenada?: Coordenada;
  clasificacion?: Clasificacion;
}

export interface CrearReporteRequest {
  idVia: number | null;
  descripcion: string;
  latitud: number;
  longitud: number;
  precisionReportadaMetros: number | null;
  instanteInicioCaptura: string;
}