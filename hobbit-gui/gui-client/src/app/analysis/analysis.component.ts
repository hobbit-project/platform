import {MessageService} from 'primeng/components/common/messageservice';
import {ConfigParamDefinition, ConfigParamRealisation, AnalysisResultset, Benchmark} from '../model';
import {Router} from '@angular/router';
import {BackendService} from '../backend.service';
import {Component, ElementRef, Input, OnChanges, OnInit, ViewChild} from '@angular/core';
import * as Chart from 'chart.js';
import {scaleOrdinal} from 'd3-scale';
import {schemeCategory10} from 'd3-scale-chromatic';

@Component({
  selector: 'app-analysis',
  templateUrl: './analysis.component.html',
})
export class AnalysisComponent implements OnChanges, OnInit {

  @Input()
  benchmark: Benchmark;

  @ViewChild('canvas')
  canvas: ElementRef;

  resultsets: AnalysisResultset[];
  kpis: ConfigParamRealisation[];
  selectedKPI: ConfigParamRealisation;
  configModel: any = {};

  chart: any;

  constructor(private bs: BackendService, private router: Router,
    private messageService: MessageService) {
  }

  ngOnInit() {
  }

  ngOnChanges() {
    this.bs.queryAnalysisResults(this.benchmark.id).subscribe(data => {
      this.resultsets = data;
      // filter out KPIs with only undefined values
      this.kpis = this.resultsets[0].benchmark.kpis.filter(kpi =>
        this.resultsets.filter(resultset => resultset.results.find(result => result.kpiUri === kpi.id)).length
      );
    });
  }

  onChangeKPI(event) {
    this.selectedKPI = this.kpis.find(k => k.id === event);
    this.drawChart();
  }

  resultMatcher(parameter, result) {
    return result.kpiUri === this.selectedKPI.id && result.parameterUri === parameter.id;
  }

  drawChart() {
    const chartCanvas = <HTMLCanvasElement>this.canvas.nativeElement;
    const ctx = chartCanvas.getContext('2d');
    const colorScale = scaleOrdinal(schemeCategory10);

    // filter out parameters with only undefined values
    const parameters = this.resultsets[0].benchmark.configurationParams.filter(parameter =>
      this.resultsets.filter(resultset => resultset.results.find(this.resultMatcher.bind(this, parameter))).length
    );
    const labels = parameters.map(kpi => kpi.name);
    const datasets = this.resultsets.map((resultset, index) => ({
      backgroundColor: 'transparent',
      borderColor: colorScale(index),
      data: parameters.map(parameter =>
        (resultset.results.find(this.resultMatcher.bind(this, parameter)) || {value: undefined}).value
      ),
      label: resultset.system.name,
    }));

    if (this.chart) {
      this.chart.destroy();
    }
    this.chart = new Chart.Chart(ctx, {
      type: 'radar',
      data: {
        datasets,
        labels,
      },
      options: {
        legend: {position: 'right'},
        responsive: true,
        scale: {
          ticks: {min: -1, max: 1},
        },
      }
    });
  }
}
