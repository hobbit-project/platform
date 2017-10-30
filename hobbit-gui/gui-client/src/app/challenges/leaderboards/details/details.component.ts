import { Router } from '@angular/router';
import { Experiment, ChallengeTask } from './../../../model';
import { BackendService } from './../../../backend.service';
import { Component, OnChanges, Input } from '@angular/core';

@Component({
  selector: 'app-leaderboard-details',
  templateUrl: './details.component.html',
  styleUrls: ['./details.component.less']
})
export class LeaderboardDetailsComponent implements OnChanges {

  @Input()
  public challengeTaskId: string;
  @Input()
  public rankingKPIs: string[];

  private experiments: Experiment[];
  public selectedExperiments: Experiment[];

  public loaded = false;

  constructor(private bs: BackendService, private router: Router) { }

  ngOnChanges() {
    this.loaded = false;

    this.bs.queryExperiments(undefined, this.challengeTaskId).subscribe(data => {

      this.experiments = [];
      const systems = {};

      const KPIVal = (experiment, id): number => {
        const kpi = experiment.kpis.find(k => k.id === id);
        if (kpi) {
          let value = parseFloat(kpi.value);
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

      this.loaded = true;
    });
  }

  showDetails() {
    const ids = this.selectedExperiments.map(ex => ex.id).join(',');
    this.router.navigate(['experiments/', ids]);
  }

}
