import { KeycloakService } from './app/auth/keycloak.service';
import { enableProdMode } from '@angular/core';
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';

import { AppModule } from './app/app.module';
import { environment } from './environments/environment';

if (environment.production) {
  enableProdMode();
}

KeycloakService.init().then(() =>
  platformBrowserDynamic().bootstrapModule(AppModule)
);
