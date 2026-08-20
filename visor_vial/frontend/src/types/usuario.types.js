export const ROLES = {
    ADMIN: "ADMIN",
    CIUDADANO: "CIUDADANO"
};


export function crearCredencialesVacias(){
    return {dni: "", password: ""};
}


export function crearRegistroVacio(){
    return{
        nombre: "",
        apellidos: "",
        correoElectronico: "",
        dni: "",
        password: "",
        confirmarPassword: "",
        aceptaTerminos: false,
    }
}

export function armarRegistroPayload(formulario) {
  const { nombre, apellidos, correoElectronico, dni, password } = formulario;
  return { nombre, apellidos, correoElectronico, dni, password };
}