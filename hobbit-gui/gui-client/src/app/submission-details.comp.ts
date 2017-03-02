import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { BackendService } from './services/backend.service';


@Component({
  selector: 'sg-submission-detail',
  template: `<h2>Submission Details for {{id}}</h2><pre>{{details}}</pre>`
})
export class SubmissionDetailsComponent implements OnInit {
  id: string;
  details: string;

  constructor(private activatedRoute: ActivatedRoute, private bs: BackendService) {
  }

  ngOnInit() {
    this.id = this.activatedRoute.snapshot.params['id'];
    this.bs.getSubmissionDetails(this.id).subscribe(data => {
      this.details = data;
    });
  }
}
