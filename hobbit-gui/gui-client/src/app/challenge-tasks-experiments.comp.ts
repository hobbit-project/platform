import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute, NavigationExtras } from '@angular/router';

import { Challenge, NamedEntity, ExperimentCount } from './model';
import { BackendService } from './services/backend.service';


@Component({
  selector: 'sg-challenge-tasks-experiments',
  template: require('./challenge-tasks-experiments.comp.html')
})
export class ChallengeTasksExperimentsComponent implements OnInit {
  challenge: Challenge;
  counts: ExperimentCount[];
  loaded = false;
  selectedCount: ExperimentCount;

  constructor(private bs: BackendService, private activatedRoute: ActivatedRoute, private router: Router) {
  }

  ngOnInit() {
    let id = this.activatedRoute.snapshot.params['id'];
    let taskId = this.activatedRoute.snapshot.queryParams['task-id'];
    this.bs.getChallenge(id).subscribe(data => {
      this.challenge = data;
    });

    this.bs.countExperiments(id).subscribe(data => {
      this.counts = data;
      if (taskId) {
        this.selectedCount = this.counts.find(value => value.challengeTask.id === taskId);
      }
      this.loaded = true;
    });
  }

  onSelect(event) {
    let qparams = {'task-id': this.selectedCount.challengeTask.id};
    this.router.navigate(['challenges', this.challenge.id, 'experiments'], {queryParams: qparams});
  }
}
