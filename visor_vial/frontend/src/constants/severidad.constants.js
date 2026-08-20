export const SEVERIDAD ={
    1: {label: "Leve", color: "#22c55e"},
    2: {label: "Leve", color: "#22c55e"},
    3: {label: "Leve", color: "#22c55e"}

};



export function obtenerSeveridad(codigo){
    return SEVERIDAD[codigo] ?? {label: "Sin clasificar" , color: "#9ca3af"}
}