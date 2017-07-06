import { Component, OnChanges, Input } from '@angular/core';

import { ConfigurationParameterValue, NamedEntity, Experiment } from './model';
import { BackendService } from './services/backend.service';

class TableRow {
  constructor(public group: string, public kpiSample: ConfigurationParameterValue, 
              public values: Map<String, String>, public descriptions: Map<String, String>) {}
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
  rowGroups: string[];
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
    let mapKpis = {};
    let mapExpr = {};
    let kpiSamples: ConfigurationParameterValue[] = [];
    let experimentParameterSamples: ConfigurationParameterValue[] = [];
    for (let ex of this.experiments) {
      for (let bp of ex.benchmark.configurationParamValues) {
        if (!mapExpr[bp.id]) {
          experimentParameterSamples.push(bp);
          mapExpr[bp.id] = true;
        }
      }
      for (let kpi of ex.kpis) {
        if (!mapKpis[kpi.id]) {
          kpiSamples.push(kpi);
          mapKpis[kpi.id] = true;
        }
      }
    }

    this.rowGroups = ['Experiment', 'Experiment Parameter', 'KPIs'];
    this.rows = [];
    let benchmarkRow = this.buildRow('Experiment', 'Benchmark', 'The benchmark performed', t => ExperimentsDetailsComponent.safeNameAndDescription(t.benchmark));
    let systemRow = this.buildRow('Experiment', 'System', 'The system evaluated' , t => ExperimentsDetailsComponent.safeNameAndDescription(t.system));
    let challengeTaskRow = this.buildRow('Experiment', 'Challenge Task', 'The challenge task performed', t => ExperimentsDetailsComponent.safeNameAndDescription(t.challengeTask));
    let errorRow = this.buildRow('Experiment', 'Error', 'The error message, if an error occured', t => [t.error, '']);
    this.rows.push(benchmarkRow);
    this.rows.push(systemRow);
    this.rows.push(challengeTaskRow);
    this.rows.push(errorRow);
    for (let bp of experimentParameterSamples) {
      let row = this.buildRowKpi('Experiment Parameter', bp, ex => {
        let exbp = ex.benchmark.configurationParamValues.find(k => k.id === bp.id);
        return ExperimentsDetailsComponent.safeValueAndDescription(exbp);
      });
      this.rows.push(row);
    }
    for (let kpi of kpiSamples) {
      let row = this.buildRowKpi('KPIs', kpi, ex => {
        let exkpi = ex.kpis.find(k => k.id === kpi.id);
        return ExperimentsDetailsComponent.safeValueAndDescription(exkpi);
      });
      this.rows.push(row);
    }
    this.rows.sort( (a,b) => {
      if (a.group < b.group) {
        return -1;
      } else if (a.group > b.group) {
        return 1;
      } else if (a.kpiSample.name < b.kpiSample.name) {
        return -1;
      } else if (a.kpiSample.name > b.kpiSample.name) {
        return 1;
      } else {
        return 0;
      }
    });
  }

  private buildRow(group: string, name: string, description: string, selector: (ex: Experiment) => [string, string]): TableRow {
    let kpi = new ConfigurationParameterValue('', name, 'xsd.string', '', description);
    return this.buildRowKpi(group, kpi, selector);
  }

  private buildRowKpi(group: string, kpi: ConfigurationParameterValue, selector: (ex: Experiment) => [string, string]): TableRow {
    let values = new Map<String, String>();
    let descriptions = new Map<String, String>();
    for (let i in this.experiments) {
      const [name, description] = selector(this.experiments[i]);
      values['' + i] = name;
      descriptions['' + i] = description;
    }
    return new TableRow(group, kpi, values, descriptions);
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
