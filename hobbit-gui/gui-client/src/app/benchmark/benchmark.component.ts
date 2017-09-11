import { System, Benchmark, BenchmarkOverview } from './../model';
import { Router } from '@angular/router';
import { BackendService } from './../backend.service';
import { Component, OnInit, ViewChild } from '@angular/core';
import { FormGroup } from '@angular/forms';

@Component({
  selector: 'app-benchmark',
  templateUrl: './benchmark.component.html',
  styleUrls: ['./benchmark.component.css']
})
export class BenchmarkComponent implements OnInit {

  // @ViewChild('configparams')
  // configParams: BenchmarkConfigParamsComponent;
  configFormGroup: FormGroup;

  benchmarks: BenchmarkOverview[];
  benchmarksLoaded: boolean;

  model: any = {};
  selectedBenchmark: Benchmark;
  selectedSystem: System;

  displaySubmitting: boolean;
  successfullySubmitted: boolean;

  constructor(private bs: BackendService, private router: Router) {
  }

  ngOnInit() {
    this.displaySubmitting = false;
    this.successfullySubmitted = false;

    this.configFormGroup = new FormGroup({});
    this.bs.listBenchmarks().subscribe(data => {
      this.benchmarks = data;
      this.benchmarksLoaded = true;
    });
  }

  onChangeBenchmark(event) {
    this.selectedBenchmark = undefined;
    this.configFormGroup = new FormGroup({});

    const selectedBenchmarkOverview = this.benchmarks.find(b => b.id === event);
    if (selectedBenchmarkOverview) {
      this.bs.getBenchmarkDetails(event).subscribe(data => {
        this.selectedBenchmark = data;
      }, err => {
        console.error(err);
      });
    }
  }

  onChangeSystem(event) {
    if (this.selectedBenchmark && event) {
      this.selectedSystem = this.selectedBenchmark.systems.find(x => x.id === event);
    } else {
      this.selectedSystem = undefined;
    }
  }
}
