import { Component, inject, signal, OnInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Pipe, PipeTransform } from '@angular/core';

import {
  UsuariosService,
  EstadisticasUsuarios,
  UsuarioFila,
} from '../../../../core/services/usuarios.service';
import { Rol } from '../../../../core/models/usuario.model';
import { ToastService } from '../../../../core/services/toast.service';

/** Pipe helper: devuelve el menor de dos números (usado en la paginación del pie) */
@Pipe({ name: 'min', standalone: true })
export class MinPipe implements PipeTransform {
  transform(values: number[]): number {
    return Math.min(...values);
  }
}

@Component({
  selector: 'app-usuarios',
  imports: [CommonModule, FormsModule, MinPipe],
  templateUrl: './usuarios.html',
  styleUrl: './usuarios.scss',
})
export class Usuarios implements OnInit {
  private svc = inject(UsuariosService);
  private toast = inject(ToastService);

  usuarios = signal<UsuarioFila[]>([]);
  estadisticas = signal<EstadisticasUsuarios | null>(null);

  cargando = signal(false);
  errorCarga = signal(false);
  mensajeErrorCarga = signal('');

  paginaActual = signal(0);
  totalPaginas = signal(0);
  totalElementos = signal(0);
  readonly tamanio = 10;

  filtroRol = '';
  filtroBusqueda = '';

  readonly rolesOpciones = [
    { valor: '', etiqueta: 'Todos' },
    { valor: 'CIUDADANO', etiqueta: 'Ciudadanos' },
    { valor: 'ADMIN', etiqueta: 'Administradores' },
  ];

  // ── Edicion en linea ───────────────────────────────────────────────────────
  editando = signal<UsuarioFila | null>(null);
  guardando = signal(false);

  borradorNombre = '';
  borradorApellidos = '';
  borradorCorreo = '';
  errorFormulario = signal('');

  // ── Cambio de rol ─────────────────────────────────────────────────────────
  confirmandoRol = signal<UsuarioFila | null>(null);
  rolPropuesto = signal<Rol>('CIUDADANO');
  cambiandoRol = signal(false);

  ngOnInit(): void {
    this.cargarUsuarios();
    this.cargarEstadisticas();
  }

  /** Escape cierra el modal abierto, que es lo que espera el teclado. */
  @HostListener('document:keydown.escape')
  alPulsarEscape(): void {
    if (this.confirmandoRol()) {
      if (!this.cambiandoRol()) {
        this.cerrarCambioRol();
      }
      return;
    }

    this.cancelarEdicion();
  }

  cargarUsuarios(pagina = 0): void {
    this.cargando.set(true);
    this.errorCarga.set(false);

    this.svc
      .listar(pagina, this.tamanio, this.filtroRol || undefined, this.filtroBusqueda || undefined)
      .subscribe({
        next: (p) => {
          this.usuarios.set(p.contenido);
          this.totalElementos.set(p.totalElementos);
          this.totalPaginas.set(p.totalPaginas);
          this.paginaActual.set(p.paginaActual);
          this.cargando.set(false);
        },
        error: (error) => {
          this.cargando.set(false);
          this.errorCarga.set(true);
          this.mensajeErrorCarga.set(
            error.status === 401
              ? 'Tu sesión venció. Vuelve a iniciar sesión.'
              : error.status === 0
                ? 'No se pudo conectar con el servidor. Comprueba que el backend esté encendido.'
                : 'El servidor no pudo cargar los usuarios. Intenta nuevamente.',
          );
        },
      });
  }

  cargarEstadisticas(): void {
    this.svc.estadisticas().subscribe({
      next: (e) => this.estadisticas.set(e),
      error: () => {
        /* las tarjetas son informativas: si fallan, el listado sigue igual */
      },
    });
  }

  aplicarFiltro(): void {
    this.cargarUsuarios(0);
  }

  limpiarFiltros(): void {
    this.filtroRol = '';
    this.filtroBusqueda = '';
    this.cargarUsuarios(0);
  }

  irAPagina(pagina: number): void {
    if (pagina < 0 || pagina >= this.totalPaginas()) {
      return;
    }
    this.cargarUsuarios(pagina);
  }

  paginasVisibles(): number[] {
    const total = this.totalPaginas();
    const actual = this.paginaActual();
    const ventana = 2;
    const inicio = Math.max(0, actual - ventana);
    const fin = Math.min(total - 1, actual + ventana);
    const arr: number[] = [];

    for (let i = inicio; i <= fin; i++) {
      arr.push(i);
    }

    return arr;
  }

  // ── Edicion ────────────────────────────────────────────────────────────────
  abrirEdicion(u: UsuarioFila): void {
    this.editando.set(u);
    this.borradorNombre = u.nombre;
    this.borradorApellidos = u.apellidos;
    this.borradorCorreo = u.correoElectronico;
    this.errorFormulario.set('');
  }

