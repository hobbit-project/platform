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

  constructor(private bs: BackendService, private activatedRoute: ActivatedRoute, private router: Router) { }

  ngOnInit() {
    const id = this.activatedRoute.snapshot.params['id'];
    const taskId = this.activatedRoute.snapshot.queryParams['task'];
    this.bs.getChallenge(id).subscribe(data => {
      this.challenge = data;
      if (taskId) {
        this.selectedTask = this.challenge.tasks.find(task => task.id === taskId);
      }
    });
  }

}
