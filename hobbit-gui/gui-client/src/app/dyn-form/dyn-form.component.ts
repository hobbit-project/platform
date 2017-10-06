import { ConfigParamDefinition } from './../model';
import { FormGroup } from '@angular/forms';
import { Component, Input, OnInit } from '@angular/core';

@Component({
  selector: 'app-dyn-form',
  templateUrl: './dyn-form.component.html'
})
export class DynFormComponent implements OnInit {
  @Input()
  public config: ConfigParamDefinition;

  @Input()
  public form: FormGroup;

  ngOnInit() {
    if (this.config.getType() === 'dropdown')
      this.config.options.sort((a, b) => a.label > b.label ? 1 : (b.label > a.label ? -1 : 0));
  }

  get isValid() {
    return this.form.controls[this.config.id].valid;
  }
}
