# configuración principal del proyecto Django
import os
from pathlib import Path

import dj_database_url
from dotenv import load_dotenv

BASE_DIR = Path(__file__).resolve().parent.parent
load_dotenv(BASE_DIR / '.env')

SECRET_KEY = os.environ.get('DJANGO_SECRET_KEY', 'django-insecure-dev-key-change-me')
DEBUG = os.environ.get('DEBUG', 'True') == 'True'
ALLOWED_HOSTS = [h.strip() for h in os.environ.get('ALLOWED_HOSTS', '127.0.0.1,localhost').split(',') if h.strip()]

# Datos del proyecto Supabase (para llamadas a la API REST/Storage si se necesitan)
SUPABASE_URL = os.environ.get('SUPABASE_URL', '')
SUPABASE_ANON_KEY = os.environ.get('SUPABASE_ANON_KEY', '')

INSTALLED_APPS = [
	'django.contrib.admin',
	'django.contrib.auth',
	'django.contrib.contenttypes',
	'django.contrib.sessions',
	'django.contrib.messages',
	'django.contrib.staticfiles',
	'django.contrib.gis',

	'rest_framework',
	'rest_framework_simplejwt',
	'corsheaders',
	'django_filters',
	'drf_spectacular',

	'apps.usuarios',
	'apps.vias',
	'apps.reportes',
	'apps.analisis',
	'apps.notificaciones',
]

MIDDLEWARE = [
	'django.middleware.security.SecurityMiddleware',
	'corsheaders.middleware.CorsMiddleware',
	'django.contrib.sessions.middleware.SessionMiddleware',
	'django.middleware.common.CommonMiddleware',
	'django.middleware.csrf.CsrfViewMiddleware',
	'django.contrib.auth.middleware.AuthenticationMiddleware',
	'django.contrib.messages.middleware.MessageMiddleware',
	'django.middleware.clickjacking.XFrameOptionsMiddleware',
]

ROOT_URLCONF = 'visor_vial.urls'

TEMPLATES = [
	{
		'BACKEND': 'django.template.backends.django.DjangoTemplates',
		'DIRS': [],
		'APP_DIRS': True,
		'OPTIONS': {
			'context_processors': [
				'django.template.context_processors.debug',
				'django.template.context_processors.request',
				'django.contrib.auth.context_processors.auth',
				'django.contrib.messages.context_processors.messages',
			],
		},
	},
]

WSGI_APPLICATION = 'visor_vial.wsgi.application'
ASGI_APPLICATION = 'visor_vial.asgi.application'

# Base de datos: Postgres + PostGIS de Supabase, vía connection string (DATABASE_URL)
DATABASES = {
	'default': dj_database_url.parse(
		os.environ.get('DATABASE_URL', 'postgresql://postgres:postgres@localhost:5432/postgres'),
		engine='django.contrib.gis.db.backends.postgis',
		conn_max_age=600,
	)
}

AUTH_USER_MODEL = 'usuarios.Usuario'

AUTH_PASSWORD_VALIDATORS = [
	{'NAME': 'django.contrib.auth.password_validation.UserAttributeSimilarityValidator'},
	{'NAME': 'django.contrib.auth.password_validation.MinimumLengthValidator'},
	{'NAME': 'django.contrib.auth.password_validation.CommonPasswordValidator'},
	{'NAME': 'django.contrib.auth.password_validation.NumericPasswordValidator'},
]

LANGUAGE_CODE = 'es'
TIME_ZONE = 'America/Lima'
USE_I18N = True
USE_TZ = True

STATIC_URL = 'static/'
DEFAULT_AUTO_FIELD = 'django.db.models.BigAutoField'

REST_FRAMEWORK = {
	'DEFAULT_AUTHENTICATION_CLASSES': (
		'rest_framework_simplejwt.authentication.JWTAuthentication',
	),
	'DEFAULT_FILTER_BACKENDS': ('django_filters.rest_framework.DjangoFilterBackend',),
	'DEFAULT_SCHEMA_CLASS': 'drf_spectacular.openapi.AutoSchema',
}

SPECTACULAR_SETTINGS = {
	'TITLE': 'Visor Vial API',
	'DESCRIPTION': 'API del sistema de reporte de incidencias viales',
	'VERSION': '1.0.0',
}

FRONTEND_URL = os.environ.get('FRONTEND_URL', 'http://localhost:5173')
CORS_ALLOWED_ORIGINS = [FRONTEND_URL]

