import { MessageService } from 'primeng/components/common/messageservice';
import { QueuedExperimentBean, RunningExperimentBean } from './../../../model';
import { Component, OnInit, Input } from '@angular/core';
import { BackendService } from '../../../backend.service';

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
  show = true;

  maxRuntime: number;
  runtime: number;
  remainingRuntime: number;

  constructor(private bs: BackendService, private ms: MessageService) { }

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
