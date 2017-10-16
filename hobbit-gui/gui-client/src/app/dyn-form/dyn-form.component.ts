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
      this.config.options.sort((a, b) => a.label.toLowerCase().localeCompare(b.label.toLowerCase()));
  }

  get isValid() {
    return this.form.controls[this.config.id].valid;
  }
}
