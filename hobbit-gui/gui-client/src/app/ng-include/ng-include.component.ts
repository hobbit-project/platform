import {Component, Input, OnInit} from '@angular/core';
import {HttpClient, HttpHeaders} from '@angular/common/http';

@Component({
  selector: 'ng-include',
  template: `<div [innerHTML]="body"></div>`,
})
export class NgIncludeComponent implements OnInit {
  @Input()
  src: string;

  body: string;

  constructor(private http: HttpClient) {
  }

  ngOnInit() {
    this.http.get(this.src, {headers: new HttpHeaders({'Cache-Control': 'no-cache, no-store, must-revalidate'}), responseType: 'text'}).subscribe(data => {
      this.body = data;
    });
  }
}
