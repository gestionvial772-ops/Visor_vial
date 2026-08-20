export const ESTADOS_REPORTE ={
    PENDIENTE: "PENDIENTE",
    EN_PROCESO: "EN_PROCESO",
    RESUELTO: "RESUELTO",
    DUPLICADO: "DUPLICADO"
}


export function crearNuevoReporteVacio(){
    return{
        puntoCapturado: null,
        imagen: null,
        descripcion: "",
        esPreprueba: false
    }
}


export function armarNuevoReporteFormData(formulario){
    const formData = new FormData();
    formData.append("lat", formulario.puntoCapturado.lat);
    formData.append("lng", formulario.puntoCapturado.lng);
    formData.append("imagen", formulario.imagen);
    formData.append("descripcion", formulario.descripcion);
    formData.append("esPrepuerba", formulario.esPreprueba);
    return formData;
}