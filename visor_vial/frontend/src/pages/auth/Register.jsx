import React from "react";
import { Link } from "react-router-dom";
import ReCAPTCHA from "react-google-recaptcha";
import { useRegister } from "../../hooks/useRegister";
import CampoTexto from "../../components/ui/CampoTexto";
import CampoPassword from "../../components/ui/CampoPassword";
import ImagenConReloj from "../../pages/auth/ImagenConReloj"

export default function Register() {
  const registro = useRegister();

  return (
    <div className="min-h-screen flex items-center justify-center bg-emerald-50 p-6">
      <div className="flex w-full max-w-4xl rounded-2xl shadow-lg overflow-hidden bg-white">
        <ImagenConReloj
          src="/img/ilustracion-mapa.jpg"
          alt="Ilustración de reportes en el mapa"
        />

        <form onSubmit={registro.manejarSubmit} className="w-full md:w-1/2 p-8 space-y-4">
          <div className="flex items-center justify-between">
            <h1 className="text-2xl font-semibold tracking-wide">SYNTHEVIA</h1>
            <p className="text-xs text-slate-500">
              ¿Ya tienes cuenta?{" "}
              <Link to="/login" className="text-emerald-700 italic hover:underline">
                Inicia sesión aquí
              </Link>
            </p>
          </div>
          <p className="text-lg font-medium">Crea una cuenta</p>

          <div className="grid grid-cols-2 gap-4">
            <CampoTexto
              label="Nombres"
              placeholder="Nombres"
              value={registro.formulario.nombre}
              onChange={(v) => registro.actualizarCampo("nombre", v)}
            />
            <CampoTexto
              label="Apellidos"
              placeholder="Apellidos"
              value={registro.formulario.apellidos}
              onChange={(v) => registro.actualizarCampo("apellidos", v)}
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <CampoTexto
              label="Correo electrónico"
              placeholder="example@gmail.com"
              type="email"
              value={registro.formulario.correoElectronico}
              onChange={(v) => registro.actualizarCampo("correoElectronico", v)}
            />
            <CampoTexto
              label="DNI"
              placeholder="123456789"
              maxLength={8}
              value={registro.formulario.dni}
              onChange={(v) => registro.actualizarCampo("dni", v)}
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <CampoPassword
              value={registro.formulario.password}
              onChange={(v) => registro.actualizarCampo("password", v)}
            />
            <CampoPassword
              label="Confirmar contraseña"
              value={registro.formulario.confirmarPassword}
              onChange={(v) => registro.actualizarCampo("confirmarPassword", v)}
            />
          </div>

          <ReCAPTCHA
            sitekey={import.meta.env.VITE_RECAPTCHA_SITE_KEY}
            onChange={registro.setTokenCaptcha}
          />

          <label className="flex items-center gap-2 text-sm text-slate-600">
            <input
              type="checkbox"
              checked={registro.formulario.aceptaTerminos}
              onChange={registro.alternarAceptaTerminos}
            />
            Crear tu cuenta y aceptar{" "}
            <a href="/terminos" className="text-emerald-700 font-medium hover:underline">
              términos y condiciones
            </a>
          </label>

          {registro.error && <p className="text-sm text-red-600">{registro.error}</p>}

          <button
            type="submit"
            disabled={registro.cargando}
            className="w-full bg-emerald-800 text-white px-5 py-2 rounded-lg hover:bg-emerald-900 disabled:opacity-50"
          >
            {registro.cargando ? "Creando cuenta..." : "Crear cuenta"}
          </button>
        </form>
      </div>
    </div>
  );
}