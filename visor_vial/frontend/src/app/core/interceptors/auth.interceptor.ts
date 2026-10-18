import { inject } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, switchMap, throwError } from 'rxjs';
import { Router } from '@angular/router';
import { SessionService } from '../services/session.service';
import { environment } from '../../../environments/environment';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const base = environment.apiUrl.replace(/\/$/, '');
  if (req.url !== base && !req.url.startsWith(base + '/')) return next(req);
  const session = inject(SessionService);
  const router = inject(Router);
  const authenticated = req.clone({ withCredentials: true });
  // Se obtiene el token actual tambien despues de entrar o cerrar sesion.
  const response = ['GET', 'HEAD', 'OPTIONS'].includes(req.method)
    ? next(authenticated)
    : inject(HttpClient).get<{ headerName: string; token: string }>(base + '/public/csrf', {
    withCredentials: true,
  }).pipe(switchMap(csrf => next(authenticated.clone({
    setHeaders: { [csrf.headerName]: csrf.token },
  }))));
  return response.pipe(catchError((error: HttpErrorResponse) => {
    if (error.status === 401 && !req.url.startsWith(base + '/public/')) {
      session.limpiarSesionLocal();
      void router.navigate(['/auth/login']);
    }
    return throwError(() => error);
  }));
};
