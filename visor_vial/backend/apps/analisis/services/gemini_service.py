"""
Esqueleto desacoplado para integracion con modelo IA (Gemini u otro)
Funciones aquí deben recibir datos simples (texto, imagen_url) y devolver
un diccionario con la clasificación.
"""
from typing import Optional, Dict


def clasificar_descripcion(texto: str, imagen_url: Optional[str] = None) -> Dict:
    """Placeholder: implementar llamada al servicio de IA.

    Retorna dict con claves: severidad(int), etiqueta(str), nivel_confianza(float), modelo(str)
    """
    # TODO: integrar con cliente oficial de Gemini o servicio HTTP
    # Ejemplo estático para pruebas:
    return {
        'severidad': 2,
        'etiqueta': 'Bache',
        'nivel_confianza': 87.5,
        'modelo': 'Gemini-3.5-Flash'
    }
# llama Gemini API → severidad (1/2/3) + confianza
