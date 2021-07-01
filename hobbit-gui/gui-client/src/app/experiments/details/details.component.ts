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
  benchmarkId: string;

  @Input()
  challengeTaskId: string;

  @Input()
  excludeErrors: boolean;

  @Input()
  distinctBySystem: boolean;

  @Input()
  limit: number;

  loaded: Boolean;
  experiments: Experiment[];
  details: Experiment[];
  rows: TableRow[];
  sameBenchmark: boolean;

  constructor(private bs: BackendService, private router: Router) { }

  ngOnInit() {
  }

  ngOnChanges() {
    this.sameBenchmark = null;
    this.rows = null;
    this.experiments = null;
    this.details = null;
    this.loaded = false;

    this.bs.queryExperiments(this.idsCommaSeparated, this.benchmarkId, this.challengeTaskId).subscribe(data => {
      this.experiments = data;

      if (this.experiments == null)
        this.router.navigateByUrl('404', { skipLocationChange: true });

      // FIXME: should be server-side
      if (this.excludeErrors) {
        this.experiments = this.experiments.filter(experiment => experiment.error === undefined);
      }

      this.details = this.experiments;

      if (this.distinctBySystem) {
        // sort experiments by date
        this.details = this.details.sort((a, b) => parseInt(b.id, 10) - parseInt(a.id, 10));
        // use only one experiment from each system
        const systemAmount = {};
        this.details = this.details.filter(experiment => {
          systemAmount[experiment.system.id] = (systemAmount[experiment.system.id] || 0) + 1;
          return systemAmount[experiment.system.id] === 1;
        });
      }

      // FIXME: should be server-side
      if (this.limit) {
        this.details = this.details.slice(0, this.limit);
      }

      if (this.details.length !== 0)
        this.buildTableRows(this.details);

      this.sameBenchmark = new Set(this.experiments.filter(ex => ex.benchmark).map(ex => ex.benchmark.id)).size === 1;
      this.loaded = true;
    });
  }

  private buildTableRows(experiments) {
    const kpiSamples = {};
    const experimentParameterSamples = {};

    for (const ex of experiments) {
      if (ex.benchmark) {
        for (const bp of ex.benchmark.configurationParamValues) {
          if (!experimentParameterSamples[bp.id])
            experimentParameterSamples[bp.id] = bp;
        }
      }
      if (ex.kpis) {
        for (const kpi of ex.kpis) {
          if (!kpiSamples[kpi.id])
            kpiSamples[kpi.id] = kpi;
        }
      }
    }

    this.rows = [];

    // FIXME server should send the real experiment URI
    this.rows.push(this.buildRow('Experiment', 'URI', 'Permanent URI of the experiment', t => ['http://w3id.org/hobbit/experiments#' + t.id, '']));

    this.rows.push(this.buildRow('Experiment', 'Benchmark', 'The benchmark performed', t => DetailsComponent.safeNameAndDescription(t.benchmark)));
    this.rows.push(this.buildRow('Experiment', 'System', 'The system evaluated', t => DetailsComponent.safeNameAndDescription(t.system)));
    this.rows.push(this.buildRow('Experiment', 'Challenge Task', 'The challenge task performed', t => DetailsComponent.safeNameAndDescription(t.challengeTask)));

    if (experiments.some(experiment => experiment.error !== undefined)) {
      this.rows.push(this.buildRow('Experiment', 'Error', 'The error message, if an error occured', t => [t.error, '']));
    }

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
    for (const ex of experiments) {
      if (ex.diagrams) {
        for (const diag of ex.diagrams) {
          diagrams[diag.name] = diag.description;
        }
      }
    }
    for (let i = 0; i < Object.keys(diagrams).length; i++) {
      const name = Object.keys(diagrams)[i];
      const row = this.buildRow('Plots', name, diagrams[name], e => {
        const res = e.diagrams && e.diagrams.find(d => d.name === name);
        return [res, name];
      });
      this.rows.push(row);
    }

    this.rows.push(this.buildRow('Logs', 'Benchmark Log', '', t => [
      t.benchmarkLogAvailable ? [`benchmark/query?id=${t.id}`, `${t.id} benchmark log`] : null, 'Download'
    ]));
    this.rows.push(this.buildRow('Logs', 'System Log', '', t => [
      t.systemLogAvailable ? [`system/query?id=${t.id}`, `${t.id} system log`] : null, 'Download'
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
      if (this.experiments[i].benchmark) {
        const [name, description] = selector(this.experiments[i]);
        values['' + i] = name;
        descriptions['' + i] = description;
      }
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

  download(path: [string], format: string) {
    this.bs.getLogFile(path[0], format).subscribe(log => {
      const fileName = `${path[1]}.${format.toLowerCase()}`;
      const link = document.createElement('a');
      link.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(log));
      link.setAttribute('download', fileName);
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

  isDotLanguage(value: string) {
    return value && value.match(/^digraph \{/m);
  }

}
