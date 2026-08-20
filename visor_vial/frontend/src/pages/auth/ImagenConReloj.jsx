import React from "react";
export default function ({ src, alt, fecha, hora }) {
  return (
    <div className="hidden md:block relative w-1/2">
      <img src={src} alt={alt} className="w-full h-full object-cover" />
      {fecha && hora && (
        <span className="absolute bottom-4 left-4 bg-white/90 text-xs text-slate-600 px-3 py-1 rounded-full">
          {fecha} {hora}
        </span>
      )}
    </div>
  );
}
