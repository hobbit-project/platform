import { Router } from '@angular/router';
import { BackendService } from './../../backend.service';
import { ConfigParamRealisation, Experiment, NamedEntity } from './../../model';
import { Component, OnInit, Input, OnChanges } from '@angular/core';
import Chart from 'chart.js';
import randomColor from 'randomcolor';

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

      if (this.experiments.length !== 0) {
        this.buildTableRows();
        setTimeout(() => this.renderChart(), 1000);
      }
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

  private buildRow(group: string, name: string, description: string, selector: (ex: Experiment) => [string, string]): TableRow {
    return this.buildRowKpi(group, new ConfigParamRealisation('', name, 'xsd.string', '', description), selector);
  }

  private buildRowKpi(group: string, kpi: ConfigParamRealisation, selector: (ex: Experiment) => [string, string]): TableRow {
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

  download(path: string) {
    this.bs.getLogFile(path).subscribe(log => {
      const link = document.createElement('a');
      link.download = 'log.txt';
      const blob = new Blob([log.text()], { type: 'text/plain' });
      link.href = window.URL.createObjectURL(blob);
      link.click();
    });
  }

  private renderChart() {
    const chartCanvas = <HTMLCanvasElement>document.getElementById('resultsChart');
    const ctx = chartCanvas.getContext('2d');
    // takes diagrams for first experiment
    const diagrams: any = this.experiments[0].diagrams;
    // takes first diagram from data
    const firstDiagram = diagrams[0];
    // generate new nice color for diagram
    const color = randomColor();
    const data: any = {
      // build x axis using label
      labels: firstDiagram.data.map(d => d.x),
      // convert diagrams to correct datasets format
      datasets: [{
        label: firstDiagram.label + '',
        data: firstDiagram.data.map(d => d.y),
        borderColor: color,
        backgroundColor: color,
      }],
    };
    const myChart = new Chart(ctx, {
      type: 'line',
      data,
      options: {
        responsive: false,
      }
    });
  }

}
