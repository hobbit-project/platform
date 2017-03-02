import { Component, ViewChild, OnInit } from '@angular/core';
import { Router, NavigationExtras } from '@angular/router';
import { FormGroup } from '@angular/forms';

import { SelectOption, ConfigurationParameter, ConfigurationParameterValue, BenchmarkLight, System, Benchmark } from './model';
import { BenchmarkConfigParamsComponent } from './benchmark-configparams.comp';
import { BackendService } from './services/backend.service';

@Component({
  selector: 'sg-benchmark-submit',
  template: require('./benchmark-submit.comp.html')
})
export class BenchmarkSubmitComponent implements OnInit {
  @ViewChild('configparams') configParams: BenchmarkConfigParamsComponent;
  configFormGroup: FormGroup;

  benchmarks: BenchmarkLight[];
  benchmarksLoaded: boolean;
  model: any = {};
  selectedBenchmarkLight: BenchmarkLight;
  selectedBenchmark: Benchmark;
  selectedSystem: System;

  constructor(private bs: BackendService, private router: Router) {
  }

  ngOnInit() {
    this.configFormGroup = new FormGroup({});
    this.bs.listBenchmarks().subscribe(data => {
      this.benchmarks = data;
      this.benchmarksLoaded = true;
    });
  }

  onConfigFormGroup(event) {
    this.configFormGroup = event;
  }

  onChangeBenchmark(event) {
    this.model.benchmark = event;
    this.selectedBenchmarkLight = this.benchmarks.find(b => { return b.id === this.model.benchmark; });
    this.selectedBenchmark = undefined;
    this.configFormGroup = new FormGroup({});
    if (this.selectedBenchmarkLight) {
      this.bs.getBenchmarkDetails(this.model.benchmark).subscribe(data => {
        this.selectedBenchmark = data;
      }, err => {
        console.error(err);
      });
    }
  }

  onChangeSystem(event) {
    this.model.system = event;
    if (this.selectedBenchmark && this.selectedBenchmark.systems) {
      this.selectedSystem = this.selectedBenchmark.systems.find(x => { return x.id === this.model.system; });
    } else {
      this.selectedSystem = undefined;
    }
  }

  onSubmit() {
    this.model.configurationParams = this.configParams.buildConfigurationParams();

    this.model.benchmarkName = this.selectedBenchmark.name;
    this.model.systemName = this.selectedBenchmark.systems.find(x => { return x.id === this.model.system; }).name;
    console.log('submit');
    console.log(JSON.stringify(this.model));
    let link = ['benchmarks/submitted'];
    sessionStorage.setItem('benchmark-submission', JSON.stringify(this.model));
    this.router.navigate(link, { queryParams: { id: 'benchmark-submission' } });
  }
}
