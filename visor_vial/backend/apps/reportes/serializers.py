from rest_framework import serializers
from .models import Reporte, Coordenada, Clasificacion


class CoordenadaSerializer(serializers.ModelSerializer):
    class Meta:
        model = Coordenada
        fields = ('punto_capturado','punto_control','error_metros','fecha_calculo')


class ClasificacionSerializer(serializers.ModelSerializer):
    class Meta:
        model = Clasificacion
        fields = ('severidad','etiqueta_detectada','nivel_confianza','modelo_ia_utilizado','fecha_procesamiento')


class ReporteSerializer(serializers.ModelSerializer):
    coordenada = CoordenadaSerializer(read_only=True)
    clasificacion = ClasificacionSerializer(read_only=True)

    class Meta:
        model = Reporte
        fields = ('id','usuario','via','imagen_url','descripcion','estado','fecha_creacion','coordenada','clasificacion','es_preprueba')
# creación, detalle, cambio de estado
