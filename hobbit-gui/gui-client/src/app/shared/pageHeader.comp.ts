import { Component } from '@angular/core';

@Component({
  selector: 'sg-app-page-header',
  template: `
<div class="pgheader">
  <div class="container-fluid">
    <div class="row">
     <ng-content></ng-content>
    </div>
  </div>
</div>
  `,
  styles: [require('./pageHeader.comp.css')]
})
export class PageHeaderComponent {
}