  cancelarEdicion(): void {
    // While the request is in flight the modal must not close, or the admin
    // would lose sight of the error if the save fails.
    if (this.guardando()) {
      return;
    }

    this.editando.set(null);
    this.errorFormulario.set('');
  }

  guardarEdicion(): void {
    const u = this.editando();
    if (!u || this.guardando()) {
      return;
    }

    const nombre = this.borradorNombre.trim();
    const apellidos = this.borradorApellidos.trim();
    const correo = this.borradorCorreo.trim();

    if (!nombre || !apellidos) {
      this.errorFormulario.set('El nombre y los apellidos son obligatorios.');
      return;
    }

    if (!correo.match(/^[^@\s]+@[^@\s]+\.[^@\s]+$/)) {
      this.errorFormulario.set('El correo no tiene un formato válido.');
      return;
    }

    this.guardando.set(true);
    this.errorFormulario.set('');

    this.svc.actualizar(u.idUsuario, { nombre, apellidos, correoElectronico: correo }).subscribe({
      next: (actualizado) => {
        this.usuarios.update((lista) =>
          lista.map((x) => (x.idUsuario === actualizado.idUsuario ? actualizado : x)),
        );
        this.guardando.set(false);
        this.editando.set(null);
        this.toast.mostrar(`Datos de ${actualizado.nombre} actualizados`, true, 'Usuario actualizado');
      },
      error: (err) => {
        this.guardando.set(false);
        console.error('No se pudo actualizar el usuario:', err);
        this.errorFormulario.set(this.mensajeError(err, 'No se pudo guardar el usuario.'));
      },
    });
  }

  // ── Cambio de rol ─────────────────────────────────────────────────────────
  abrirCambioRol(u: UsuarioFila): void {
    this.confirmandoRol.set(u);
    this.rolPropuesto.set(u.rol);
  }

  cerrarCambioRol(): void {
    if (this.cambiandoRol()) {
      return;
    }

    this.confirmandoRol.set(null);
  }

  confirmarCambioRol(): void {
    const u = this.confirmandoRol();
    if (!u || this.cambiandoRol()) {
      return;
    }

    const destino = this.rolPropuesto();

    if (destino === u.rol) {
      this.confirmandoRol.set(null);
      return;
    }

    this.cambiandoRol.set(true);

    this.svc.cambiarRol(u.idUsuario, destino).subscribe({
      next: (actualizado) => {
        this.usuarios.update((lista) =>
          lista.map((x) => (x.idUsuario === actualizado.idUsuario ? actualizado : x)),
        );
        this.cambiandoRol.set(false);
        this.confirmandoRol.set(null);
        this.cargarEstadisticas();
        this.toast.mostrar(
          `${actualizado.nombre} ahora es ${this.etiquetaRol(actualizado.rol)}`,
          true,
          'Rol actualizado',
        );
      },
      error: (err) => {
        this.cambiandoRol.set(false);
        console.error('No se pudo cambiar el rol:', err);
        this.toast.mostrar(this.mensajeError(err, 'No se pudo cambiar el rol.'), false, 'Falló el cambio');
      },
    });
  }

  /** Traduce el fallo del backend a algo que el admin pueda entender. */
  private mensajeError(err: unknown, porDefecto: string): string {
    const status = (err as { status?: number })?.status;
    const backend = (err as { error?: { message?: string; detail?: string } })?.error;

    if (status === 0) {
      return 'No hay conexión con el servidor.';
    }

    if (status === 409) {
      return 'Ese correo ya está registrado en otra cuenta.';
    }

    return backend?.message || backend?.detail || porDefecto;
  }

  // ── Presentacion ───────────────────────────────────────────────────────────
  etiquetaRol(rol: string): string {
    return rol === 'ADMIN' ? 'Administrador' : 'Ciudadano';
  }

  iniciales(u: UsuarioFila): string {
    const a = (u.nombre || '').trim().charAt(0);
    const b = (u.apellidos || '').trim().charAt(0);
    return (a + b).toUpperCase() || '—';
  }

  nombreCompleto(u: UsuarioFila): string {
    return `${u.nombre} ${u.apellidos}`.trim();
  }

  exportarCsv(): void {
    const headers = ['ID', 'DNI', 'Nombre', 'Apellidos', 'Correo', 'Rol', 'Reportes', 'Registrado'];
    const filas = this.usuarios().map((u) => [
      u.idUsuario,
      u.dni,
      `"${(u.nombre || '').replace(/"/g, '""')}"`,
      `"${(u.apellidos || '').replace(/"/g, '""')}"`,
      u.correoElectronico,
      u.rol,
      u.totalReportes,
      u.fechaCreacion,
    ]);
    const csv = [headers, ...filas].map((f) => f.join(',')).join('\n');
    const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `usuarios_${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }
}