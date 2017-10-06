import { Router } from '@angular/router';
import { Experiment, ChallengeTask } from './../model';
import { BackendService } from './../backend.service';
import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-experiments',
  templateUrl: './experiments.component.html',
  styleUrls: ['./experiments.component.less']
})
export class ExperimentsComponent implements OnInit {

  public distinctTasks: ChallengeTask[];
  private experiments: Experiment[];
  public selectedExperiments: Experiment[];

  public loaded = false;

  constructor(private bs: BackendService, private router: Router) { }

  ngOnInit() {
    this.bs.queryExperiments().subscribe(data => {
      this.experiments = data;
      const map = new Map<String, ChallengeTask>();
      for (const ex of this.experiments) {
        if (ex.challengeTask && ex.challengeTask.name) {
          map.set(ex.challengeTask.id, ex.challengeTask);
        }
      }
      this.distinctTasks = Array.from(map.values());
      this.loaded = true;
    });
  }

  showDetails() {
    const ids = this.selectedExperiments.map(ex => ex.id).join(',');
    this.router.navigate(['experiments/', ids]);
  }

  showDetailsForChallengeTask(task) {
    this.router.navigate(['experiments/task', task.data.id]);
  }

}
