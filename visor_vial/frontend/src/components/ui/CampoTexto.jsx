import React from "react";
export default function CampoTexto({
  label,
  value,
  onChange,
  placeholder,
  type = "text",
  maxLength,
}) {
  return (
    <div>
      <label className="text-sm font-medium block mb-1">{label}</label>
      <input
        type={type}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        maxLength={maxLength}
        className="w-full border border-slate-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-emerald-500"
        required
      />
    </div>
  );
}
