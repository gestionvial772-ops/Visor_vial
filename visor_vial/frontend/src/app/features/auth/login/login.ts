import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { RecaptchaFormsModule, RecaptchaModule } from 'ng-recaptcha-2';
import { CampoPassword } from '../../../shared/components/campo-password/campo-password';
import { CampoTexto } from '../../../shared/components/campo-texto/campo-texto';
import { ImagenConReloj } from '../../../shared/components/imagen-con-reloj/imagen-con-reloj';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { SessionService } from '../../../core/services/session.service';

@Component({
  imports: [CommonModule, ReactiveFormsModule, RouterLink,
            RecaptchaFormsModule, RecaptchaModule,
            CampoPassword, CampoTexto, ImagenConReloj],
  selector: 'app-login',
  styleUrl: './login.scss',
  templateUrl: './login.html',
})
export class Login {
  formulario: FormGroup;
  private fb       = inject(FormBuilder);
  private auth     = inject(AuthService);
  private toast    = inject(ToastService);
  private session  = inject(SessionService);
  private router   = inject(Router);

  constructor() {
    this.formulario = this.fb.group({
      dni:        ['', [Validators.required, Validators.pattern(/^\d{8}$/)]],
      contrasena: ['', Validators.required],
      recordarme: [false],
      captcha:    ['', Validators.required],
    });
  }

  onSubmit(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    const { dni, contrasena } = this.formulario.value;
    this.auth.iniciarSesion({ dni, contrasena }).subscribe({
      next: (res) => {
        if (res.exito && res.nombre && res.rol) {
          this.session.iniciar({ nombre: res.nombre, rol: res.rol });
          this.toast.mostrar(res.mensaje, true);
          // Redirigir según rol
          if (res.rol === 'ADMIN') {
            this.router.navigate(['/admin/dashboard']);
          } else {
            this.router.navigate(['/ciudadano/dashboard']);
          }
        } else {
          this.toast.mostrar(res.mensaje, false);
        }
      },
      error: () => this.toast.mostrar('Error de conexión con el servidor', false),
    });
  }
}
