import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
@Component({
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  selector: 'app-login',
  styleUrl: './login.scss',
  templateUrl: './login.html',
})
export class Login {

  formulario: FormGroup;
  mostrarContrasena = false;


  constructor(private fb: FormBuilder){
    this.formulario = this.fb.group({
      dni: ['', [Validators.required, Validators.pattern(/^\d{8}$/)]],
      contrasena: ['', Validators.required],
      recordarme: [false]
    })
  }



  alternarVisibilidadContrasena():void{
    this.mostrarContrasena = !this.mostrarContrasena
  }


  onSubmit(): void{
    if(!this.formulario.invalid){
      this.formulario.markAllAsTouched();
      return;
    }

    console.log('Login enviado:', this.formulario.value);

  }





}
