import { QueuedExperimentBean, RunningExperimentBean } from './../../../model';
import { Component, OnInit, Input } from '@angular/core';

@Component({
  selector: 'app-experiment-view',
  templateUrl: './view.component.html',
  styleUrls: ['./view.component.less']
})
export class ViewComponent implements OnInit {

  @Input()
  experiment: QueuedExperimentBean;

  runningExperiment: RunningExperimentBean = null;
  cancelable = false;

  maxRuntime: number;
  runtime: number;
  remainingRuntime: number;

  constructor() { }

  ngOnInit() {
    if (this.experiment instanceof RunningExperimentBean) {
      this.runningExperiment = this.experiment;

      if (this.runningExperiment.latestDateToFinish !== null) {
        this.maxRuntime = new Date(this.runningExperiment.latestDateToFinish).getTime() -
          new Date(this.runningExperiment.startTimestamp).getTime();
        this.runtime = new Date().getTime() - new Date(this.runningExperiment.startTimestamp).getTime();
        this.remainingRuntime = Math.ceil((this.maxRuntime - this.runtime) / 1000);
      }
    }
    this.cancelable = this.experiment.canBeCanceled && this.runningExperiment == null;
  }

}
