import { HttpHeaders, HttpParams } from '@angular/common/http';
import { CustomHttp } from './custom-http.service';
import { BenchmarkComponent } from './upload/benchmark/benchmark.component';
import { plainToClass } from 'class-transformer';
import { environment } from './../environments/environment';
import { User, BenchmarkOverview, Benchmark, Challenge, ExperimentCount, Experiment, ChallengeRegistration, ExtendedChallengeRegistration, StatusBean } from './model';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Rx';
import { map, publishReplay } from 'rxjs/operators';

@Injectable()
export class BackendService {

  private obsUserInfo: Observable<User>;

  constructor(private http: CustomHttp) { }

  userInfo(): Observable<User> {
    if (!this.obsUserInfo)
      this.obsUserInfo = this.http.get(environment.backendPrefix + '/rest/internal/user-info').map(res => User.fromJson(res)).publishReplay(1).refCount();
    return this.obsUserInfo;
  }

  flushCache() {
    this.obsUserInfo = null;
  }

  getStatus(): Observable<StatusBean> {
    return this.http.get('BACKEND/rest/status').map(res => plainToClass<StatusBean, Object>(StatusBean, res));
  }

  listBenchmarks(): Observable<BenchmarkOverview[]> {
    return this.http.get(environment.backendPrefix + '/rest/benchmarks').map(res => plainToClass(BenchmarkOverview, res));
  }

  getBenchmarkDetails(benchmarkId: string): Observable<Benchmark> {
    return this.http.get(environment.backendPrefix + `/rest/benchmarks/${encodeURIComponent(benchmarkId)}`).map(res => plainToClass<Benchmark, Object>(Benchmark, res));
  }

  submitBenchmark(model: any) {
    return this.http.post('BACKEND/rest/benchmarks', JSON.stringify(model), {
      headers: new HttpHeaders({ 'Content-Type': 'application/json' })
    }).map(res => res);
  }

  listChallenges(): Observable<Challenge[]> {
    return this.http.get('BACKEND/rest/challenges').map(res => plainToClass(Challenge, res));
  }

  getChallenge(id: string): Observable<Challenge> {
    return this.http.get(`BACKEND/rest/challenges/${encodeURIComponent(id)}`).map(res => plainToClass<Challenge, Object>(Challenge, res));
  }

  addChallenge(challenge: Challenge) {
    return this.http.post('BACKEND/rest/challenges', challenge).map(res => res);
  }

  updateChallenge(challenge: Challenge) {
    return this.http.put(`BACKEND/rest/challenges/${encodeURIComponent(challenge.id)}`, challenge).map(res => res);
  }

  closeChallenge(id: string) {
    return this.http.put(`BACKEND/rest/challenges/operation/close/${encodeURIComponent(id)}`, {}).map(res => res);
  }

  deleteChallenge(id: string) {
    return this.http.delete(`BACKEND/rest/challenges/${encodeURIComponent(id)}`).map(res => res);
  }

  countExperiments(challengeId: string): Observable<ExperimentCount[]> {
    return this.http.get(`BACKEND/rest/experiments/count-by-challenge/${encodeURIComponent(challengeId)}`).map(res => plainToClass(ExperimentCount, res));
  }

  queryExperiments(ids?: string, challengeTaskId?: string): Observable<Experiment[]> {
    let params = new HttpParams();
    if (ids)
      params = params.set('id', ids);
    if (challengeTaskId)
      params = params.set('challenge-task-id', challengeTaskId);
    return this.http.get('BACKEND/rest/experiments/query', { params: params }).map(res => plainToClass(Experiment, res));
  }

  terminateExperiment(id: string): Observable<any> {
    return this.http.get(`BACKEND/rest/experiments/terminate/${encodeURIComponent(id)}`);
  }

  getAllChallengeRegistrations(challengeId: string): Observable<ChallengeRegistration[]> {
    return this.http.get(`BACKEND/rest/system-provider/challenge-all-registrations/${encodeURIComponent(challengeId)}`).map(res => plainToClass(ChallengeRegistration, res));
  }

  getChallengeRegistrations(challengeId: string): Observable<ExtendedChallengeRegistration[]> {
    return this.http.get(`BACKEND/rest/system-provider/challenge-registrations/${encodeURIComponent(challengeId)}`).map(res => plainToClass(ExtendedChallengeRegistration, res));
  }

  updateChallengeTaskRegistrations(challengeId: string, taskId: string, registrations: ChallengeRegistration[]) {
    return this.http.put(`BACKEND/rest/system-provider/challenge-registrations/${encodeURIComponent(challengeId)}/${encodeURIComponent(taskId)}`, registrations);
  }

  getSystemProviderSystems() {
    return this.http.get('BACKEND/rest/system-provider/systems').map(res => res);
  }

  getLogFile(url: string, format: string) {
    return this.http.get('BACKEND/rest/logs/' + url + '&format=' + format, { responseType: 'text' });
  }

}
