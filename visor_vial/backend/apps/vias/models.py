from django.contrib.gis.db import models


class ViaSegmento(models.Model):
	codigo_segmento = models.CharField(max_length=20, unique=True)
	nombre_via = models.CharField(max_length=200)
	geom = models.LineStringField(srid=4326)
	fecha_registro = models.DateTimeField(auto_now_add=True)

	def __str__(self):
		return f"{self.codigo_segmento} - {self.nombre_via}"

