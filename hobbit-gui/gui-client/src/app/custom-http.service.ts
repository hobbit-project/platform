import { environment } from './../environments/environment';
import { Injectable, EventEmitter } from '@angular/core';
import { Http, RequestOptions, Headers, ConnectionBackend, RequestOptionsArgs, Request, Response } from '@angular/http';
import { Observable } from 'rxjs/Rx';
import { SlimLoadingBarService } from 'ng2-slim-loading-bar';

import { KeycloakService } from './auth/keycloak.service';

@Injectable()
export class CustomHttp extends Http {
  constructor(backend: ConnectionBackend, defaultOptions: RequestOptions, private keycloakService: KeycloakService,
    private slimLoadingBarService: SlimLoadingBarService) {
    super(backend, defaultOptions);
  }

  get(url: string, options?: RequestOptionsArgs): Observable<Response> {
    this.showLoader();
    return this.transform(url, options).switchMap(x => {
      return super.get(x.url, x.options).finally(() => this.hideLoader());
    });
  }

  post(url: string, body: string, options?: RequestOptionsArgs): Observable<Response> {
    this.showLoader();
    return this.transform(url, options).switchMap(x => {
      return super.post(x.url, body, x.options).finally(() => this.hideLoader());
    });
  }

  put(url: string, body: string, options?: RequestOptionsArgs): Observable<Response> {
    this.showLoader();
    return this.transform(url, options).switchMap(x => {
      return super.put(x.url, body, x.options).finally(() => this.hideLoader());
    });
  }

  delete(url: string, options?: RequestOptionsArgs): Observable<Response> {
    this.showLoader();
    return this.transform(url, options).switchMap(x => {
      return super.delete(x.url, x.options).finally(() => this.hideLoader());
    });
  }

  patch(url: string, body: string, options?: RequestOptionsArgs): Observable<Response> {
    this.showLoader();
    return this.transform(url, options).switchMap(x => {
      return super.patch(x.url, body, x.options).finally(() => this.hideLoader());
    });
  }

  head(url: string, options?: RequestOptionsArgs): Observable<Response> {
    this.showLoader();
    return this.transform(url, options).switchMap(x => {
      return super.head(x.url, x.options).finally(() => this.hideLoader());
    });
  }

  private showLoader(): void {
    console.log('Showing loader');
    this.slimLoadingBarService.start();
  }

  private hideLoader(): void {
    console.log('Showing loader');
    this.slimLoadingBarService.complete();
  }

  private transform(url: string, options?: RequestOptionsArgs): Observable<any> {
    if (!url.startsWith(environment.backendPrefix))
      return Observable.of({ url: url, options: options });

    const obs = Observable.fromPromise(this.keycloakService.getToken()).map(token => {
      const requestUrl = environment.backendUrl + url.substr(environment.backendPrefix.length);
      const headers = { 'Authorization': 'bearer ' + token };
      let requestOptions = options;
      if (!options)
        requestOptions = { 'headers': new Headers(headers) };
      else if (!options.headers)
        requestOptions.headers = new Headers(headers);
      else
        requestOptions.headers.set('Authorization', headers['Authorization']);
      return { url: requestUrl, options: requestOptions };
    });
    return obs;
  }

}