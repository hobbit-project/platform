import { Component, ViewChild, OnInit, Input, Output, EventEmitter, OnChanges } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Benchmark, System, ConfigurationParameter } from './model';

import { BackendService } from './services/backend.service';

@Component({
  selector: 'sg-benchmark-submit-response',
  template: require('./benchmark-submit-response.comp.html')
})
export class BenchmarkSubmitResponseComponent implements OnInit, OnChanges {
  @Input() model: any;
  @Input() submitting: boolean;
  submitted: boolean;
  error: string;
  @Output() successfullySubmitted = new EventEmitter<boolean>();

  constructor(private bs: BackendService) {
  }

  ngOnInit() {
    this.submitted = false;
    this.submitting = false;
  }

  ngOnChanges() {
    if (this.submitting) {
      this.submitted = false;
      this.submitting = false;
      this.error = undefined;
      this.bs.submitBenchmark(this.model).subscribe(
        data => {
          if (!data.error) {
            this.model.submission = data;
            this.submitted = true;
            this.successfullySubmitted.emit(true);
          } else {
            console.log(data.error);
            this.error = data.error;
          }
        },
        error => {
          console.log(error);
          this.error = error;
        });
    }
  }
}
