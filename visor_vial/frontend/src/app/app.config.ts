import { ApplicationConfig, 
        provideBrowserGlobalErrorListeners, 
        LOCALE_ID } from '@angular/core';

import { registerLocaleData } from '@angular/common';
import localeEsPe from '@angular/common/locales/es-PE';

import { provideRouter } from '@angular/router';

import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { RECAPTCHA_SETTINGS, RecaptchaSettings } from 'ng-recaptcha-2';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { environment } from '../environments/environment';

registerLocaleData(localeEsPe, 'es-PE');

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(
      withInterceptors(
        [authInterceptor]
      )
    ),
    {provide: LOCALE_ID, useValue: 'es-PE'},
    {provide: RECAPTCHA_SETTINGS, useValue: {siteKey: environment.VITE_RECAPTCHA_SITE_KEY} as RecaptchaSettings}
  ]
};
