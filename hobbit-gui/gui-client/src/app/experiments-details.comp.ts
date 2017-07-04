import { Component, OnChanges, Input } from '@angular/core';

import { ConfigurationParameterValue, NamedEntity, Experiment } from './model';
import { BackendService } from './services/backend.service';

class TableRow {
  constructor(public kpiSample: ConfigurationParameterValue, public values: Map<String, String>, public descriptions: Map<String, String>) {}
}

@Component({
  selector: 'sg-experiments-details',
  template: require('./experiments-details.comp.html')
})
export class ExperimentsDetailsComponent implements OnChanges {
  @Input() idsCommaSeparated: string;
  @Input() challengeTaskId: string;
  experiments: Experiment[];
  rows: TableRow[];
  loaded: boolean = false;

  constructor(private bs: BackendService) {
  }

  ngOnChanges() {
    //console.debug('idsCommaSeparated: ' + this.idsCommaSeparated);
    //console.debug('challengeTaskId  : ' + this.challengeTaskId);
    this.bs.queryExperiments(this.idsCommaSeparated, this.challengeTaskId).subscribe(data => {
      this.experiments = data;
      this.buildTableRows();
      this.loaded = true;
    });
  }

  private buildTableRows() {
    let map = {};
    let kpiSamples: ConfigurationParameterValue[] = [];
    for (let ex of this.experiments) {
      for (let kpi of ex.kpis) {
        if (!map[kpi.id]) {
          kpiSamples.push(kpi);
          map[kpi.id] = true;
        }
      }
    }

    this.rows = [];
    let benchmarkRow = this.buildRow('Benchmark', 'The benchmark performed', t => ExperimentsDetailsComponent.safeNameAndDescription(t.benchmark));
    let systemRow = this.buildRow('System', 'The system evaluated' , t => ExperimentsDetailsComponent.safeNameAndDescription(t.system));
    let challengeTaskRow = this.buildRow('Challenge Task', 'The challenge task performed', t => ExperimentsDetailsComponent.safeNameAndDescription(t.challengeTask));
    let errorRow = this.buildRow('Error', 'The error message, if an error occured', t => [t.error, '']);
    this.rows.push(benchmarkRow);
    this.rows.push(systemRow);
    this.rows.push(challengeTaskRow);
    this.rows.push(errorRow);
    for (let kpi of kpiSamples) {
      let row = this.buildRowKpi(kpi, ex => {
        let exkpi = ex.kpis.find(k => k.id === kpi.id);
        return ExperimentsDetailsComponent.safeValueAndDescription(exkpi);
      });
      this.rows.push(row);
    }
  }

  private buildRow(name: string, description: string, selector: (ex: Experiment) => [string, string]): TableRow {
    let kpi = new ConfigurationParameterValue('', name, 'xsd.string', '', description);
    return this.buildRowKpi(kpi, selector);
  }

  private buildRowKpi(kpi: ConfigurationParameterValue, selector: (ex: Experiment) => [string, string]): TableRow {
    let values = new Map<String, String>();
    let descriptions = new Map<String, String>();
    for (let i in this.experiments) {
      const [name, description] = selector(this.experiments[i]);
      values['' + i] = name;
      descriptions['' + i] = description;
    }
    return new TableRow(kpi, values, descriptions);
  }

  static safeNameAndDescription(entity: NamedEntity): [string, string] {
    return [
      (entity && entity.name) ? entity.name : '',
      (entity && entity.description) ? entity.description : ''
    ];
  }

  static safeValueAndDescription(pv: ConfigurationParameterValue): [string, string] {
    return [
      (pv && pv.value) ? pv.value : '',
      (pv && pv.description) ? pv.description : ((pv && pv.name) ? pv.name : '')
    ];
  }
}
