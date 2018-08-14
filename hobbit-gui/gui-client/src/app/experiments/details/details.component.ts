import { Router } from '@angular/router';
import { BackendService } from '../../backend.service';
import { ConfigParamRealisation, Experiment, NamedEntity } from '../../model';
import { Component, OnInit, Input, OnChanges } from '@angular/core';

class TableRow {
  constructor(public group: string, public kpiSample: ConfigParamRealisation,
    public values: Map<String, String>, public descriptions: Map<String, String>) { }
}

@Component({
  selector: 'app-experiments-details',
  templateUrl: './details.component.html',
  styleUrls: ['./details.component.less']
})
export class DetailsComponent implements OnInit, OnChanges {

  @Input()
  idsCommaSeparated: string;

  @Input()
  challengeTaskId: string;

  loaded: Boolean;
  experiments: Experiment[];
  rows: TableRow[];

  constructor(private bs: BackendService, private router: Router) { }

  ngOnInit() {
  }

  ngOnChanges() {
    this.rows = null;
    this.loaded = false;

    this.bs.queryExperiments(this.idsCommaSeparated, this.challengeTaskId).subscribe(data => {
      this.experiments = data;

      if (this.experiments == null)
        this.router.navigateByUrl('404', { skipLocationChange: true });

      if (this.experiments.length !== 0)
        this.buildTableRows();
    });
    this.loaded = true;
  }

  private buildTableRows() {
    const kpiSamples = {};
    const experimentParameterSamples = {};

    for (const ex of this.experiments) {
      for (const bp of ex.benchmark.configurationParamValues) {
        if (!experimentParameterSamples[bp.id])
          experimentParameterSamples[bp.id] = bp;
      }
      for (const kpi of ex.kpis) {
        if (!kpiSamples[kpi.id])
          kpiSamples[kpi.id] = kpi;
      }
    }

    this.rows = [];

    this.rows.push(this.buildRow('Experiment', 'Benchmark', 'The benchmark performed', t => DetailsComponent.safeNameAndDescription(t.benchmark)));
    this.rows.push(this.buildRow('Experiment', 'System', 'The system evaluated', t => DetailsComponent.safeNameAndDescription(t.system)));
    this.rows.push(this.buildRow('Experiment', 'Challenge Task', 'The challenge task performed', t => DetailsComponent.safeNameAndDescription(t.challengeTask)));
    this.rows.push(this.buildRow('Experiment', 'Error', 'The error message, if an error occured', t => [t.error, '']));

    for (const key of Object.keys(experimentParameterSamples)) {
      const bp = experimentParameterSamples[key];
      const row = this.buildRowKpi('Parameter', bp, ex => {
        const exbp = ex.benchmark.configurationParamValues.find(k => k.id === bp.id);
        return DetailsComponent.safeValueAndDescription(exbp);
      });
      this.rows.push(row);
    }

    for (const key of Object.keys(kpiSamples)) {
      const kpi = kpiSamples[key];
      const row = this.buildRowKpi('KPIs', kpi, ex => {
        const exkpi = ex.kpis.find(k => k.id === kpi.id);
        return DetailsComponent.safeValueAndDescription(exkpi);
      });
      this.rows.push(row);
    }


    const diagrams = {};
    for (const ex of this.experiments) {
      for (const diag of ex.diagrams) {
        diagrams[diag.name] = diag.description;
      }
    }
    for (let i = 0; i < Object.keys(diagrams).length; i++) {
      const name = Object.keys(diagrams)[i];
      console.log(name);
      const row = this.buildRow('Plots', name, diagrams[name], e => {
        const res = e.diagrams.find(d => d.name === name);
        return [res, name];
      });
      this.rows.push(row);
    }

    this.rows.push(this.buildRow('Logs', 'Benchmark Log', '', t => [
      t.benchmarkLogAvailable ? 'benchmark/query?id=' + t.id : null, 'Download'
    ]));
    this.rows.push(this.buildRow('Logs', 'System Log', '', t => [
      t.systemLogAvailable ? 'system/query?id=' + t.id : null, 'Download'
    ]));

    this.rows.sort((a, b) => {
      if (a.group !== b.group)
        return a.group.localeCompare(b.group);
      return a.kpiSample.name.localeCompare(b.kpiSample.name);
    });
  }

  private buildRow(group: string, name: string, description: string, selector: (ex: Experiment) => [any, string]): TableRow {
    return this.buildRowKpi(group, new ConfigParamRealisation('', name, 'xsd.string', '', description), selector);
  }

  private buildRowKpi(group: string, kpi: ConfigParamRealisation, selector: (ex: Experiment) => [any, string]): TableRow {
    const values = new Map<String, String>();
    const descriptions = new Map<String, String>();
    for (let i = 0; i < this.experiments.length; i++) {
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

  static safeValueAndDescription(pv: ConfigParamRealisation): [string, string] {
    return [
      (pv && pv.value) ? pv.value : '',
      (pv && pv.description) ? pv.description : ((pv && pv.name) ? pv.name : '')
    ];
  }

  download(path: string, format: string) {
    this.bs.getLogFile(path, format).subscribe(log => {
      const link = document.createElement('a');
      link.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(log));
      link.setAttribute('download', `log.${format.toLowerCase()}`);
      link.style.display = 'none';
      document.body.appendChild(link);

      link.click();
      document.body.removeChild(link);
    });
  }

  private getMimeType(format: string): string {
    if (format === 'JSON')
      return 'application/json';
    if (format === 'CSV')
      return 'text/comma-separated-values';
    return 'text/plain';
  }

}
