import { Component, ViewChild, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Benchmark, System, ConfigurationParameter } from './model';

import { BackendService } from './services/backend.service';

@Component({
  selector: 'sg-benchmark-submit-response',
  template: require('./benchmark-submit-response.comp.html')
})
export class BenchmarkSubmitResponseComponent implements OnInit {
  model: any;
  submitted: boolean;
  backend: string;

  constructor(private activatedRoute: ActivatedRoute, private bs: BackendService) {
  }

  ngOnInit() {
    this.submitted = false;
    this.backend = this.bs.getBackendUrl();
    this.activatedRoute.queryParams.subscribe(params => {
      this.model = JSON.parse(sessionStorage.getItem(params['id']));
      this.bs.submitBenchmark(this.model).subscribe(data => {
        this.model.submission = data;
        this.submitted = true;
      });
    });
  }
}
