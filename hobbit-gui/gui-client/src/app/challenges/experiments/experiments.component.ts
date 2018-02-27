import { Location } from '@angular/common';
import { Challenge, ExperimentCount } from './../../model';
import { ActivatedRoute, Router } from '@angular/router';
import { BackendService } from './../../backend.service';
import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-experiments',
  templateUrl: './experiments.component.html',
  styleUrls: ['./experiments.component.less']
})
export class ExperimentsComponent implements OnInit {

  public challenge: Challenge;
  private counts: ExperimentCount[] = [];
  private selectedCount: ExperimentCount;

  constructor(private bs: BackendService, private activatedRoute: ActivatedRoute, private router: Router,
    private location: Location) { }

  ngOnInit() {
    const id = this.activatedRoute.snapshot.params['id'];
    this.bs.getChallenge(id).subscribe(data => {
      this.challenge = data;
    });

    this.bs.countExperiments(id).subscribe(data => {
      this.counts = data;
      let taskId = this.activatedRoute.snapshot.params['task'];
      if (taskId) {
        taskId = decodeURIComponent(taskId);
        this.selectedCount = this.counts.find(value => value.challengeTask.id === taskId);
      }
    });
  }

  handleSelect(event) {
    this.selectedCount = event.data;
    const url = this.router.createUrlTree([], { relativeTo: this.activatedRoute }).toString();

    const tokens = url.split('/');
    if (tokens[tokens.length - 1] !== 'experiments')
      tokens.splice(-1, 1);
    tokens.push(encodeURIComponent(this.selectedCount.challengeTask.id));

    this.location.go(tokens.join('/'));
  }

}
