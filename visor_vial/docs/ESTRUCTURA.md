# Visor Vial — Arquitectura del Monorepo

> Árbol de directorios y archivos. Sin código fuente.
> Trujillo, 2026 · Django 5 + React 18 + Vite + Tailwind CSS

---

```
visor_vial/                                        ← raíz del monorepo
│
├── backend/                                       ← API REST (Python / Django)
├── frontend/                                      ← SPA (React / Vite)
├── docs/                                          ← documentación del proyecto
│   ├── ESTRUCTURA.md                              ← este archivo
│   ├── esquema_bd.sql                             ← DDL PostgreSQL / PostGIS
│   ├── matriz_operacionalizacion.md               ← variables, dimensiones, indicadores
│   └── diagramas/                                 ← ERD, secuencia, wireframes
└── .gitignore                                     ← exclusiones globales del repo
```

---

## /backend

```
backend/
│
├── .env                                           ← variables de entorno reales (no versionar)
├── .env.example                                   ← plantilla de variables para el equipo
├── requirements.txt                               ← dependencias Python del proyecto
├── manage.py                                      ← CLI de Django
│
├── visor_vial/                                    ← paquete de configuración central
│   ├── __init__.py
│   ├── settings.py                                ← configuración global (DB, JWT, Cloudinary…)
│   ├── urls.py                                    ← enrutador raíz → api/v1/ + Swagger
│   ├── wsgi.py                                    ← entrada WSGI (producción)
│   └── asgi.py                                    ← entrada ASGI (WebSocket / futuro)
│
└── apps/                                          ← módulos de negocio
    ├── __init__.py
    │
    ├── usuarios/                                  ← autenticación, roles y límite diario
    │   ├── __init__.py
    │   ├── apps.py                                ← configuración de la app
    │   ├── models.py                              ← Usuario (AbstractBaseUser), Rol, reportes_hoy
    │   ├── serializers.py                         ← registro, login, perfil
    │   ├── views.py                               ← endpoints: register / login / perfil
    │   ├── urls.py                                ← rutas de la app
    │   ├── permissions.py                         ← permisos por rol (IsAdmin, IsCiudadano…)
    │   └── admin.py                               ← panel Django Admin
    │
    ├── vias/                                      ← segmentos viales (mapa temático RF13)
    │   ├── __init__.py
    │   ├── apps.py
    │   ├── models.py                              ← ViaSegmento (LineStringField / PostGIS)
    │   ├── serializers.py                         ← salida GeoJSON para Mapbox
    │   ├── views.py                               ← CRUD + endpoint GeoJSON
    │   ├── urls.py
    │   └── admin.py
    │
    ├── reportes/                                  ← UNIDAD DE ESTUDIO del proyecto
    │   ├── __init__.py
    │   ├── apps.py
    │   ├── models.py                              ← Reporte + RegistroTiempo (append-only)
    │   ├── serializers.py                         ← creación, detalle, cambio de estado
    │   ├── views.py                               ← crear / listar / actualizar estado
    │   ├── urls.py
    │   └── admin.py
    │
    ├── analisis/                                  ← Dimensiones 2 y 3 de la matriz
    │   ├── __init__.py
    │   ├── apps.py
    │   ├── models.py                              ← Coordenada (Dim.2) + Clasificacion (Dim.3)
    │   ├── serializers.py
    │   ├── views.py                               ← trigger IA + consulta resultados
    │   ├── urls.py
    │   ├── admin.py
    │   └── services/                             ← lógica de negocio desacoplada
    │       ├── __init__.py
    │       ├── gemini_service.py                  ← llama Gemini API → severidad + confianza
    │       └── geo_service.py                     ← calcula error_metros (UTM 17S / SRID 32717)
    │
    └── notificaciones/                            ← avisos internos por rol (RNF12)
        ├── __init__.py
        ├── apps.py
        ├── models.py                              ← TipoNotificacion + Notificacion
        ├── serializers.py
        ├── views.py
        ├── urls.py
        └── admin.py
```

---

## /frontend

