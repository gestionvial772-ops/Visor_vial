import { useState, useEffect, act } from "react";
import { crearCredencialesVacias } from "../types/usuario.types";

export function useLogin() {
  const [credenciales, setCredenciales] = useState(crearCredencialesVacias());
  const [recordarme, setRecordarme] = useState(false);
  const [tokenCaptcha, setTokenCaptcha] = useState(false);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState(null);
  const [fechaHora, setFechaHora] = useState(new Date());


  useEffect(() => {
    const intervalo = setInterval(() => setFechaHora(new Date()), 1000);
    return () => clearInterval(intervalo);
  }, []);

  const fechaFormateada = fechaHora.toLocaleDateString("es-PE", {
    weekday: "long",
    day: "numeric",
    month: "long",
  });

  const horaFormateada = fechaHora.toLocaleTimeString("es-PE");

  function actualizarCampo(campo, valor) {
    setCredenciales((prev) => ({ ...prev, [campo]: valor }));
  }

  async function manejarSubmit(e) {
    e.preventDefault();
    setError(null);

    if (!tokenCaptcha) {
      setError("Debes verificar que no eres un robot");
      return;
    }

    setCargando(true);

    //Aqui se conecta con authservice.js cundo el endpoint esté listo
    try {
      console.log("Enviando el login: ", credenciales, tokenCaptcha);
    } catch (err) {
      setError("DNI O CONTRASEÑA INCORRECTA");
    } finally {
      setCargando(false);
    }
  }
  return {
    credenciales,
    actualizarCampo,
    recordarme,
    setRecordarme,
    setTokenCaptcha,
    cargando,
    error,
    fechaFormateada,
    horaFormateada,
    manejarSubmit,
  };
}
