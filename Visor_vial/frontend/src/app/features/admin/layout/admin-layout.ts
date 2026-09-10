import { Component, inject, signal } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { SessionService } from '../../../core/services/session.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-admin-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule],
  templateUrl: './admin-layout.html',
  styleUrl: './admin-layout.scss',
})
export class AdminLayout {
  session  = inject(SessionService);
  private router = inject(Router);

  sidebarColapsado = signal(false);

  toggleSidebar() {
    this.sidebarColapsado.update(v => !v);
  }

  cerrarSesion() {
    this.session.cerrar();
    this.router.navigate(['/auth/login']);
  }

  readonly navItems = [
    { ruta: '/admin/dashboard', icono: 'bi-speedometer2',   etiqueta: 'Inicio'    },
    { ruta: '/admin/reportes',  icono: 'bi-exclamation-triangle', etiqueta: 'Reportes'  },
    { ruta: '/admin/vias',      icono: 'bi-map',             etiqueta: 'Vías'      },
    { ruta: '/admin/usuarios',  icono: 'bi-people',          etiqueta: 'Usuarios'  },
  ];
}
