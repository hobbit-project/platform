import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { BackendService } from './services/backend.service';

@Component({
  selector: 'sg-benchmark-result-validate',
  template: `<h2>Submission Details for {{id}}</h2><pre>{{details}}</pre>`
})
export class BenchmarkStatusComponent implements OnInit {

  details: string;

  constructor(private activatedRoute: ActivatedRoute, private bs: BackendService) {
  }

  ngOnInit() {
    this.bs.getStatus().subscribe(data => {
      this.details = data;
    });
  }

}
