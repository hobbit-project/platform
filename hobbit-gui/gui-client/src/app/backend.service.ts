import { BenchmarkComponent } from './upload/benchmark/benchmark.component';
import { plainToClass } from 'class-transformer';
import { environment } from './../environments/environment';
import { User, BenchmarkOverview, Benchmark, Challenge } from './model';
import { Injectable } from '@angular/core';
import { Http, Headers } from '@angular/http';
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
    return this.http.get(environment.backendPrefix + '/rest/benchmarks').map(res => plainToClass(BenchmarkOverview, res.json()));
  }

  getBenchmarkDetails(benchmarkId: string): Observable<Benchmark> {
    return this.http.get(environment.backendPrefix + `/rest/benchmarks/${encodeURIComponent(benchmarkId)}`).map(res => res.json());
  }

  submitBenchmark(model: any) {
    return this.http.post('BACKEND/rest/benchmarks', JSON.stringify(model), {
      headers: new Headers({ 'Content-Type': 'application/json' })
    }).map(res => res.json());
  }

  listChallenges(): Observable<Challenge[]> {
    return this.http.get('BACKEND/rest/challenges').map(res => plainToClass(Challenge, res.json()));
  }

  getChallenge(id: string) {
    return this.http.get(`BACKEND/rest/challenges/${encodeURIComponent(id)}`).map(res => res.json());
  }

  addChallenge(challenge: Challenge) {
    return this.http.post('BACKEND/rest/challenges', challenge).map(res => res.json());
  }

  updateChallenge(challenge: Challenge) {
    return this.http.put(`BACKEND/rest/challenges/${encodeURIComponent(challenge.id)}`, challenge).map(res => res.json());
  }


}
