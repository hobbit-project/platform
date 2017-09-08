import { User, Role } from './../model';
import { BackendService } from './../backend.service';
import { KeycloakService } from './../auth/keycloak.service';
import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent implements OnInit {

  public Role = Role;

  private userInfo: User;

  constructor(private keycloak: KeycloakService, private backend: BackendService) {
    this.backend.userInfo().subscribe(data => {
      this.userInfo = data;
    });
  }

  ngOnInit() {
  }

  logout() {
    this.keycloak.logout();
    this.backend.flushCache();
  }

}
