import { KeycloakService } from './keycloak.service';
import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot } from '@angular/router';

import { Role } from '../model';

@Injectable()
export class AuthGuardService implements CanActivate {

  constructor(private keycloak: KeycloakService) { }

  canActivate(route: ActivatedRouteSnapshot) {
    console.log('AuthGuard ' + this.keycloak.isLoggedIn());
    return true;
  }


  getMinimumAllowedRole(route: ActivatedRouteSnapshot): Role {
    if (route.data['roles'] === undefined)
      return Role.UNAUTHORIZED;

    const roles = route.data['roles'] as Array<Role>;
    if (roles.length === 0)
      return Role.UNAUTHORIZED;
    if (roles.includes(Role.UNAUTHORIZED))
      return Role.UNAUTHORIZED;
    if (roles.includes(Role.GUEST))
      return Role.GUEST;

    return Role.CHALLENGE_ORGANISER;
  }

}
