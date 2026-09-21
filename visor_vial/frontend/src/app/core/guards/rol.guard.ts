import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { SessionService } from '../services/session.service';

export const rolGuard: CanActivateFn = (route) => {
  const session = inject(SessionService);

  const router = inject(Router);

  const usuario = session.usuario();

  if (!usuario) {
    return router.createUrlTree(['/auth/login']);
  }

  
  const rolesPermitidos = route.data?.['roles'] as string[] | undefined;

  if (!rolesPermitidos) {
    return true;
  }

  if (rolesPermitidos.includes(usuario.rol)) {
    return true;
  }

  if (usuario.rol === 'ADMIN') {
    return router.createUrlTree(['/admin/dashboard']);
  }

  return router.createUrlTree(['/ciudadano/nuevo-reporte']);
};
