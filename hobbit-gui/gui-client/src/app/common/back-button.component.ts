import { Location } from '@angular/common';
import { Component, OnInit, Input } from '@angular/core';

@Component({
  selector: 'back-button',
  template: '<button type="button" class="btn {{ rightClass }}" (click)="cancel()">Back</button>'
})
export class BackButtonComponent implements OnInit {

  @Input()
  pullRight = true;

  @Input()
  skipUrl;

  rightClass = '';

  constructor(private location: Location) { }

  ngOnInit() {
    if (this.pullRight)
      this.rightClass = 'pull-right';
    else
      this.rightClass = '';
  }

  cancel() {
    if (this.skipUrl && !this.location.path().endsWith(this.skipUrl))
      this.location.back();
    this.location.back();
  }

}
