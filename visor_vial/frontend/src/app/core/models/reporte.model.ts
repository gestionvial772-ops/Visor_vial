export type Estado = 'PENDIENTE' | 'EN_PROCESO' | 'RESUELTO' | 'DUPLICADO';


export interface Reporte{
    idReporte: number,
    idUsuario: number,
    idVia: number | null,
    nombreVia?: string | null;
    codigoSegmento?: string | null;
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

export interface CrearReporteRequest {
  idVia: number | null;
  descripcion: string;
  latitud: number;
  longitud: number;
  precisionReportadaMetros: number | null;
  instanteInicioCaptura: string;
}

/** Fila de la tabla de deteriorateos que muestra el panel del administrador */
export interface TipoDanoFila {
  codigo: string;
  etiqueta: string;
  detectado: boolean;
  cantidad: number;
  confianza: number | null;
}

/** Resultado del analisis de imagenes con Gemini */
export interface AnalisisIa {
  idReporte: number;
  disponible: boolean;
  tieneDano: boolean;
  cantidadDanos: number;
  severidad: 1 | 2 | 3;
  severidadEtiqueta: string;
  tiposDano: TipoDanoFila[];
  nivelConfianza: number | null;
  modeloIaUtilizado: string;
  versionModelo: string | null;
  reporteBreve: string | null;
  fechaProcesamiento: string;
}

export interface CoordenadaReporte {
  latitud: number | null;
  longitud: number | null;
  precisionReportadaMetros: number | null;
  fechaCaptura: string;
}

/** Refleja el ReporteDetalleDto del backend: el reporte viene anidado */
export interface ReporteDetalle {
  reporte: Reporte;
  coordenada: CoordenadaReporte | null;
  analisis: AnalisisIa | null;
}