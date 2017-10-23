import { Experiment } from './../../model';
import { BackendService } from './../../backend.service';
import { Router } from '@angular/router';
import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-experiments-wrapper',
  templateUrl: './wrapper.component.html'
})
export class WrapperComponent implements OnInit {

  experiments: Experiment[];
  constructor(private bs: BackendService, private router: Router) {
  }

  ngOnInit() {
    this.bs.queryExperiments().subscribe(data => {
      this.experiments = data;
    });
  }

}
