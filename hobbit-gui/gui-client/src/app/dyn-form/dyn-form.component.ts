import { ConfigParamDefinition } from './../model';
import { FormGroup } from '@angular/forms';
import { Component, Input, OnInit } from '@angular/core';

@Component({
  selector: 'app-dyn-form',
  templateUrl: './dyn-form.component.html'
})
export class DynFormComponent implements OnInit {
  @Input()
  private config: ConfigParamDefinition;

  @Input()
  private form: FormGroup;

  ngOnInit() {
  }

  get isValid() {
    return this.form.controls[this.config.id].valid;
  }
}
