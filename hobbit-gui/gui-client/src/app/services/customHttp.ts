import { Injectable, EventEmitter } from '@angular/core';
import { Http, RequestOptions, Headers, ConnectionBackend, RequestOptionsArgs, Request, Response } from '@angular/http';
import { Observable } from 'rxjs/Rx';

import { KeycloakService } from './keycloakService';

@Injectable()
export class CustomHttp extends Http {
  constructor(backend: ConnectionBackend, defaultOptions: RequestOptions, private keycloakService: KeycloakService) {
    super(backend, defaultOptions);
  }

  get(url: string, options?: RequestOptionsArgs): Observable<Response> {
    let obs = this.transform(url, options);
    return obs.switchMap( x => {
      return super.get(x.turl, x.toptions);
    });
  }

  post(url: string, body: string, options?: RequestOptionsArgs): Observable<Response> {
    let obs = this.transform(url, options);
    return obs.switchMap( x => {
      return super.post(x.turl, body, x.toptions);
    });
  }

  put(url: string, body: string, options?: RequestOptionsArgs): Observable<Response> {
    let obs = this.transform(url, options);
    return obs.switchMap( x => {
      return super.put(x.turl, body, x.toptions);
    });
  }

  delete(url: string, options?: RequestOptionsArgs): Observable<Response> {
    let obs = this.transform(url, options);
    return obs.switchMap( x => {
      return super.delete(x.turl, x.toptions);
    });
  }

  patch(url: string, body: string, options?: RequestOptionsArgs): Observable<Response> {
    let obs = this.transform(url, options);
    return obs.switchMap( x => {
      return super.patch(x.turl, body, x.toptions);
    });
  }

  head(url: string, options?: RequestOptionsArgs): Observable<Response> {
    let obs = this.transform(url, options);
    return obs.switchMap( x => {
      return super.head(x.turl, x.toptions);
    });
  }

  transform(url: string, options?: RequestOptionsArgs) {
    if (url.startsWith('BACKEND/')) {
      const otoken = Observable.fromPromise(this.keycloakService.getToken());
      let obs = otoken.map(token => {
        const turl = webpack.BACKEND_URL + url.substr(7);
        const headers = { 'Authorization': 'bearer ' + token };
        let toptions = options;
        if (!options) {
          toptions = { 'headers': new Headers(headers) };
        } else if (!options.headers) {
          toptions.headers = new Headers(headers);
        } else {
          toptions.headers.set('Authorization', headers['Authorization']);
        }
        return { turl: turl, toptions: toptions };
      });
      return obs;
    } else {
      return Observable.of({ turl: url, toptions: options });
    }
  }

  getToken(): Observable<string> {
    return Observable.fromPromise(this.keycloakService.getToken());
  }

  static transformUrl(url: string): string {
    if (url.startsWith('BACKEND/')) {
      return webpack.BACKEND_URL + url.substr(7);
    } else {
      return url;
    }
  }
}
