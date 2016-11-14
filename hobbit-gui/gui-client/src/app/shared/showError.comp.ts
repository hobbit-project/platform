import { Component, Input } from '@angular/core';

@Component({
  selector: 'sg-show-error',
  template: `
<div *ngIf="!!error" class="alert alert-danger" role="alert">{{error}}</div>
  `
})
export class ShowErrorComponent {
  @Input() error: string;
}
