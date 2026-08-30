import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { registerLocaleData } from '@angular/common';
import { LOCALE_ID} from '@angular/core';
import localeEsPe from '@angular/common/locales/es-PE';
import { RECAPTCHA_SETTINGS, RecaptchaSettings } from 'ng-recaptcha-2';
import { environment } from '../environments/environment';

registerLocaleData(localeEsPe, 'es-PE');

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    {provide: LOCALE_ID, useValue: 'es-PE'},
    {provide: RECAPTCHA_SETTINGS, useValue: {siteKey: environment.VITE_RECAPTCHA_SITE_KEY} as RecaptchaSettings}
    
  ]
};
