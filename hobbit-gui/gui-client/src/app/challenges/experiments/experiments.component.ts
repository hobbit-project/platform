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

  constructor(private bs: BackendService, private activatedRoute: ActivatedRoute, private router: Router) { }

  ngOnInit() {
    const id = this.activatedRoute.snapshot.params['id'];
    const taskId = this.activatedRoute.snapshot.queryParams['task'];
    this.bs.getChallenge(id).subscribe(data => {
      this.challenge = data;
    });

    this.bs.countExperiments(id).subscribe(data => {
      this.counts = data;
      if (taskId) {
        this.selectedCount = this.counts.find(value => value.challengeTask.id === taskId);
      }
    });
  }

}
