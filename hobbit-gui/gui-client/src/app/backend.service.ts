import { environment } from './../environments/environment';
import { User, BenchmarkOverview, Benchmark } from './model';
import { Injectable } from '@angular/core';
import { Http } from '@angular/http';
import { Observable } from 'rxjs/Rx';

import 'rxjs/add/operator/map';
import 'rxjs/add/operator/publishReplay';

@Injectable()
export class BackendService {

  private obsUserInfo: Observable<User>;

  constructor(private http: Http) { }

  userInfo(): Observable<User> {
    if (!this.obsUserInfo)
      this.obsUserInfo = this.http.get(environment.backendPrefix + '/rest/internal/user-info').map(res => User.fromJson(res.json())).publishReplay(1).refCount();
    return this.obsUserInfo;
  }

  flushCache() {
    this.obsUserInfo = null;
  }

  listBenchmarks(): Observable<BenchmarkOverview[]> {
    return this.http.get(environment.backendPrefix + '/rest/benchmarks').map(res => res.json());
  }

  getBenchmarkDetails(benchmarkId: string): Observable<Benchmark> {
    return this.http.get(environment.backendPrefix + `/rest/benchmarks/${encodeURIComponent(benchmarkId)}`).map(res => res.json());
  }

}
