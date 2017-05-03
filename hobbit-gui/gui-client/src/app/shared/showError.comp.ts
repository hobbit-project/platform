import { Component, Input } from '@angular/core';

@Component({
  selector: 'sg-show-error',
  template: `
<div *ngIf="!!error" class="alert alert-danger" role="alert">{{error}}</div>
<div *ngIf="!!info" class="alert alert-info" role="alert">{{info}}</div>
<div *ngIf="!!warning" class="alert alert-warning" role="alert">{{warning}}</div>
  `
})
export class ShowErrorComponent {
  @Input() error: string;
  @Input() info: string;
  @Input() message: string;
}
