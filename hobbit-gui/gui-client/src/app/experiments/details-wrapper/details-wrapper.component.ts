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
    const {params, queryParams} = this.activatedRoute.snapshot;

    if (params['id'] !== 'details') {
      this.idsCommaSeparated = params['id'];
      this.challengeTaskId = params['task'];

    } else {
      // handle old URLs
      this.idsCommaSeparated = queryParams['id'];
      this.challengeTaskId = queryParams['challenge-task-id'];
    }

    console.log(this.idsCommaSeparated, this.challengeTaskId);
  }

}
