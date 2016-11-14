import { Component, OnInit } from '@angular/core';
import { Router, NavigationExtras } from '@angular/router';

import { BackendService } from './services/backend.service';


@Component({
  selector: 'sg-experiments',
  template: require('./experiments.comp.html')
})
export class ExperimentsComponent implements OnInit {
  constructor(private bs: BackendService, private router: Router) {
  }

  ngOnInit() {
  }
}
