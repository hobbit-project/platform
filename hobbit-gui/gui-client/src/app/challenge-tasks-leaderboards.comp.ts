import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute, NavigationExtras } from '@angular/router';

import { Challenge, ChallengeTask } from './model';
import { BackendService } from './services/backend.service';


@Component({
  selector: 'sg-challenge-tasks-leaderboards',
  template: require('./challenge-tasks-leaderboards.comp.html')
})
export class ChallengeTasksLeaderboardsComponent implements OnInit {
  challenge: Challenge;
  loaded = false;
  selectedTask: ChallengeTask;

  constructor(private bs: BackendService, private activatedRoute: ActivatedRoute, private router: Router) {
  }

  ngOnInit() {
    let id = this.activatedRoute.snapshot.params['id'];
    let taskId = this.activatedRoute.snapshot.queryParams['task-id'];
    this.bs.getChallenge(id).subscribe(data => {
      this.challenge = data;
      if (taskId) {
        this.selectedTask = this.challenge.tasks.find(task => task.id === taskId);
      }
      this.loaded = true;
    });
  }

  onSelect(event) {
    let qparams = {'task-id': this.selectedTask.id};
    this.router.navigate(['challenges', this.challenge.id, 'leaderboards'], {queryParams: qparams});
  }
}
