import { MessageService } from 'primeng/components/common/messageservice';
import { Router, NavigationStart } from '@angular/router';
import { environment } from './../environments/environment';
import { Injectable, EventEmitter } from '@angular/core';
import { Observable } from 'rxjs/Rx';
import { map } from 'rxjs/operators';
import { SlimLoadingBarService } from 'ng2-slim-loading-bar';

import { KeycloakService } from './auth/keycloak.service';
import { HttpClient, HttpHeaders } from '@angular/common/http';

@Injectable()
export class CustomHttp {
  constructor(private http: HttpClient, private keycloakService: KeycloakService,
    private slimLoadingBarService: SlimLoadingBarService, private router: Router, private messageService: MessageService) {

    router.events.subscribe(event => {
      if (event instanceof NavigationStart) {
        this.slimLoadingBarService.color = 'blue';
        this.messageService.clear();
      }
    });
  }

  get(url: string, options?): Observable<any> {
    this.showLoader();
    return this.transform(url, options).switchMap(x => {
      return this.http.get(x.url, x.options).map((res) => this.hideLoader(res))
        .catch((err) => this.errorLoader(err));
    });
  }

  post(url: string, body: any, options?): Observable<any> {
    this.showLoader();
    return this.transform(url, options).switchMap(x => {
      return this.http.post(x.url, body, x.options).map((res) => this.hideLoader(res))
        .catch((err) => this.errorLoader(err));
    });
  }

  put(url: string, body: any, options?): Observable<any> {
    this.showLoader();
    return this.transform(url, options).switchMap(x => {
      return this.http.put(x.url, body, x.options).map((res) => this.hideLoader(res))
        .catch((err) => this.errorLoader(err));
    });
  }

  delete(url: string, options?): Observable<any> {
    this.showLoader();
    return this.transform(url, options).switchMap(x => {
      return this.http.delete(x.url, x.options).map((res) => this.hideLoader(res))
        .catch((err) => this.errorLoader(err));
    });
  }

  patch(url: string, body: any, options?): Observable<any> {
    this.showLoader();
    return this.transform(url, options).switchMap(x => {
      return this.http.patch(x.url, body, x.options).map((res) => this.hideLoader(res))
        .catch((err) => this.errorLoader(err));
    });
  }

  head(url: string, options?): Observable<any> {
    this.showLoader();
    return this.transform(url, options).switchMap(x => {
      return this.http.head(x.url, x.options).map((res) => this.hideLoader(res))
        .catch((err) => this.errorLoader(err));
    });
  }

  private showLoader(): void {
    this.slimLoadingBarService.reset();
    this.slimLoadingBarService.start();
  }

  private hideLoader(response) {
    this.slimLoadingBarService.complete();
    return response;
  }

  private errorLoader(error: Observable<Response>): Observable<Response> {
    this.slimLoadingBarService.stop();
    this.slimLoadingBarService.color = 'red';
    this.slimLoadingBarService.progress = 100;
    return Observable.throw(error);
  }

  private transform(url: string, options?): Observable<any> {
    if (!url.startsWith(environment.backendPrefix))
      return Observable.of({ url: url, options: options });

    const obs = Observable.fromPromise(this.keycloakService.getToken()).map(token => {
      const requestUrl = environment.backendUrl + url.substr(environment.backendPrefix.length);
      const headers = {};
      if (token !== null) {
        Object.assign(headers, {Authorization: 'bearer ' + token});
      }
      let requestOptions = options;
      if (!options)
        requestOptions = { 'headers': new HttpHeaders(headers) };
      else if (!options.headers)
        requestOptions.headers = new HttpHeaders(headers);
      else
        Object.assign(requestOptions.headers, headers);
      return { url: requestUrl, options: requestOptions };
    });
    return obs;
  }

}
