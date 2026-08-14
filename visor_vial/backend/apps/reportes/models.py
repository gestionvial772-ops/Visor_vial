from django.db import models
from django.contrib.gis.db import models as geomodels
from apps.usuarios.models import Usuario
from apps.vias.models import ViaSegmento


class Reporte(models.Model):
	usuario = models.ForeignKey(Usuario, on_delete=models.RESTRICT)
	via = models.ForeignKey(ViaSegmento, on_delete=models.RESTRICT, null=True, blank=True)
	reporte_original = models.ForeignKey('self', null=True, blank=True, on_delete=models.SET_NULL)
	es_preprueba = models.BooleanField(default=False)
	imagen_url = models.URLField(max_length=500)
	descripcion = models.TextField(blank=True)
	estado = models.CharField(max_length=12, choices=(('PENDIENTE','PENDIENTE'),('EN_PROCESO','EN_PROCESO'),('RESUELTO','RESUELTO'),('DUPLICADO','DUPLICADO')), default='PENDIENTE')
	fecha_creacion = models.DateTimeField(auto_now_add=True)

	def __str__(self):
		return f"Reporte {self.id} - {self.estado}"


class Coordenada(models.Model):
	reporte = models.OneToOneField(Reporte, on_delete=models.CASCADE, related_name='coordenada')
	punto_capturado = geomodels.PointField(srid=4326)
	punto_control = geomodels.PointField(srid=4326, null=True, blank=True)
	error_metros = models.DecimalField(max_digits=8, decimal_places=3, null=True, blank=True)
	fecha_calculo = models.DateTimeField(auto_now_add=True)


class Clasificacion(models.Model):
	reporte = models.OneToOneField(Reporte, on_delete=models.CASCADE, related_name='clasificacion')
	severidad = models.SmallIntegerField(choices=((1,'Leve'),(2,'Moderado'),(3,'Severo')))
	etiqueta_detectada = models.CharField(max_length=50, blank=True, null=True)
	nivel_confianza = models.DecimalField(max_digits=5, decimal_places=2)
	modelo_ia_utilizado = models.CharField(max_length=50, default='Gemini 3.5 Flash')
	fecha_procesamiento = models.DateTimeField(auto_now_add=True)


class RegistroTiempo(models.Model):
	ETAPAS = (
		('INICIO_CAPTURA','INICIO_CAPTURA'),
		('ENVIO_SERVIDOR','ENVIO_SERVIDOR'),
		('PROCESAMIENTO_IA','PROCESAMIENTO_IA'),
		('GEOPROCESAMIENTO','GEOPROCESAMIENTO'),
		('CONSOLIDACION','CONSOLIDACION'),
	)
	reporte = models.ForeignKey(Reporte, on_delete=models.CASCADE, related_name='registro_tiempos')
	etapa = models.CharField(max_length=30, choices=ETAPAS)
	timestamp = models.DateTimeField(auto_now_add=True)

	class Meta:
		unique_together = ('reporte','etapa')

