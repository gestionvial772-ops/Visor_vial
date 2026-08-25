export type Rol = 'ADMIN' | 'CIUDADANO'

export interface Usuario{
    idUsuario : number;
    dni: string;
    nombre: string;
    apellidos: string;
    correoEletronico: string;
    rol: Rol;
    reportesHoy: number;
    fechaUltimoReporte: string | null;
    fechaCreacion: string; 
}


export interface UsuarioRegistro{
    dni: string,
    nombre: string,
    apellidos: string,
    correoElectronico: string,
    contrasena: string;
}

export interface UsuarioLogin{
    dni: string,
    contrasena: string;
}