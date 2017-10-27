import { Component, OnInit } from '@angular/core';
import { Router, NavigationExtras } from '@angular/router';

import { ConfigurationParameterValue, NamedEntity, Experiment } from './model';
import { BackendService } from './services/backend.service';


@Component({
  selector: 'sg-experiments-wrapper',
  template: require('./experiments-wrapper.comp.html')
})
export class ExperimentsWrapperComponent implements OnInit {
  experiments: Experiment[];
  loaded: boolean = false;

  constructor(private bs: BackendService, private router: Router) {
  }

  ngOnInit() {
    this.bs.queryExperiments().subscribe(data => {
      this.experiments = data;
      this.loaded = true;
    });
  }
}
