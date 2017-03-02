import { Injectable } from '@angular/core';
import { Http, Headers, URLSearchParams, Response } from '@angular/http';
import { Observable } from 'rxjs/Rx';

import { UserInfo, Challenge, ChallengeRegistration, Experiment } from '../model';

@Injectable()
export class BackendService {
  private jsonHeaders = new Headers({ 'Content-Type': 'application/json' });
  private BACKEND = webpack.BACKEND_URL;
  private obsUserInfo: Observable<UserInfo>;

  constructor(private http: Http) {
  }


  listBenchmarks() {
    return this.http.get('BACKEND/rest/benchmarks').map(res => res.json());
  }

  getBenchmarkDetails(benchmarkId: string) {
    return this.http.get(`BACKEND/rest/benchmarks/${encodeURIComponent(benchmarkId)}`).map(res => res.json());
  }

  getStatus() {
    return this.http.get('BACKEND/rest/status').map(res => res.text());
  }

  submitBenchmark(model: any) {
    return this.http.post('BACKEND/rest/benchmarks', model).map(res => res.json());
  }

  getSubmissionDetails(id: string) {
    return this.http.get(`BACKEND/rest/submissions/${encodeURIComponent(id)}`).map(res => res.text());
  }

  userInfo(): Observable<UserInfo> {
    // cached request
    if (!this.obsUserInfo) {
      this.obsUserInfo = this.http.get('BACKEND/rest/internal/user-info').map(res => res.json()).publishReplay(1).refCount();
    }
    return this.obsUserInfo;
  }

  listChallenges() {
    return this.http.get('BACKEND/rest/challenges').map(res => res.json());
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

  deleteChallenge(id: string) {
    return this.http.delete(`BACKEND/rest/challenges/${encodeURIComponent(id)}`).map(res => res.json());
  }

  closeChallenge(id: string) {
    return this.http.put(`BACKEND/rest/challenges/operation/close/${encodeURIComponent(id)}`, {}).map(res => res.json());
  }

  getSystemProviderSystems() {
    return this.http.get('BACKEND/rest/system-provider/systems').map(res => res.json());
  }

  getChallengeRegistrations(challengeId: string): Observable<ChallengeRegistration[]> {
    return this.http.get(`BACKEND/rest/system-provider/challenge-registrations/${encodeURIComponent(challengeId)}`).map(res => res.json());
  }

  getAllChallengeRegistrations(challengeId: string): Observable<ChallengeRegistration[]> {
    return this.http.get(`BACKEND/rest/system-provider/challenge-all-registrations/${encodeURIComponent(challengeId)}`).map(res => res.json());
  }

  updateChallengeTaskRegistrations(challengeId: string, taskId: string, registrations: ChallengeRegistration[]) {
    return this.http.put(`BACKEND/rest/system-provider/challenge-registrations/${encodeURIComponent(challengeId)}/${encodeURIComponent(taskId)}`, registrations);
  }

  queryExperiments(ids?: string, challengeTaskId?: string): Observable<Experiment[]> {
    let params = new URLSearchParams();
    if (ids) {
      params.append('id', ids);
    }
    if (challengeTaskId) {
        params.append('challenge-task-id', challengeTaskId);
    }
    return this.http.get('BACKEND/rest/experiments/query', {search: params}).map(res => res.json());
  }

  countExperiments(challengeId: string) {
    return this.http.get(`BACKEND/rest/experiments/count-by-challenge/${encodeURIComponent(challengeId)}`).map(res => res.json());
  }

  getBackendUrl() {
    return this.BACKEND;
  }
}
