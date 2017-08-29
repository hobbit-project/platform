import { Component, OnChanges, Input } from '@angular/core';
import { Router, NavigationExtras } from '@angular/router';

import { ConfigurationParameterValue, NamedEntity, Experiment } from './model';


@Component({
  selector: 'sg-experiments',
  template: require('./experiments.comp.html')
})
export class ExperimentsComponent implements OnChanges {
  @Input() experiments: Experiment[];
  @Input() displayRanks: boolean;
  @Input() displayIDs: boolean;
  @Input() displayChallengeTasks: boolean;
  distinctTasks: NamedEntity[];
  selectedExperiments: Experiment[];

  constructor(private router: Router) {
  }

  ngOnChanges() {
    this.distinctTasks = [];
    let map = {};
    for (let ex of this.experiments) {
      if (ex.challengeTask && ex.challengeTask.name && !map[ex.challengeTask.id]) {
        this.distinctTasks.push(ex.challengeTask);
        map[ex.challengeTask.id] = true;
      }
    }
  }

  showDetails() {
    let ids = this.selectedExperiments.map(ex => ex.id).join(',');
    this.router.navigate(['experiments/details'], { queryParams: {'id': ids} });
  }

  showDetailsForChallengeTask(task) {
    this.router.navigate(['experiments/details'], { queryParams: {'challenge-task-id': task.id} });
  }
}
