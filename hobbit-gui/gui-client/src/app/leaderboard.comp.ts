import { Component, OnChanges, Input } from '@angular/core';

import { ConfigurationParameterValue, NamedEntity, Experiment } from './model';
import { BackendService } from './services/backend.service';


@Component({
  selector: 'sg-leaderboard',
  template: require('./leaderboard.comp.html')
})
export class LeaderboardComponent implements OnChanges {
  @Input() challengeTaskId: string;
  @Input() rankingKPIs: string[];
  experiments: Experiment[];
  loaded: boolean = false;

  constructor(private bs: BackendService) {
  }

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
}
