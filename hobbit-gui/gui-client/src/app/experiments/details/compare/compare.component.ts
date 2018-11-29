import {MessageService} from 'primeng/components/common/messageservice';
import {ConfigParamDefinition, ConfigParamRealisation, Experiment} from '../../../model';
import {Router} from '@angular/router';
import {BackendService} from '../../../backend.service';
import {Component, ElementRef, Input, OnInit, ViewChild} from '@angular/core';
import * as Chart from 'chart.js';
import {scaleOrdinal} from 'd3-scale';
import {schemeCategory10} from 'd3-scale-chromatic';

@Component({
  selector: 'app-experiment-compare',
  templateUrl: './compare.component.html',
})
export class CompareComponent implements OnInit {

  @Input()
  experiments: Experiment[];

  @ViewChild('canvas')
  canvas: ElementRef;

  parameters: ConfigParamDefinition[];
  kpis: ConfigParamRealisation[];

  configModel: any = {};
  selectedParameter: ConfigParamDefinition;
  selectedKPI: ConfigParamRealisation;

  constructor(private bs: BackendService, private router: Router,
    private messageService: MessageService) {
  }

  ngOnInit() {
    this.parameters = [];
    this.experiments.forEach(experiment => {
      experiment.benchmark.configurationParams.forEach(parameter => {
        if (this.parameters.every(p => p.id !== parameter.id)) {
          this.parameters.push(parameter);
        }
      });
    });

    this.kpis = [];
    this.experiments.forEach(experiment => {
      experiment.kpis.forEach(kpi => {
        if (this.kpis.every(k => k.id !== kpi.id)) {
          this.kpis.push(kpi);
        }
      });
    });
  }

  drawChart() {
    if (this.selectedParameter && this.selectedKPI) {
      const chartCanvas = <HTMLCanvasElement>this.canvas.nativeElement;
      const ctx = chartCanvas.getContext('2d');

      const systems = Array.from(new Set(this.experiments.map(ex => ex.system.id)))
          .map(id => this.experiments.find(ex => ex.system.id === id).system)
          .sort((a, b) => a.name > b.name ? 1 : b.name < a.name ? -1 : 0);
      const colorScale = scaleOrdinal(schemeCategory10);

      const datasets = systems.map((system, index) => ({
        label: system.name,
        radius: 5,
        borderWidth: 2,
        pointStyle: 'crossRot',
        borderColor: colorScale(index),
        backgroundColor: colorScale(index),
        data: this.experiments.filter(ex => ex.system.id === system.id).map(experiment => ({
          x: (experiment.benchmark.configurationParamValues.find(p => p.id === this.selectedParameter.id) || {value: undefined}).value,
          y: (experiment.kpis.find(k => k.id === this.selectedKPI.id) || {value: undefined}).value,
        })),
      }));

      const xAxis = {
        scaleLabel: {
          display: true,
          labelString: this.selectedParameter.name,
        },
      };
      if ([
        'xsd:byte',
        'xsd:decimal',
        'xsd:double',
        'xsd:int',
        'xsd:integer',
        'xsd:long',
        'xsd:negativeInteger',
        'xsd:nonNegativeInteger',
        'xsd:nonPositiveInteger',
        'xsd:positiveInteger',
        'xsd:short',
        'xsd:unsignedByte',
        'xsd:unsignedInt',
        'xsd:unsignedLong',
        'xsd:unsignedShort',
      ].indexOf(this.selectedParameter.datatype) === -1) {
        Object.assign(xAxis, {
          labels: Array.from(new Set(this.experiments.map(experiment => (experiment.benchmark.configurationParamValues.find(p => p.id === this.selectedParameter.id) || {value: undefined}).value))),
          type: 'category',
          ticks: {
            autoSkip: false,
          },
        });
      }

      const yAxis = {
        scaleLabel: {
          display: true,
          labelString: this.selectedKPI.name,
        },
      };

      const chart = new Chart.Chart(ctx, {
        type: 'scatter',
        data: {datasets},
        options: {
          legend: {position: 'right'},
          responsive: true,
          scales: {xAxes: [xAxis], yAxes: [yAxis]},
          tooltips: {
            callbacks: {
              beforeLabel: item => systems[item.datasetIndex].name,
            },
          },
        }
      });
    }
  }

  onChangeParameter(event) {
    this.selectedParameter = this.parameters.find(p => p.id === event);
    this.drawChart();
  }

  onChangeKPI(event) {
    this.selectedKPI = this.kpis.find(k => k.id === event);
    this.drawChart();
  }
}
