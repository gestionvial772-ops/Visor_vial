import { useState } from "react";
import React from "react";

export default function CampoPassword({
  label = "Contraseña",
  value,
  onChange,
}) {
  const [mostrar, setMostrar] = useState(false);

  return (
    <div>
      <label className="text-sm font-medium block mb-1">{label}</label>
      <div className="relative">
        <input
          type={mostrar ? "text" : "password"}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          className="w-full border border-slate-300 rounded-lg px-3 py-2 pr-10 focus:outline-none focus:ring-2 focus:ring-emerald-500"
          required
        />

        <button
          type="button"
          onClick={() => setMostrar((v) => !v)}
          className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 text-sm"
        >
          {mostrar ? "Ocultar" : "Ver"}
        </button>
      </div>
    </div>
  );
}
