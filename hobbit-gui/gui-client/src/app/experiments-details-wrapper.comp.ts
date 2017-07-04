import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'sg-experiments-details-wrapper',
  template: require('./experiments-details-wrapper.comp.html')
})
export class ExperimentsDetailsWrapperComponent implements OnInit {
  idsCommaSeparated: string;
  challengeTaskId: string;

  constructor(private activatedRoute: ActivatedRoute) {
  }

  ngOnInit() {
    let params = this.activatedRoute.snapshot.queryParams;
    this.idsCommaSeparated = params['id'];
    this.challengeTaskId = params['challenge-task-id'];
  }
}
