import { Component, Input, ElementRef, OnChanges, OnInit } from '@angular/core';
import { Observable } from 'rxjs/Rx';

import { UserInfo } from './model';
import { KeycloakService } from './services/keycloakService';
import { BackendService } from './services/backend.service';

@Component({
  // tslint:disable-next-line
  selector: '[sg-app-menuitem]',
  template: `<a [routerLink]="enabled ? routerLink : ['/']">{{label}}</a>`
})
export class MenuItemComponent implements OnChanges {
  // tslint:disable-next-line
  @Input('x-routerLink') routerLink: any;
  @Input() label: string;
  @Input() role: string;
  enabled: boolean;
  private el: ElementRef;

  constructor(el: ElementRef, private bs: BackendService) {
    this.el = el;
  }

  ngOnChanges() {
    this.enabled = false;
    this.updateElem();
    this.bs.userInfo().subscribe(userInfo => {
      this.enabled = userInfo.roles.findIndex(r => r === this.role) !== -1;
      this.updateElem();
    });
  }

  private updateElem() {
    let elem: HTMLElement = this.el.nativeElement;
    if (!this.enabled) {
      elem.setAttribute('class', 'disabled');
    } else {
      elem.removeAttribute('class');
    }
  }
}

@Component({
  selector: 'sg-app-navbar',
  styles: [require('./navbar.comp.css')],
  template: require('./navbar.comp.html')
})
export class NavbarComponent implements OnInit {
  roles: string[];
  userInfo: UserInfo;

  constructor(private bs: BackendService, private keycloak: KeycloakService) {
  }

  ngOnInit() {
    this.bs.userInfo().subscribe(data => {
      this.userInfo = data;
      this.roles = data.roles;
      console.info(`role names: ${this.roles}`);
    });
  }

  isPermitted(name: string): boolean {
    return true;
  }

  
  logout(event) {
    this.keycloak.logout();
    event.preventDefault();
  }
}
