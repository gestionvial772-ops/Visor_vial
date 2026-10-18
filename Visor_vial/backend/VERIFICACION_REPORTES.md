# Reportes, busqueda y permisos

**Correccion posterior:** la lectura GET de `/admin/reportes` (incluidos detalle,
imagen y estadisticas) y `/admin/vias` vuelve a ser publica como antes, a peticion
del usuario. Las escrituras siguen requiriendo sesion y CSRF. Se verifico la
lectura real contra Supabase desde el backend corregido en el puerto 8080,
incluyendo busqueda por nombre y el conteo con distintos tamanios de pagina.
No se crearon ni borraron registros durante esa comprobacion.

**Excepcion temporal solicitada:** el control de rol ADMIN esta comentado en
`SecurityConfig.java` y `app.routes.ts` para probar ambas ventanas con una cuenta
ciudadana. Cualquier usuario autenticado puede usar el panel administrativo,
incluidas sus acciones. El inicio de sesion y CSRF siguen activos. Para restaurar
la separacion de roles, reactivar las lineas comentadas y retirar las alternativas
temporales; ajustar tambien las pruebas que documentan esta excepcion.

Cambios acotados a reportes y autenticacion. No se modificaron `.env`, las
propiedades de conexion, versiones Java/Gradle, dependencias ni el esquema de BD.
En Storage se clonaron los constructores de clientes HTTP para que las descargas
externas no hereden la cabecera `apikey` del cliente Supabase.

## Comportamiento

- Busqueda por ID de reporte, descripcion, nombre de via o codigo de segmento.
  El LEFT JOIN conserva tambien los reportes sin via. El codigo sigue siendo texto
  para mantener ceros iniciales, como `067500`.
- El DTO devuelve `nombreVia` y `codigoSegmento`; la tabla y los detalles los muestran.
- El servidor valida que la via seleccionada exista y guarda su `id_via`.
- Login crea una sesion de Spring Security. Las peticiones Angular llevan cookie
  y las escrituras llevan CSRF. Se debe iniciar sesion nuevamente tras actualizar.
- `/admin/reportes/**` requiere ADMIN. CIUDADANO puede crear reportes, consultar
  vias y consultar `/reportes/mios`. La imagen ciudadana exige ser propietario.
- El usuario creador se toma de la sesion, nunca del JSON ni de un ID fijo.
- Logout invalida la sesion del servidor. Los roles del navegador no dan permisos.
- Anular actualiza `eliminado_en`; conserva fila, coordenada, imagen y bitacora.
  Listados, estadisticas, detalles, imagenes y reanalisis excluyen anulados.
- Si falla el flujo transaccional se revierte la BD y se intenta limpiar el objeto
  de Storage, incluso si falla el commit. Una caida de Storage puede impedir esa
  limpieza; no existe una transaccion distribuida entre PostgreSQL y Storage.

## Verificacion local

```powershell
cd backend
.\gradlew.bat test --tests '*ReporteServiceTest' --tests '*SeguridadReportesTest' --tests '*ReporteConsultasTest' --offline
cd ../frontend
npm.cmd run build -- --configuration development
```

Las pruebas de servicio simulan BD y Storage. Las pruebas HTTP usan la cadena real
de Spring Security. La prueba de consultas valida JPQL y el mapeo con Hibernate
sin conectarse a PostgreSQL. No ejecutar `contextLoads` como sustituto de estas
pruebas: inicia la configuracion real del proyecto.

## Comprobacion pendiente en Supabase real

1. Iniciar sesion como ciudadano, seleccionar una via conocida y registrar una foto.
2. Confirmar que aparece en Mis reportes de ese usuario y no en otro usuario.
3. Como ADMIN, buscar por ID, nombre, codigo (incluidos ceros iniciales) y descripcion.
   Revisar detalle e imagen y comprobar la paginacion con varios resultados.
4. Ejecutar `verificacion-reportes.sql` en el SQL Editor: contrastar usuario, via,
   coordenada y existencia del objeto en el bucket configurado. Las imagenes por
   URL externa del formulario administrativo no son objetos de Storage.
5. Anular ese reporte desde ADMIN. Repetir las consultas: la fila debe conservarse
   con `eliminado_en`, mientras los totales activos y los listados ya no la incluyen.
6. Si un trigger de la BD reasigna la via al insertar coordenadas, revisar su
   funcion antes de modificarlo. El codigo Java no permite verificar reglas que
   solo existen en el servidor PostgreSQL.

Estas comprobaciones remotas no se ejecutaron automaticamente. No se crearon ni
anularon reportes reales para las pruebas locales.

Referencia de sesiones y CSRF:
[Spring Security](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html).
