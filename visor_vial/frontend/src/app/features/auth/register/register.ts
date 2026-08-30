import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { RouterLink } from '@angular/router';

function contrasenasIgualesValidator(grupo: AbstractControl): ValidationErrors | null{
  const contrasena = grupo.get('contrasena')?.value;
  const confirmar = grupo.get('confirmarContrasena')?.value;
  return contrasena === confirmar ? null : { contrasenasNoCoinciden: true };
}

@Component({
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink
  ],
  selector: 'app-register',
  styleUrl: './register.scss',
  templateUrl: './register.html',
})
export class Register {
  formulario: FormGroup;

  constructor(private fb: FormBuilder) {
    this.formulario = this.fb.group(
      {
        nombres: ['', Validators.required],
        apellidos: ['', Validators.required],
        correoElectronico: ['', [Validators.required, Validators.email]],
        dni: ['', [Validators.required, Validators.pattern(/^\d{8}$/)]],
        contrasena: ['', [Validators.required, Validators.minLength(8)]],
        confirmarContrasena: ['', Validators.required], 
        aceptaTerminos: [false, Validators.requiredTrue],
        captcha: ['', Validators.required],
      },
      { validators: contrasenasIgualesValidator },
    );
  }

  onSubmit(): void {
    if(this.formulario.invalid){
      this.formulario.markAllAsTouched();
      return;
    }

    //Si leen esto, desde aqui se conectara al service
    console.log("Registro enviado: ", this.formulario.value)
  }
}
