import { User, Role, LicenseBean } from './../model';
import { BackendService } from './../backend.service';
import { KeycloakService } from './../auth/keycloak.service';
import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-footer',
  templateUrl: './footer.component.html',
  styleUrls: ['./footer.component.css']
})
export class FooterComponent implements OnInit {
  public license: LicenseBean;

  constructor(private keycloak: KeycloakService, private backend: BackendService) {
    this.backend.getLicense().subscribe(data => {
      this.license = data;
    }, e => {
      console.log('Encountered error ' + e);
      return e;
    });
  }

  ngOnInit() {
  }
}
