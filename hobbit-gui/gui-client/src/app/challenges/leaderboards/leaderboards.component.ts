import { Location } from '@angular/common';
import { Challenge, ChallengeTask } from './../../model';
import { ActivatedRoute, Router } from '@angular/router';
import { BackendService } from './../../backend.service';
import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-leaderboards',
  templateUrl: './leaderboards.component.html',
  styleUrls: ['./leaderboards.component.less']
})
export class LeaderboardsComponent implements OnInit {

  public challenge: Challenge;
  private tasks: ChallengeTask[] = [];
  private selectedTask: ChallengeTask;

  constructor(private bs: BackendService, private activatedRoute: ActivatedRoute, private router: Router,
    private location: Location) { }

  ngOnInit() {
    const id = this.activatedRoute.snapshot.params['id'];
    this.bs.getChallenge(id).subscribe(data => {
      this.challenge = data;
      let taskId = this.activatedRoute.snapshot.params['task'];
      if (taskId) {
        taskId = decodeURIComponent(taskId);
        this.selectedTask = this.challenge.tasks.find(task => task.id === taskId);
      }
    });
  }

  handleSelect(event) {
    this.selectedTask = event.data;
    const url = this.router.createUrlTree([], { relativeTo: this.activatedRoute }).toString();

    const tokens = url.split('/');
    if (tokens[tokens.length - 1] !== 'leaderboards')
      tokens.splice(-1, 1);
    tokens.push(encodeURIComponent(this.selectedTask.id));

    this.location.go(tokens.join('/'));
  }

}
