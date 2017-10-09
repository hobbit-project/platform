import { MessageService } from 'primeng/components/common/messageservice';
import { Router, NavigationStart } from '@angular/router';
import { environment } from './../environments/environment';
import { Injectable, EventEmitter } from '@angular/core';
import { Http, RequestOptions, Headers, ConnectionBackend, RequestOptionsArgs, Request, Response } from '@angular/http';
import { Observable } from 'rxjs/Rx';
import { SlimLoadingBarService } from 'ng2-slim-loading-bar';

import { KeycloakService } from './auth/keycloak.service';

@Injectable()
export class CustomHttp extends Http {
  constructor(backend: ConnectionBackend, defaultOptions: RequestOptions, private keycloakService: KeycloakService,
    private slimLoadingBarService: SlimLoadingBarService, private router: Router, private messageService: MessageService) {
    super(backend, defaultOptions);

    router.events.subscribe(event => {
      if (event instanceof NavigationStart) {
        this.slimLoadingBarService.color = 'blue';
        this.messageService.clear();
      }
    });
  }

  get(url: string, options?: RequestOptionsArgs): Observable<Response> {
    this.showLoader();
    return this.transform(url, options).switchMap(x => {
      return super.get(x.url, x.options).map((res) => this.hideLoader(res))
        .catch((err) => this.errorLoader(err));
    });
  }

  post(url: string, body: string, options?: RequestOptionsArgs): Observable<Response> {
    this.showLoader();
    return this.transform(url, options).switchMap(x => {
      return super.post(x.url, body, x.options).map((res) => this.hideLoader(res))
        .catch((err) => this.errorLoader(err));
    });
  }

  put(url: string, body: string, options?: RequestOptionsArgs): Observable<Response> {
    this.showLoader();
    return this.transform(url, options).switchMap(x => {
      return super.put(x.url, body, x.options).map((res) => this.hideLoader(res))
        .catch((err) => this.errorLoader(err));
    });
  }

  delete(url: string, options?: RequestOptionsArgs): Observable<Response> {
    this.showLoader();
    return this.transform(url, options).switchMap(x => {
      return super.delete(x.url, x.options).map((res) => this.hideLoader(res))
        .catch((err) => this.errorLoader(err));
    });
  }

  patch(url: string, body: string, options?: RequestOptionsArgs): Observable<Response> {
    this.showLoader();
    return this.transform(url, options).switchMap(x => {
      return super.patch(x.url, body, x.options).map((res) => this.hideLoader(res))
        .catch((err) => this.errorLoader(err));
    });
  }

  head(url: string, options?: RequestOptionsArgs): Observable<Response> {
    this.showLoader();
    return this.transform(url, options).switchMap(x => {
      return super.head(x.url, x.options).map((res) => this.hideLoader(res))
        .catch((err) => this.errorLoader(err));
    });
  }

  private showLoader(): void {
    this.slimLoadingBarService.reset();
    this.slimLoadingBarService.start();
  }

  private hideLoader(response: Response): Response {
    this.slimLoadingBarService.complete();
    return response;
  }

  private errorLoader(error: Observable<Response>): Observable<Response> {
    this.slimLoadingBarService.stop();
    this.slimLoadingBarService.color = 'red';
    this.slimLoadingBarService.progress = 100;
    return Observable.throw(error);
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
