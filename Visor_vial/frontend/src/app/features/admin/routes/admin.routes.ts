import { Routes } from '@angular/router';
import { AdminLayout } from '../layout/admin-layout';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    component: AdminLayout,
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('../pages/inicio/inicio').then(m => m.Inicio),
      },
      {
        path: 'reportes',
        loadComponent: () => import('../pages/reportes/reportes').then(m => m.Reportes),
      },
      {
        path: 'usuarios',
        loadComponent: () => import('../pages/usuarios/usuarios').then(m => m.Usuarios),
      },
      {
        path: 'vias',
        loadComponent: () => import('../pages/vias/vias').then(m => m.Vias),
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    ],
  },
];
