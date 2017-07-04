import { Component, OnInit } from '@angular/core';
import { Router, NavigationExtras } from '@angular/router';

import { ConfigurationParameterValue, NamedEntity, Experiment } from './model';
import { BackendService } from './services/backend.service';


@Component({
  selector: 'sg-experiments',
  template: require('./experiments.comp.html')
})
export class ExperimentsComponent implements OnInit {
  experiments: Experiment[];
  distinctTasks: NamedEntity[];
  selectedExperiments: Experiment[];
  loaded: boolean = false;

  constructor(private bs: BackendService, private router: Router) {
  }

  ngOnInit() {
    this.bs.queryExperiments().subscribe(data => {
      this.experiments = data;
      this.distinctTasks = [];
      let map = {};
      for (let ex of this.experiments) {
        if (ex.challengeTask && ex.challengeTask.name && !map[ex.challengeTask.id]) {
          this.distinctTasks.push(ex.challengeTask);
          map[ex.challengeTask.id] = true;
        }
      }
      this.loaded = true;
    });
  }

  showDetails() {
    let ids = this.selectedExperiments.map(ex => ex.id).join(',');
    this.router.navigate(['experiments/details'], { queryParams: {'id': ids} });
  }

  showDetailsForChallengeTask(task) {
    this.router.navigate(['experiments/details'], { queryParams: {'challenge-task-id': task.id} });
  }
}
