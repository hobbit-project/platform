import { User } from './model';
import { Injectable } from '@angular/core';
import { Http } from '@angular/http';
import { Observable } from 'rxjs/Rx';

import 'rxjs/add/operator/map';
import 'rxjs/add/operator/publishReplay';

@Injectable()
export class BackendService {

  private obsUserInfo: Observable<User>;

  constructor(private http: Http) { }

  userInfo() {
    if (!this.obsUserInfo)
      this.obsUserInfo = this.http.get('BACKEND/rest/internal/user-info').map(res => User.fromJson(res.json())).publishReplay(1).refCount();
    return this.obsUserInfo;
  }

  flushCache() {
    this.obsUserInfo = null;
  }

}
