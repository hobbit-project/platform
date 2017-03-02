import { Injectable } from '@angular/core';

import Keycloak = require('keycloak-js');

// source: https://github.com/keycloak/keycloak/tree/master/examples/demo-template/angular2-product-app

@Injectable()
export class KeycloakService {

  private static auth: any = {};

  static init(): Promise<any> {
    KeycloakService.auth.loggedIn = false;
    KeycloakService.auth.withoutAuth = false;

    // need to use plain XMLHttpRequest here, as Http instance is instantiated after this call
    let promise = new Promise((resolve, reject) => {
      let req = new XMLHttpRequest();
      let url = `${webpack.BACKEND_URL}/rest/internal/keycloak-config`;
      req.open('GET', url, true);
      req.onload = (e) => {
        let content = req.responseText;
        let keycloakConfig = JSON.parse(content);

        if (keycloakConfig.withoutAuth) {
          KeycloakService.auth.loggedIn = true;
          KeycloakService.auth.withoutAuth = true;
          resolve(null);
        } else {
          let keycloakAuth: any = new Keycloak(keycloakConfig);
          keycloakAuth.init({onLoad: 'login-required'})
            .success(() => {
              KeycloakService.auth.loggedIn = true;
              KeycloakService.auth.authz = keycloakAuth;
              // KeycloakService.auth.logoutUrl = keycloakAuth.authServerUrl + '/realms/demo/tokens/logout?redirect_uri=/angular2-product/index.html';
              // KeycloakService.auth.logoutUrl = keycloakAuth.authz.authServerUrl + '/realms/demo/tokens/logout?redirect_uri=/angular2-product/index.html';
              resolve(null);
            })
            .error(() => {
              console.error('error on keycloak init');
              reject(null);
            });
        }
      };
      req.onerror = (e) => {
        console.error(`Error on fetching keycloak configuration: status = ${req.status} ${req.statusText}`);
        reject(null);
      };
      req.send(null);
    });

    return promise;
  }

  logout() {
    console.log('*** LOGOUT');
    KeycloakService.auth.loggedIn = false;
    KeycloakService.auth.withoutAuth = false;
    let authz = KeycloakService.auth.authz;
    KeycloakService.auth.authz = null;

    authz.logout();
    // window.location.href = KeycloakService.auth.logoutUrl;
  }

  isLoggedIn(): boolean {
    return KeycloakService.auth.loggedIn;
  }

  getToken(): Promise<string> {
    // used by transform() in customHttp.ts
    return new Promise<string>((resolve, reject) => {
      if (KeycloakService.auth.withoutAuth) {
        resolve('dev-token');
      } else if (KeycloakService.auth.authz.token) {
        KeycloakService.auth.authz.updateToken(5).success(function () {
          resolve(<string>KeycloakService.auth.authz.token);
        })
          .error(function () {
            reject('Failed to refresh token');
          });
      }
    });
  }
}
