import { Component, OnInit, ViewChild } from '@angular/core';
import { Router, NavigationExtras, ActivatedRoute } from '@angular/router';
import { FormGroup } from '@angular/forms';
import { ConfirmationService } from 'primeng/primeng';

import {
  SelectOption, ConfigurationParameter, ConfigurationParameterValue, BenchmarkLight, System, Benchmark,
  Challenge, ChallengeTask, isChallengeOrganiser
} from './model';
import { BenchmarkConfigParamsComponent } from './benchmark-configparams.comp';
import { BackendService } from './services/backend.service';


@Component({
  selector: 'sg-challenge-task-edit',
  template: require('./challenge-task-edit.comp.html')
})
export class ChallengeTaskEditComponent implements OnInit {
  @ViewChild('configparams') configParams: BenchmarkConfigParamsComponent;
  configFormGroup: FormGroup;

  challenge: Challenge;
  task: ChallengeTask;
  taskIdx: number;
  loaded = false;
  adding = false;
  editPermission = false;
  benchmarks: BenchmarkLight[];
  benchmarksLoaded: boolean;
  selectedBenchmarkId: string;
  selectedBenchmark: Benchmark;
  selectedBenchmarkLight: BenchmarkLight;
  validBenchmarkId: boolean = false;
  error: string;

  constructor(private bs: BackendService, private activatedRoute: ActivatedRoute, private router: Router, private confirmationService: ConfirmationService) {
  }

  ngOnInit() {
    let id = this.activatedRoute.snapshot.params['id'];
    let taskId = this.activatedRoute.snapshot.params['task'];
    this.adding = taskId === 'add-task';
    this.bs.getChallenge(id).subscribe(data => {
      this.challenge = data;
      if (this.adding) {
        this.taskIdx = this.challenge.tasks.length;
        this.task = new ChallengeTask(`${id}_task${this.taskIdx + 1}`, `task${this.taskIdx + 1}`);
        this.loaded = true;
      } else {
        this.taskIdx = this.challenge.tasks.findIndex(t => t.id === taskId);
        if (this.taskIdx !== undefined) {
          this.task = this.challenge.tasks[this.taskIdx];
          if (this.task.benchmark && this.task.benchmark.id) {
            this.selectedBenchmarkId = this.task.benchmark.id;
          }
          this.loaded = true;
        } else {
          this.showError('Task not found');
        }
      }
    });
    this.bs.listBenchmarks().subscribe(data => {
      this.benchmarks = data;
      this.benchmarksLoaded = true;
      this.onChangeBenchmark();
    });
    this.bs.userInfo().subscribe(userInfo => {
      this.editPermission = isChallengeOrganiser(userInfo);
    });
    this.configFormGroup = new FormGroup({});
  }

  onChangeBenchmark() {
    this.selectedBenchmarkLight = this.benchmarks.find(b => { return b.id === this.selectedBenchmarkId; });
    this.selectedBenchmark = undefined;
    this.validBenchmarkId = false;
    this.configFormGroup = new FormGroup({});
    if (this.selectedBenchmarkLight) {
      this.validBenchmarkId = true;
      if (this.task.benchmark && this.task.benchmark.id === this.selectedBenchmarkId) {
          this.selectedBenchmark = this.task.benchmark;
      } else {
        this.bs.getBenchmarkDetails(this.selectedBenchmarkId).subscribe(data => {
          this.selectedBenchmark = data;
        });
      }
    }
  }

  onConfigFormGroup(event) {
    this.configFormGroup = event;
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
    }, err => {
      this.showError(err);
    });
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

  cancel() {
    this.router.navigate(['/challenges', this.activatedRoute.snapshot.params['id']]);
  }

  showError(err) {
    this.error = err;
  }
}
