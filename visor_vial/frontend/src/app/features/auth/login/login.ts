import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { RecaptchaFormsModule, RecaptchaModule } from 'ng-recaptcha-2';
import { CampoPassword } from '../../../shared/components/campo-password/campo-password';
import { CampoTexto } from '../../../shared/components/campo-texto/campo-texto';
import { ImagenConReloj } from "../../../shared/components/imagen-con-reloj/imagen-con-reloj";
@Component({
  imports: [CommonModule, ReactiveFormsModule, RouterLink, RecaptchaFormsModule, RecaptchaModule, CampoPassword, CampoTexto, ImagenConReloj],
  selector: 'app-login',
  styleUrl: './login.scss',
  templateUrl: './login.html',
})
export class Login {

  formulario: FormGroup;


  constructor(private fb: FormBuilder){
    this.formulario = this.fb.group({
      dni: ['', [Validators.required, Validators.pattern(/^\d{8}$/)]],
      contrasena: ['', Validators.required],
      recordarme: [false],
      captcha: ['', Validators.required]

    })
  }

  onSubmit(): void{
    if(this.formulario.invalid){
      this.formulario.markAllAsTouched();
      return;
    }

    console.log('Login enviado:', this.formulario.value);

  }





}
