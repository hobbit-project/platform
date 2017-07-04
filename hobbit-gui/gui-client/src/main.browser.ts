import { enableProdMode } from '@angular/core';
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import { AppModule } from './app/app.module';

import { KeycloakService } from './app/services/keycloakService';

if (webpack.ENV === 'production') {
  enableProdMode();
}

KeycloakService.init().then(
  o => {
    platformBrowserDynamic().bootstrapModule(AppModule).catch(console.error.bind(console));
  }, rejected => {
    console.error(rejected);
  });
