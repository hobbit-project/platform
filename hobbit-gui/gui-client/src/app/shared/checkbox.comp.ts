import { Component, Input } from '@angular/core';

@Component({
  selector: 'sg-checkbox',
  styles: [`
  .showcheckbox {
    font-family: FontAwesome;
    font-size: 16px;
  }
  `],
  template: `
<div [ngSwitch]="value">
  <template [ngSwitchCase]="false"><span class="showcheckbox">&#xf096;</span></template>
  <template [ngSwitchCase]="true"><span class="showcheckbox">&#xf046;</span></template>
</div>
  `
})
export class CheckboxComponent {
  @Input() value: boolean;
}
