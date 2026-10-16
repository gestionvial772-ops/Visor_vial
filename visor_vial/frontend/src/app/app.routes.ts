import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
// import { rolGuard } from './core/guards/rol.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'auth/login', pathMatch: 'full' },
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/routes/auth.routes').then((m) => m.AUTH_ROUTES),
  },

  {
    path: 'ciudadano',

    // Temporal: acceso a ambas ventanas mientras se crea la cuenta ADMIN.
    // canActivate: [authGuard, rolGuard],
    canActivate: [authGuard],

    loadChildren: () =>
      import('./features/ciudadano/routes/ciudadano.routes').then((m) => m.CIUDADANO_ROUTES),
  },
  {
    path: 'admin',
    // Temporal: reactivar junto con hasRole("ADMIN") en SecurityConfig.
    // data: { roles: ['ADMIN'] },
    // canActivate: [authGuard, rolGuard],
    canActivate: [authGuard],
    loadChildren: () => import('./features/admin/routes/admin.routes').then((m) => m.ADMIN_ROUTES),
  },
  { path: '**', redirectTo: 'auth/login' },
];
