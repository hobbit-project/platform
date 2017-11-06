import { ActivatedRoute } from '@angular/router';
import { Component, OnInit } from '@angular/core';
import { Location } from '@angular/common';

@Component({
  selector: 'app-details-wrapper',
  templateUrl: './details-wrapper.component.html'
})
export class DetailsWrapperComponent implements OnInit {

  public idsCommaSeparated: string;
  public challengeTaskId: string;

  constructor(private activatedRoute: ActivatedRoute, private location: Location) { }

  ngOnInit() {
    const params = {};
    Object.assign(params, this.activatedRoute.snapshot.queryParams);
    Object.assign(params, this.activatedRoute.snapshot.params);

    this.idsCommaSeparated = params['id'];
    this.challengeTaskId = params['task'];
  }

  cancel() {
    this.location.back();
  }

}
