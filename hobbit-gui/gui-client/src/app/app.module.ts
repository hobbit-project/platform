import { CustomHttp } from './custom-http.service';
import { BackendService } from './backend.service';
import { KeycloakService } from './auth/keycloak.service';
import { AuthGuardService } from './auth/auth-guard.service';
import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { Http, HttpModule, XHRBackend, RequestOptions } from '@angular/http';

import { AppComponent } from './app.component';
import { NavbarComponent } from './navbar/navbar.component';
import { HomeComponent } from './home/home.component';
import { NotFoundComponent } from './not-found/not-found.component';
import { BenchmarkComponent } from './upload/benchmark/benchmark.component';
import { PageHeaderComponent } from './page-header/page-header.component';
import { SystemComponent } from './upload/system/system.component';


const appRoutes: Routes = [
  { path: '', redirectTo: 'home', pathMatch: 'full' },
  { path: 'home', component: HomeComponent, canActivate: [AuthGuardService] },
  { path: 'upload/benchmarks', component: BenchmarkComponent, canActivate: [AuthGuardService] },
  { path: 'upload/systems', component: SystemComponent, canActivate: [AuthGuardService] },
  { path: '**', component: NotFoundComponent }
];


export const httpProvide = {
  provide: Http,
  useFactory: httpClientFactory,
  deps: [XHRBackend, RequestOptions, KeycloakService]
};
export function httpClientFactory(backend: XHRBackend, defaultOptions: RequestOptions, keycloakService: KeycloakService): Http {
  return new CustomHttp(backend, defaultOptions, keycloakService);
}


@NgModule({
  declarations: [
    AppComponent,
    NavbarComponent,
    HomeComponent,
    NotFoundComponent,
    BenchmarkComponent,
    PageHeaderComponent,
    SystemComponent
  ],
  imports: [
    RouterModule.forRoot(appRoutes),
    BrowserModule,
    HttpModule
  ],
  providers: [
    AuthGuardService,
    KeycloakService,
    BackendService,
    httpProvide
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
