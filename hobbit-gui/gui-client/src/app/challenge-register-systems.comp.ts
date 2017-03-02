import { Component, OnInit } from '@angular/core';
import { Router, NavigationExtras, ActivatedRoute } from '@angular/router';
import { ConfirmationService } from 'primeng/primeng';

import {
  SelectOption, ConfigurationParameter, ConfigurationParameterValue, BenchmarkLight, System, Benchmark,
  Challenge, ChallengeTask, ChallengeRegistration, isChallengeOrganiser, isSystemProvider
} from './model';
import { BackendService } from './services/backend.service';



@Component({
  selector: 'sg-challenge-register-systems',
  template: require('./challenge-register-systems.comp.html')
})
export class ChallengeRegisterSystemsComponent implements OnInit {
  challenge: Challenge;
  systems: System[];
  registrations: ChallengeRegistration[];
  loaded = false;
  loadedSystems = false;
  loadedChallenge = false;
  loadedRegistrations = false;
  error: string;
  selectedSystems = {};

  constructor(private bs: BackendService, private activatedRoute: ActivatedRoute, private router: Router, private confirmationService: ConfirmationService) {
  }

  ngOnInit() {
    let id = this.activatedRoute.snapshot.params['id'];
    this.bs.getChallenge(id).subscribe(data => {
      this.challenge = data;
      this.loadedChallenge = true;
      this.updateLoaded();
    });
    this.bs.getSystemProviderSystems().subscribe(data => {
      this.systems = data;
      this.loadedSystems = true;
      this.updateSelectedSystems();
      this.updateLoaded();
    });
    this.bs.getChallengeRegistrations(id).subscribe(data => {
      this.registrations = data;
      this.loadedRegistrations = true;
      this.updateSelectedSystems();
      this.updateLoaded();
    });
  }

  private updateLoaded() {
    this.loaded = this.loadedChallenge && this.loadedSystems && this.loadedRegistrations;
  }

  private updateSelectedSystems() {
    if (this.loadedRegistrations && this.loadedSystems) {
      let map = {};
      for (let system of this.systems) {
        map[system.id] = system;
      }
      for (let reg of this.registrations) {
        if (!this.selectedSystems[reg.taskId]) {
          this.selectedSystems[reg.taskId] = [];
        }
        let system = map[reg.systemId];
        if (system) {
          this.selectedSystems[reg.taskId].push(system);
        }
      }
    }
  }

  onSelectionChange(taskId: string, selection) {
    let taskRegistrations: ChallengeRegistration[] = [];
    for (let sel of selection) {
      taskRegistrations.push(new ChallengeRegistration(this.challenge.id, taskId, sel.id));
    }
    this.bs.updateChallengeTaskRegistrations(this.challenge.id, taskId, taskRegistrations).subscribe(ok => { this.error = ''; },
      error => { this.error = error.toString(); });
  }

  cancel() {
    this.router.navigate(['/challenges', this.challenge.id]);
  }
}
