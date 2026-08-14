from django.db import models
from django.contrib.auth.models import AbstractBaseUser, PermissionsMixin, BaseUserManager


class UsuarioManager(BaseUserManager):
	def create_user(self, dni, email=None, password=None, **extra_fields):
		if not dni:
			raise ValueError('El dni es obligatorio')
		email = self.normalize_email(email)
		user = self.model(dni=dni, email=email, **extra_fields)
		user.set_password(password)
		user.save(using=self._db)
		return user

	def create_superuser(self, dni, email, password=None, **extra_fields):
		extra_fields.setdefault('is_staff', True)
		extra_fields.setdefault('is_superuser', True)
		return self.create_user(dni, email, password, **extra_fields)


class Usuario(AbstractBaseUser, PermissionsMixin):
	dni = models.CharField(max_length=8, unique=True)
	email = models.EmailField(max_length=150, unique=True, null=True, blank=True)
	nombre = models.CharField(max_length=100)
	apellidos = models.CharField(max_length=100)
	rol = models.CharField(max_length=15, choices=(('ADMIN','ADMIN'),('CIUDADANO','CIUDADANO'),('INVESTIGADOR','INVESTIGADOR')), default='CIUDADANO')
	reportes_hoy = models.SmallIntegerField(default=0)
	fecha_ultimo_reporte = models.DateField(null=True, blank=True)
	is_active = models.BooleanField(default=True)
	is_staff = models.BooleanField(default=False)

	objects = UsuarioManager()

	USERNAME_FIELD = 'dni'
	REQUIRED_FIELDS = ['email']

	def __str__(self):
		return f"{self.dni} - {self.nombre} {self.apellidos}"

