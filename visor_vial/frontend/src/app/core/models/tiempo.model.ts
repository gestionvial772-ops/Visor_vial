export type Etapa =
  | 'INICIO_CAPTURA'
  | 'ENVIO_SERVIDOR'
  | 'PROCESAMIENTO_IA'
  | 'GEOPROCESAMIENTO'
  | 'CONSOLIDACION';

export interface RegistroTiempo {
  idRegistro: number;
  idReporte: number;
  etapa: Etapa;
  timestamp: string; 
}

export interface TiemposReporte {
  idReporte: number;
  inicioCaptura: string | null;
  envioServidor: string | null;
  procesamientoIa: string | null;
  geoprocesamiento: string | null;
  consolidacion: string | null;
}