```
frontend/
│
├── .env                                           ← variables de entorno reales (no versionar)
├── .env.example                                   ← plantilla: VITE_API_URL, VITE_MAPBOX_TOKEN
├── index.html                                     ← entrada HTML principal (Vite)
├── package.json                                   ← dependencias Node y scripts npm
├── vite.config.js                                 ← configuración Vite + proxy a :8000
├── tailwind.config.js                             ← tema, colores y plugins de Tailwind
├── postcss.config.js                              ← procesador CSS requerido por Tailwind
│
└── src/
    ├── main.jsx                                   ← monta React + BrowserRouter en el DOM
    ├── App.jsx                                    ← árbol de rutas principal
    │
    ├── assets/                                    ← recursos estáticos
    │   ├── images/                                ← logos, placeholders, íconos SVG
    │   └── fonts/                                 ← fuentes locales si aplica
    │
    ├── components/                                ← componentes reutilizables
    │   │
    │   ├── layout/                                ← estructura visual de la app
    │   │   ├── Navbar.jsx                         ← barra de navegación superior
    │   │   ├── Sidebar.jsx                        ← menú lateral por rol
    │   │   └── Footer.jsx
    │   │
    │   ├── map/                                   ← componentes Mapbox GL JS
    │   │   ├── MapaReportes.jsx                   ← pines de reportes sobre el mapa
    │   │   └── MapaSegmentos.jsx                  ← capa GeoJSON de vías (mapa temático)
    │   │
    │   ├── reportes/                              ← componentes del flujo de reporte
    │   │   ├── FormReporte.jsx                    ← formulario: foto + GPS + descripción
    │   │   ├── TarjetaReporte.jsx                 ← vista compacta de un reporte
    │   │   └── EstadoBadge.jsx                    ← chip de estado (PENDIENTE / RESUELTO…)
    │   │
    │   └── ui/                                    ← átomos genéricos de interfaz
    │       ├── Button.jsx
    │       ├── Modal.jsx
    │       ├── Alert.jsx
    │       └── Spinner.jsx
    │
    ├── pages/                                     ← vistas completas agrupadas por rol
    │   │
    │   ├── auth/                                  ← acceso público
    │   │   ├── Login.jsx
    │   │   └── Register.jsx
    │   │
    │   ├── ciudadano/                             ← rol CIUDADANO
    │   │   ├── Dashboard.jsx                      ← resumen de reportes propios
    │   │   └── NuevoReporte.jsx                   ← flujo completo de creación
    │   │
    │   ├── admin/                                 ← rol ADMIN
    │   │   ├── GestionReportes.jsx                ← tabla con filtros y cambio de estado
    │   │   └── GestionUsuarios.jsx                ← CRUD de usuarios
    │   │
    │   └── investigador/                          ← rol INVESTIGADOR
    │       ├── MapaTematico.jsx                   ← visualización geoespacial (RF13)
    │       └── ExportarSPSS.jsx                   ← descarga del dataset para análisis
    │
    ├── hooks/                                     ← lógica reutilizable con estado React
    │   ├── useAuth.js                             ← sesión JWT: token, usuario, rol
    │   ├── useReportes.js                         ← fetch y mutaciones de reportes
    │   └── useGeolocalizacion.js                  ← wrapper de la Geolocation API del browser
    │
    ├── services/                                  ← capa de comunicación con la API Django
    │   ├── api.js                                 ← instancia Axios base + interceptores JWT
    │   ├── authService.js                         ← login, register, refresh token
    │   ├── reportesService.js                     ← CRUD de reportes
    │   ├── viasService.js                         ← obtiene GeoJSON de segmentos
    │   └── analisisService.js                     ← consulta resultados IA y coordenadas
    │
    └── context/                                   ← estado global de la aplicación
        └── AuthContext.jsx                        ← proveedor: usuario autenticado + rol activo
```

---

## Resumen de responsabilidades

| Carpeta | Tecnología | Responsabilidad principal |
|---|---|---|
| `backend/visor_vial/` | Django | Configuración central del proyecto |
| `backend/apps/usuarios/` | DRF + JWT | Autenticación y control de acceso por rol |
| `backend/apps/vias/` | GeoDjango | Catálogo geoespacial de segmentos viales |
| `backend/apps/reportes/` | DRF | Unidad de estudio + pipeline de tiempos |
| `backend/apps/analisis/` | Gemini + PostGIS | Clasificación IA y precisión GPS |
| `backend/apps/notificaciones/` | DRF | Avisos internos del sistema |
| `frontend/src/components/` | React | Componentes reutilizables (UI, mapas, formularios) |
| `frontend/src/pages/` | React Router | Vistas agrupadas por rol de usuario |
| `frontend/src/services/` | Axios | Capa de abstracción de la API REST |
| `frontend/src/context/` | React Context | Estado global de autenticación |
