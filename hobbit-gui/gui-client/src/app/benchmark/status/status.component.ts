import { StatusBean } from './../../model';
import { BackendService } from './../../backend.service';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { Title } from '@angular/platform-browser';

@Component({
  selector: 'app-status',
  templateUrl: './status.component.html'
})
export class StatusComponent implements OnInit, OnDestroy {

  public status: StatusBean;

  constructor(private bs: BackendService, private titleService: Title) { }

  ngOnInit() {
    this.init();

    Observable.interval(30000).subscribe(x => {
      this.init();
    });
  }

  private init() {
    this.bs.getStatus().subscribe(data => {
      this.status = data;

      const experimentAmount = this.status.queuedExperiments.length + (this.status.runningExperiment ? 1 : 0);
      this.titleService.setTitle(`HOBBIT (${experimentAmount})`);
    });
  }

  ngOnDestroy() {
    this.titleService.setTitle(`HOBBIT`);
  }

}
