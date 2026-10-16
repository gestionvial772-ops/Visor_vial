import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { authInterceptor } from './auth.interceptor';
import { SessionService } from '../services/session.service';
import { rolGuard } from '../guards/rol.guard';
import { routes } from '../../app.routes';
import { environment } from '../../../environments/environment';

describe('Acceso a reportes', () => {
  let http: HttpClient;
  let requests: HttpTestingController;
  let session: SessionService;
  let router: Router;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [
      provideHttpClient(withInterceptors([authInterceptor])),
      provideHttpClientTesting(), provideRouter([]),
    ] });
    http = TestBed.inject(HttpClient);
    requests = TestBed.inject(HttpTestingController);
    session = TestBed.inject(SessionService);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => { requests.verify(); sessionStorage.clear(); });

  it('permite temporalmente entrar al panel con una cuenta ciudadana', () => {
    const admin = routes.find(route => route.path === 'admin')!;
    expect(admin.data?.['roles']).toBeUndefined();
    expect(admin.canActivate).not.toContain(rolGuard);
    session.iniciar({ nombre: 'Fabricio', rol: 'CIUDADANO' });
    const result = TestBed.runInInjectionContext(() => rolGuard(
      { data: admin.data } as ActivatedRouteSnapshot, {} as RouterStateSnapshot));
    expect(result).toBe(true);
  });

  it('permite al administrador abrir el panel', () => {
    session.iniciar({ nombre: 'Admin', rol: 'ADMIN' });
    expect(TestBed.runInInjectionContext(() => rolGuard(
      { data: { roles: ['ADMIN'] } } as unknown as ActivatedRouteSnapshot, {} as RouterStateSnapshot))).toBe(true);
  });

  it('envia cookie y limpia la sesion obsoleta al recibir 401', () => {
    session.iniciar({ nombre: 'Admin', rol: 'ADMIN' });
    http.get(environment.apiUrl + '/admin/reportes').subscribe({ error: () => {} });
    const req = requests.expectOne(environment.apiUrl + '/admin/reportes');
    expect(req.request.withCredentials).toBe(true);
    req.flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(session.estaAutenticado()).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/auth/login']);
  });

  it('no confunde falta de permiso con sesion vencida', () => {
    session.iniciar({ nombre: 'Fabricio', rol: 'CIUDADANO' });
    http.get(environment.apiUrl + '/admin/reportes').subscribe({ error: () => {} });
    requests.expectOne(environment.apiUrl + '/admin/reportes')
      .flush({}, { status: 403, statusText: 'Forbidden' });
    expect(session.estaAutenticado()).toBe(true);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('conserva CSRF para escrituras', () => {
    http.delete(environment.apiUrl + '/admin/reportes/70').subscribe();
    requests.expectOne(environment.apiUrl + '/public/csrf')
      .flush({ headerName: 'X-CSRF-TOKEN', token: 'prueba' });
    const req = requests.expectOne(environment.apiUrl + '/admin/reportes/70');
    expect(req.request.headers.get('X-CSRF-TOKEN')).toBe('prueba');
    expect(req.request.withCredentials).toBe(true);
    req.flush({});
  });
});
