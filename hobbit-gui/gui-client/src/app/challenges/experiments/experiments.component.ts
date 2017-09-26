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

  private challenge: Challenge;
  private counts: ExperimentCount[] = [];
  private selectedCount: ExperimentCount;

  constructor(private bs: BackendService, private activatedRoute: ActivatedRoute, private router: Router) { }

  ngOnInit() {
    const id = this.activatedRoute.snapshot.params['id'];
    const taskId = this.activatedRoute.snapshot.queryParams['task-id'];
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

  onSelect(event) {
    const qparams = { 'task-id': this.selectedCount.challengeTask.id };
    this.router.navigate(['challenges', this.challenge.id, 'experiments'], { queryParams: qparams });
  }

}
