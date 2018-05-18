import { FormGroup, FormControl, Validators, FormBuilder, AbstractControl } from '@angular/forms';
import { Benchmark, ConfigParamDefinition, ConfigParamRealisation, System } from './../../model';
import { Component, OnInit, OnChanges, Input, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'app-benchmark-config',
  templateUrl: './config.component.html'
})
export class ConfigComponent implements OnInit, OnChanges {

  @Input()
  benchmark: Benchmark;

  @Input()
  readOnly: boolean;

  @Input()
  system: System;

  @Output()
  submitCallback = new EventEmitter<any>();

  public loaded: Boolean;
  public formGroup: FormGroup;
  public config: ConfigParamDefinition[] = [];
  private configMap: { [s: string]: ConfigParamDefinition } = {};

  constructor(private formBuilder: FormBuilder) { }

  ngOnInit() {
    this.ngOnChanges();
  }

  ngOnChanges() {
    this.loaded = false;
    const group: { [s: string]: FormControl } = {};
    this.config = [];
    this.configMap = {};

    for (let i = 0; i < this.benchmark.configurationParams.length; i++) {
      const config = this.benchmark.configurationParams[i];
      const validators = [];
      if (config.required)
        validators.push(Validators.required);

      let value = config.defaultValue;
      if (this.benchmark.configurationParamValues) {
        const currentValue = this.benchmark.configurationParamValues.find(c => c.id === config.id);
        if (currentValue) {
          value = currentValue.value;
        }
      }

      group[config.id] = this.formBuilder.control(value, validators);
      this.config.push(config);
      this.configMap[config.id] = config;
    }
    this.formGroup = new FormGroup(group);
    this.loaded = true;
  }

  onSubmit() {
    const values = this.buildConfigurationParams();
    this.submitCallback.emit(values);
  }

  buildConfigurationParams() {
    const realisations = [];
    if (this.benchmark.hasConfigParams()) {
      for (const id of Object.keys(this.formGroup.controls)) {
        const value: string = this.formGroup.controls[id]['value'];

        const param = this.configMap[id];
        const paramValue = new ConfigParamRealisation(param.id, param.name, param.datatype, value, param.description, param.range);
        realisations.push(paramValue);
      }
    }
    return realisations;
  }

}
