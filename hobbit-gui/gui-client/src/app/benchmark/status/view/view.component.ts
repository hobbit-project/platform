import { MessageService } from 'primeng/components/common/messageservice';
import { QueuedExperimentBean, RunningExperimentBean } from './../../../model';
import { Component, OnInit, OnChanges, Input } from '@angular/core';
import { BackendService } from '../../../backend.service';

@Component({
  selector: 'app-experiment-view',
  templateUrl: './view.component.html',
  styleUrls: ['./view.component.less']
})
export class ViewComponent implements OnInit, OnChanges {

  @Input()
  experiment: QueuedExperimentBean;

  runningExperiment: RunningExperimentBean = null;
  cancelable = false;
  show = true;

  maxRuntime: number;
  runtime: number;
  remainingRuntime: number;

  constructor(private bs: BackendService, private ms: MessageService) {
    setInterval(this.updateRemainingTime.bind(this), 500);
  }

  ngOnInit() {
    this.update();
  }

  ngOnChanges() {
    this.update();
  }

  update() {
    if (this.experiment instanceof RunningExperimentBean) {
      this.runningExperiment = this.experiment;

      if (this.runningExperiment.latestDateToFinish !== null) {
        this.maxRuntime = new Date(this.runningExperiment.latestDateToFinish).getTime() -
          new Date(this.runningExperiment.startTimestamp).getTime();
        this.updateRemainingTime();
      }
    }
    this.cancelable = this.experiment.canBeCanceled;
  }

  updateRemainingTime() {
    if (this.maxRuntime) {
      this.runtime = new Date().getTime() - new Date(this.runningExperiment.startTimestamp).getTime();
      this.remainingRuntime = Math.max(0, Math.ceil((this.maxRuntime - this.runtime) / 1000));
    }
  }

  formatRemainingTime(sec) {
    let fmt = '';
    if (sec > 60) {
      let min = Math.floor(sec / 60);
      if (min > 60) {
        const hour = Math.floor(min / 60);
        fmt += hour + ':';
        min %= 60;
      }
      fmt += ('0' + min).substr(-2) + ':';
      sec %= 60;
    }
    fmt += ('0' + sec).substr(-2);
    return fmt;
  }

  public cancel() {
    this.cancelable = false;
    this.bs.terminateExperiment(this.experiment.experimentId).subscribe(data => {
      this.show = false;
    }, error => {
      this.cancelable = true;
      this.ms.add({ severity: 'warn', summary: 'Failed', detail: 'Unable to remove experiment ' + this.experiment.experimentId });
    });
  }

}
