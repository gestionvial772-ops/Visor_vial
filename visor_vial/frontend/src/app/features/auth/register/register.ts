import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';

import { Router, RouterLink } from '@angular/router';
import { RecaptchaFormsModule, RecaptchaModule } from 'ng-recaptcha-2';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { UsuarioRegistro } from '../../../core/models/usuario.model';

function contrasenasIgualesValidator(grupo: AbstractControl): ValidationErrors | null {
  const contrasena = grupo.get('contrasena')?.value;
  const confirmar = grupo.get('confirmarContrasena')?.value;
  return contrasena === confirmar
    ? null
    : {
        contrasenasNoCoinciden: true,
      };
}

@Component({
  imports: [CommonModule, ReactiveFormsModule, RouterLink, RecaptchaFormsModule, RecaptchaModule],
  selector: 'app-register',
  styleUrl: './register.scss',
  templateUrl: './register.html',
})
export class Register {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  enviando = false;

  formulario: FormGroup = this.fb.group(
    {
      nombre: ['', [Validators.required, Validators.maxLength(100)]],
      apellidos: ['', [Validators.required, Validators.maxLength(100)]],
      correoElectronico: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
      dni: ['', [Validators.required, Validators.pattern(/^\d{8}$/)]],
      contrasena: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
      confirmarContrasena: ['', Validators.required],
      aceptaTerminos: [false, Validators.requiredTrue],
      captcha: ['', Validators.required],
    },

    {
      validators: contrasenasIgualesValidator,
    },
  );

  onSubmit(): void {
    console.log('CLICK EN CREAR CUENTA');

    console.log('Formulario válido:', this.formulario.valid);

    console.log('Valores:', this.formulario.value);

    console.log('Errores formulario:', this.formulario.errors);

    Object.keys(this.formulario.controls).forEach((nombreControl) => {
      const control = this.formulario.get(nombreControl);

      console.log(nombreControl, {
        valor: control?.value,
        valido: control?.valid,
        errores: control?.errors,
      });
    });

    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();

      this.toast.mostrar('Revisa los campos del formulario antes de continuar.', false);

      return;
    }

    const captchaToken = this.formulario.value.captcha;

    if (!captchaToken) {
      this.toast.mostrar('Debes completar el CAPTCHA.', false);

      return;
    }

    const datos: UsuarioRegistro = {
      nombre: this.formulario.value.nombre,
      apellidos: this.formulario.value.apellidos,
      correoElectronico: this.formulario.value.correoElectronico,
      dni: this.formulario.value.dni,
      contrasena: this.formulario.value.contrasena,
      captchaToken,
    };

    this.enviando = true;

    this.auth
      .registrarse(datos)

      .pipe(
        finalize(() => {
          this.enviando = false;
        }),
      )

      .subscribe({
        next: (respuesta) => {
          this.toast.mostrar(respuesta.mensaje, true);

          this.router.navigate(['/auth/login']);
        },

        error: (error) => {
          console.error('Error registrando usuario:', error);
          const mensaje = error?.error?.mensaje ?? 'No fue posible crear la cuenta.';
          this.toast.mostrar(mensaje, false);
        },
      });
  }
}
