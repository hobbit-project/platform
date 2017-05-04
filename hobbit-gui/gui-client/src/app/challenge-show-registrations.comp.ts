import { Component, OnInit } from '@angular/core';
import { Router, NavigationExtras, ActivatedRoute } from '@angular/router';
import { ConfirmationService } from 'primeng/primeng';

import {
  SelectOption, ConfigurationParameter, ConfigurationParameterValue, BenchmarkLight, System, Benchmark,
  Challenge, ChallengeTask, ChallengeRegistration, isChallengeOrganiser, isSystemProvider
} from './model';
import { BackendService } from './services/backend.service';



@Component({
  selector: 'sg-challenge-show-registrations',
  template: require('./challenge-show-registrations.comp.html')
})
export class ChallengeShowRegistrationsComponent implements OnInit {
  challenge: Challenge;
  registrations: ChallengeRegistration[];
  loaded = false;
  loadedChallenge = false;
  loadedRegistrations = false;
  error: string;
  registeredSystems = {};

  constructor(private bs: BackendService, private activatedRoute: ActivatedRoute, private router: Router, private confirmationService: ConfirmationService) {
  }

  ngOnInit() {
    let id = this.activatedRoute.snapshot.params['id'];
    this.bs.getChallenge(id).subscribe(data => {
      this.challenge = data;
      this.loadedChallenge = true;
      this.updateLoaded();
    });
    this.bs.getAllChallengeRegistrations(id).subscribe(data => {
      this.registrations = data;
      this.loadedRegistrations = true;
      this.updateRegisteredSystems();
      this.updateLoaded();
    });
  }

  private updateLoaded() {
    this.loaded = this.loadedChallenge && this.loadedRegistrations;
  }

  private updateRegisteredSystems() {
    for (let reg of this.registrations) {
      if (!this.registeredSystems[reg.taskId]) {
        this.registeredSystems[reg.taskId] = [];
      }
      this.registeredSystems[reg.taskId].push(reg);
    }
  }

  cancel() {
    this.router.navigate(['/challenges', this.challenge.id]);
  }
}
