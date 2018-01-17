import { StatusBean } from './../../model';
import { BackendService } from './../../backend.service';
import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-status',
  templateUrl: './status.component.html'
})
export class StatusComponent implements OnInit {

  public status: StatusBean;

  constructor(private bs: BackendService) { }

  ngOnInit() {
    this.bs.getStatus().subscribe(data => {
      this.status = data;
    });
  }

}
