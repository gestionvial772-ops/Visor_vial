import { use, useState } from "react";
import {
  crearRegistroVacio,
  armarRegistroPayload,
} from "../types/usuario.types";

export function useRegister() {
  const [formulario, setFormulario] = useState(crearRegistroVacio());
  const [tokenCaptcha, setTokenCaptcha] = useState(null);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState(null);

  function actualizarCampo(campo, valor) {
    setFormulario((prev) => ({ ...prev, [campo]: valor }));
  }

  function alternarAceptaTerminos() {
    setFormulario((prev) => ({
      ...prev,
      aceptaTerminos: !prev.aceptaTerminos,
    }));
  }

  function validar() {
    if (formulario.password !== formulario.confirmarPassword) {
      return "Las constraseñas no coinciden";
    }

    if (!formulario.aceptaTerminos) {
      return "Debes aceptar los terminos y condiciones";
    }

    if (!tokenCaptcha) {
      return "Por favor verifica que no eres un robot";
    }

    return null;
  }

  async function manejarSubmit(e) {
    e.preventDefault();
    const mensajeError = validar();

    if (mensajeError) {
      setError(mensajeError);
      return;
    }

    setError(null);
    setCargando(true);

    try {
      const payload = armarRegistroPayload(formulario);
      console.log("Enviando registor: ", payload, tokenCaptcha);
    } catch (err) {
      setError("No se pudo completar el registro, intenta nuevamente");
    } finally {
      setCargando(false);
    }

    
  }
  return {
      formulario,
      actualizarCampo,
      alternarAceptaTerminos,
      setTokenCaptcha,
      cargando,
      error,
      manejarSubmit,
    };
}
