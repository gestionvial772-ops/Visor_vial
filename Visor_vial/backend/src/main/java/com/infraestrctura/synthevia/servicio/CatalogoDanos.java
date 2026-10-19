package com.infraestrctura.synthevia.servicio;

import java.util.List;

/**
 * Catalogo cerrado de deterioroos que el modelo debe evaluar. El mismo orden y
 * los mismos codigos se exigen en el prompt y en el responseSchema de Gemini,
 * y despues se reutilizan para reconstruir la tabla de ocho filas que ve el
 * administrador.
 */
public final class CatalogoDanos {

    public static final String BACHES = "BACHES";
    public static final String DANO_EN_RAMPA = "DANO_EN_RAMPA";
    public static final String DESNIVEL = "DESNIVEL";
    public static final String FISURAS = "FISURAS";
    public static final String RAJADURA = "RAJADURA";
    public static final String HUECOS = "HUECOS";
    public static final String HUNDIMIENTO = "HUNDIMIENTO";
    public static final String PIEL_DE_COCODRILO = "PIEL_DE_COCODRILO";

    public record Definicion(String codigo, String etiqueta) {
    }

    public static final List<Definicion> DEFINICIONES = List.of(
            new Definicion(BACHES, "Baches"),
            new Definicion(DANO_EN_RAMPA, "Daño en rampa"),
            new Definicion(DESNIVEL, "Desnivel"),
            new Definicion(FISURAS, "Fisuras"),
            new Definicion(RAJADURA, "Rajadura"),
            new Definicion(HUECOS, "Huecos"),
            new Definicion(HUNDIMIENTO, "Hundimiento"),
            new Definicion(PIEL_DE_COCODRILO, "Piel de cocodrilo"));

    private CatalogoDanos() {
    }

    public static String etiquetaDe(String codigo) {
        return DEFINICIONES.stream()
                .filter(d -> d.codigo().equalsIgnoreCase(codigo))
                .map(Definicion::etiqueta)
                .findFirst()
                .orElse(codigo);
    }
}
