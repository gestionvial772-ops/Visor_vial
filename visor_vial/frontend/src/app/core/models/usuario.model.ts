export type Rol = 'ADMIN' | 'CIUDADANO'

export interface Usuario{
    idUsuario : number;
    dni: string;
    nombre: string;
    apellidos: string;
    correoElectronico: string;
    rol: Rol;
    reportesHoy: number;
    fechaUltimoReporte: string | null;
    fechaCreacion: string; 
}


export interface UsuarioRegistro {
  nombre: string;
  apellidos: string;
  correoElectronico: string;
  dni: string;
  contrasena: string;
  captchaToken: string;
}
export interface UsuarioLogin{
    dni: string,
    contrasena: string;
    captchaToken: string;
    recordarme: boolean;
}