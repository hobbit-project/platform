import { environment } from './../../environments/environment';
import { Injectable } from '@angular/core';
import * as Keycloak from 'keycloak-js';


@Injectable()
export class KeycloakService {
  private static auth: any = {};

  static init(): Promise<any> {
    KeycloakService.auth.loggedIn = false;
    KeycloakService.auth.withoutAuth = false;

    // need to use plain XMLHttpRequest here, as Http instance is instantiated after this call
    const promise = new Promise((resolve, reject) => {
      const req = new XMLHttpRequest();
      req.open('GET', environment.backendUrl + '/rest/internal/keycloak-config', true);
      req.onload = (e) => {
        const keycloakConfig = JSON.parse(req.responseText);

        const keycloakAuth: any = Keycloak(keycloakConfig);
        keycloakAuth.init({ onLoad: 'login-required' })
          .success(() => {
            KeycloakService.auth.loggedIn = true;
            KeycloakService.auth.authz = keycloakAuth;
            resolve(null);
          })
          .error((response) => {
            console.error('error on keycloak init');
            reject(null);
          });
      };
      req.onerror = (e) => {
        console.error(`Error on fetching keycloak configuration: status = ${req.status} ${req.statusText}`);
        document.getElementById('rootError').innerHTML = 'Failed to init user authentication. Please contact a system administrator.';

        reject('Unable to query keycloak config.');
      };
      req.send(null);
    });

    return promise;
  }

  isLoggedIn() {
    return KeycloakService.auth.loggedIn;
  }

  logout() {
    console.log('*** LOGOUT');
    KeycloakService.auth.loggedIn = false;

    const authz = KeycloakService.auth.authz;
    KeycloakService.auth.authz = null;
    authz.logout();
  }

  getToken(): Promise<string> {
    return new Promise<string>((resolve, reject) => {
      if (KeycloakService.auth.authz.token) {
        KeycloakService.auth.authz.updateToken(5)
          .success(() => {
            resolve(<string>KeycloakService.auth.authz.token);
          })
          .error(() => {
            reject('Failed to refresh token');
          });
      }
    });
  }
}
