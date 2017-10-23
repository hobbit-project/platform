import { ActivatedRoute, Router } from '@angular/router';
import { BackendService } from './../../backend.service';
import { Challenge, ChallengeTask, Experiment } from './../../model';
import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-leaderboard',
  templateUrl: './leaderboard.component.html'
})
export class LeaderboardComponent implements OnInit {

  challenge: Challenge;
  loaded = false;
  selectedTask: ChallengeTask;

  experiments: Experiment[];
  rankingKPIs: string[];
  loadedExperiment = false;

  constructor(private bs: BackendService, private activatedRoute: ActivatedRoute, private router: Router) {
  }

  ngOnInit() {
    const id = this.activatedRoute.snapshot.params['id'];
    const taskId = this.activatedRoute.snapshot.queryParams['task-id'];
    this.bs.getChallenge(id).subscribe(data => {
      this.challenge = data;
      if (taskId) {
        this.selectedTask = this.challenge.tasks.find(task => task.id === taskId);
      }
      this.loaded = true;
    });
  }

  onSelect(event) {
    this.loadedExperiment = false;
    this.bs.queryExperiments(undefined, this.selectedTask.id).subscribe(data => {
      this.experiments = [];
      const systems = {};

      const KPIVal = (experiment, id): number => {
        const kpi = experiment.kpis.find(k => k.id === id);
        if (kpi) {
          const value = parseFloat(kpi.value);
          if (!isNaN(value)) {
            switch (kpi.ranking) {
              case 'http://w3id.org/hobbit/vocab#AscendingOrder':
                return value;
              case 'http://w3id.org/hobbit/vocab#DescendingOrder':
                return -value;
            }
          }
        }
        return undefined;
      };
      const cmp = (a, b) => {
        if (a === undefined && b !== undefined) {
          return 1;
        }
        if (a !== undefined && b === undefined) {
          return -1;
        }
        return a > b ? 1 : b > a ? -1 : 0;
      };

      data.sort((a, b) => this.rankingKPIs.map(kpi => cmp(KPIVal(a, kpi), KPIVal(b, kpi))).find(cmp => cmp !== 0) || cmp(a.system.id, b.system.id));
      for (let i = 0; i < data.length; i++) {
        if (!systems[data[i].system.id]) {
          systems[data[i].system.id] = true;
          data[i].rank = this.experiments.length + 1;
          this.experiments.push(data[i]);
        }
      }
      this.loadedExperiment = true;
    });
  }
}

