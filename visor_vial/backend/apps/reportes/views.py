from rest_framework import viewsets, permissions
from .models import Reporte
from .serializers import ReporteSerializer


class ReporteViewSet(viewsets.ModelViewSet):
    queryset = Reporte.objects.select_related('via').all()
    serializer_class = ReporteSerializer
    permission_classes = [permissions.IsAuthenticatedOrReadOnly]
# crear / listar / actualizar estado de reportes
