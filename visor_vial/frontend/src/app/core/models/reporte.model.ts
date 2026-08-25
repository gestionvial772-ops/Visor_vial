export type Estado = 'PENDIENTE' | 'EN_PROCESO' | 'RESUELTO' | 'DUPLICADO';


export interface Reporte{
    idReporte: number,
    idUsuario: number,
    idVia: number | null,
    reporteOriginalId: string | null,
    esPreprueba: boolean;
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
  nivelConfianza: number; // 0 a 100
  modeloIaUtilizado: string;
}

export interface ReporteDetalle extends Reporte {
  coordenada?: Coordenada;
  clasificacion?: Clasificacion;
}


export interface NuevoReporte {
  idVia?: number;
  imagenUrl: string;
  descripcion?: string;
  puntoCapturado: PuntoGeografico;
}