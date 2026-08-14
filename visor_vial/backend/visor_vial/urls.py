from django.contrib import admin
from django.urls import path, include
from rest_framework import routers
from apps.reportes.views import ReporteViewSet
from drf_spectacular.views import SpectacularAPIView, SpectacularSwaggerView

router = routers.DefaultRouter()
router.register(r'reportes', ReporteViewSet, basename='reporte')

urlpatterns = [
	path('admin/', admin.site.urls),
	path('api/v1/', include(router.urls)),
	path('api/schema/', SpectacularAPIView.as_view(), name='schema'),
	path('api/docs/', SpectacularSwaggerView.as_view(url_name='schema'), name='swagger-ui'),
]

