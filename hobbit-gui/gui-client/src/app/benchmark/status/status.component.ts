import { StatusBean } from './../../model';
import { BackendService } from './../../backend.service';
import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs/Observable';

@Component({
  selector: 'app-status',
  templateUrl: './status.component.html'
})
export class StatusComponent implements OnInit {

  public status: StatusBean;

  constructor(private bs: BackendService) { }

  ngOnInit() {
    this.init();

    Observable.interval(30000).subscribe(x => {
      this.init();
    });
  }

  private init() {
    this.bs.getStatus().subscribe(data => {
      this.status = data;
    });
  }

}
