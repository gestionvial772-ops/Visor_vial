import React from "react";
import { Link } from "react-router-dom";
import ReCAPTCHA from "react-google-recaptcha";
import { useLogin } from "../../hooks/useLogin";
import CampoTexto from "../../components/ui/CampoTexto";
import CampoPassword from "../../components/ui/CampoPassword";
import ImagenConReloj from "../../pages/auth/ImagenConReloj"

export default function Login() {
  const login = useLogin();

  return (
    <div className="min-h-screen flex items-center justify-center bg-emerald-50 p-6">
      <div className="flex w-full max-w-4xl rounded-2xl shadow-lg overflow-hidden bg-white">
        <ImagenConReloj
          src="/img/vista_aerea.jpg"
          alt="Vista aérea de La Esperanza"
          fecha={login.fechaFormateada}
          hora={login.horaFormateada}
        />

        <form onSubmit={login.manejarSubmit} className="w-full md:w-1/2 p-8 space-y-4">
          <h1 className="text-2xl font-semibold tracking-wide">SYNTHEVIA</h1>
          <p className="text-sm text-slate-500 mb-2">
            ¡Bienvenido! Ingresa tus credenciales para gestionar el análisis vial.
          </p>

          <CampoTexto
            label="DNI"
            placeholder="123456789"
            maxLength={8}
            value={login.credenciales.dni}
            onChange={(v) => login.actualizarCampo("dni", v)}
          />

          <CampoPassword
            value={login.credenciales.password}
            onChange={(v) => login.actualizarCampo("password", v)}
          />

          <div className="flex items-center justify-between text-sm">
            <label className="flex items-center gap-2">
              <input
                type="checkbox"
                checked={login.recordarme}
                onChange={(e) => login.setRecordarme(e.target.checked)}
              />
              Recordarme
            </label>
            <Link to="/olvide-password" className="text-emerald-700 italic hover:underline">
              ¿Olvidaste tu contraseña?
            </Link>
          </div>

          <ReCAPTCHA
            sitekey={import.meta.env.VITE_RECAPTCHA_SITE_KEY}
            onChange={login.setTokenCaptcha}
          />

          {login.error && <p className="text-sm text-red-600">{login.error}</p>}

          <div className="flex items-center justify-between pt-2">
            <p className="text-sm text-slate-500">
              ¿Nuevo usuario?{" "}
              <Link to="/registro" className="text-emerald-700 italic hover:underline">
                Regístrate
              </Link>
            </p>
            <button
              type="submit"
              disabled={login.cargando}
              className="bg-emerald-800 text-white px-5 py-2 rounded-lg hover:bg-emerald-900 disabled:opacity-50"
            >
              {login.cargando ? "Ingresando..." : "Iniciar sesión"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}