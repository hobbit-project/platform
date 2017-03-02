import { Component, OnInit } from '@angular/core';
import { Router, NavigationExtras } from '@angular/router';

import { SelectOption, ConfigurationParameter, ConfigurationParameterValue, BenchmarkLight, System, Benchmark,
         Challenge, ChallengeTask, isChallengeOrganiser } from './model';
import { BackendService } from './services/backend.service';



@Component({
  selector: 'sg-challenges-list',
  template: require('./challenges-list.comp.html')
})
export class ChallengesListComponent implements OnInit {
  challenges: Challenge[];
  selectedChallenge: Challenge;
  loaded = false;
  editPermission = false;

  constructor(private bs: BackendService, private router: Router) {
  }

  ngOnInit() {
    this.bs.listChallenges().subscribe(data => {
      this.challenges = data;
      this.loaded = true;
    });
    this.bs.userInfo().subscribe(userInfo => {
      this.editPermission = isChallengeOrganiser(userInfo);
    });
  }

  addChallenge() {
    this.router.navigate(['/challenges', 'add-challenge']);
  }

  onSelect(event) {
    this.router.navigate(['/challenges', this.selectedChallenge.id]);
  }
}
