import { ConfigComponent } from './../../benchmark/config/config.component';
import { ConfirmationService } from 'primeng/primeng';
import { FormGroup } from '@angular/forms';
import { BackendService } from './../../backend.service';
import { ActivatedRoute, Router } from '@angular/router';
import { Challenge, ChallengeTask, Benchmark, BenchmarkOverview, Role } from './../../model';
import { Component, OnInit, ViewChild } from '@angular/core';

@Component({
  selector: 'app-task',
  templateUrl: './task.component.html'
})
export class TaskComponent implements OnInit {

  public static readonly ADD_TASK = 'add-task';

  @ViewChild(ConfigComponent)
  configParams: ConfigComponent;

  challenge: Challenge;
  task: ChallengeTask;
  benchmarks: BenchmarkOverview[];

  adding: boolean;
  loaded: boolean;
  benchmarksLoaded: boolean;
  taskIdx: number;

  editPermission: boolean;
  selectedBenchmarkId: string;
  selectedBenchmark: Benchmark;

  constructor(private activatedRoute: ActivatedRoute, private bs: BackendService, private router: Router,
    private confirmationService: ConfirmationService) { }

  ngOnInit() {
    const id = this.activatedRoute.snapshot.params['id'];
    const taskId = this.activatedRoute.snapshot.params['task'];
    this.adding = taskId === TaskComponent.ADD_TASK;

    this.bs.getChallenge(id).subscribe(data => {
      this.challenge = data;
      if (this.adding) {
        this.taskIdx = this.challenge.tasks.length;
        this.task = new ChallengeTask(`${id}_task${this.taskIdx + 1}`, `task${this.taskIdx + 1}`);
        this.loaded = true;
      } else {
        this.taskIdx = this.challenge.tasks.findIndex(t => t.id === taskId);
        if (this.taskIdx !== -1) {
          this.task = this.challenge.tasks[this.taskIdx];
          if (this.task.benchmark && this.task.benchmark.id) {
            this.selectedBenchmarkId = this.task.benchmark.id;
            if (this.benchmarks) {
              this.onChangeBenchmark();
            }
          }
          this.loaded = true;
        } else {
          this.router.navigateByUrl('404', { skipLocationChange: true });
        }
      }
    });

    this.bs.listBenchmarks().subscribe(data => {
      this.benchmarks = data;
      this.benchmarksLoaded = true;
      this.onChangeBenchmark();
    });
    this.bs.userInfo().subscribe(userInfo => {
      this.editPermission = userInfo.hasRole(Role.CHALLENGE_ORGANISER);
    });
  }

  canEdit() {
    return this.editPermission && this.challenge && !this.challenge.closed;
  }

  canSave() {
    return this.selectedBenchmarkId !== undefined;
  }

  onChangeBenchmark() {
    const selectedBenchmarkOverview = this.benchmarks.find(b => b.id === this.selectedBenchmarkId);
    this.selectedBenchmark = undefined;
    if (selectedBenchmarkOverview) {
      if (this.task.benchmark && this.task.benchmark.id === this.selectedBenchmarkId)
        this.selectedBenchmark = this.task.benchmark;
      else
        this.bs.getBenchmarkDetails(this.selectedBenchmarkId).subscribe(data => {
          this.selectedBenchmark = data;
        });
    }
  }

  cancel() {
    this.router.navigate(['/challenges', this.activatedRoute.snapshot.params['id']]);
  }

  deleteTask() {
    this.confirmationService.confirm({
      message: 'Do you want to delete this task?',
      header: 'Delete Confirmation',
      icon: 'fa fa-trash',
      accept: () => {
        this.challenge.tasks.splice(this.taskIdx);
        this.doSave();
      }
    });
  }

  saveTask() {
    this.challenge.tasks[this.taskIdx] = this.task;
    this.task.benchmark = this.selectedBenchmark;
    if (this.task.benchmark) {
      this.task.configurationParams = this.configParams.buildConfigurationParams();
    } else {
      this.task.configurationParams = [];
    }
    this.doSave();
  }


  private doSave() {
    this.bs.updateChallenge(this.challenge).subscribe(ok => {
      this.cancel();
    });
  }

}
