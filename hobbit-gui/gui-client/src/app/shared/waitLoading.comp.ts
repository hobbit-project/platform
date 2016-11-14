import { Component, Input } from '@angular/core';

@Component({
  selector: 'sg-wait-loading',
  template: `
<p *ngIf="!loaded">Loading... <img src="assets/images/ajax-loader.gif" alt="Loading..."></p>
  `
})
export class WaitLoadingComponent {
  @Input() loaded: boolean;
}
