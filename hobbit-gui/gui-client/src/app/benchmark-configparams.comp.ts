import { Component, OnInit, OnChanges, Input, Output, EventEmitter } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { DynamicFormControlModel, DynamicFormService, DynamicFormOption,
         DynamicInputModel, DynamicCheckboxModel, DynamicSelectModel,
         DYNAMIC_FORM_CONTROL_INPUT_TYPE_TEXT, DYNAMIC_FORM_CONTROL_INPUT_TYPE_NUMBER  } from '@ng2-dynamic-forms/core';

import { SelectOption, ConfigurationParameter, ConfigurationParameterValue, BenchmarkLight, System, Benchmark } from './model';

@Component({
  selector: 'sg-benchmark-configparams',
  template: require('./benchmark-configparams.comp.html')
})
export class BenchmarkConfigParamsComponent implements OnInit, OnChanges {
  @Input() benchmark: Benchmark;
  @Input() configurationParams: ConfigurationParameterValue[];
  @Input() readonly: boolean = true;
  @Output() onConfigFormGroup = new EventEmitter<FormGroup>();
  configFormGroup: FormGroup;

  configDynamicFormModel: Array<DynamicFormControlModel>;

  constructor(private dynamicFormService: DynamicFormService) {
  }

  ngOnInit() {
    this.initConfigFormGroup();
  }

  ngOnChanges() {
    this.initConfigFormGroup();
  }

  private initConfigFormGroup() {
     this.configDynamicFormModel = new Array<DynamicFormControlModel>();
     if (this.hasConfigParams()) {
       let i = 0;
       for (let param of this.benchmark.configurationParams) {
         let input = this.createControlModel(i, param);
         this.configDynamicFormModel.push(input);
         i += 1;
       }
     }
     this.configFormGroup = this.dynamicFormService.createFormGroup(this.configDynamicFormModel);
     this.onConfigFormGroup.emit(this.configFormGroup);
  }

  private createControlModel(i: number, param: ConfigurationParameter): DynamicFormControlModel {
    let config = { id: `param_${i}`, label: param.name, required: param.required, value: param.defaultValue };
    if (this.configurationParams && this.configurationParams[i]) {
      config.value = this.configurationParams[i].value;
    }
    if (param.datatype === 'xsd:boolean') {
      let input = new DynamicCheckboxModel(config);
      input.disabled = this.readonly;
      return input;
    } else if (param.options) {
      let input = new DynamicSelectModel(config);
      input.disabled = this.readonly;
      let options = param.options.map(x => { return new DynamicFormOption(x); });
      input.options = options;
      return input;
    } else {
      let input = new DynamicInputModel(config);
      input.readOnly = this.readonly;
      if (this.isNumber(param)) {
        input.inputType = DYNAMIC_FORM_CONTROL_INPUT_TYPE_NUMBER;
        input.min = param.min;
        input.max = param.max;
        if (this.isFloatingPointNumber(param)) {
          let inputRaw: any = input;
          inputRaw.step = 'any';
        }
      } else {
        input.inputType = DYNAMIC_FORM_CONTROL_INPUT_TYPE_TEXT;
      }
      return input;
    }
  }

  isFloatingPointNumber(param: ConfigurationParameter): boolean {
    return param.datatype === 'xsd:decimal' || param.datatype === 'xsd:float' || param.datatype === 'xsd:double';
  }

  isNumber(param: ConfigurationParameter): boolean {
    return param.datatype === 'xsd:integer' || param.datatype === 'xsd:decimal' || param.datatype === 'xsd:float' || param.datatype === 'xsd:double';
  }

  hasConfigParams() {
    return this.benchmark && this.benchmark.configurationParams;
  }

  buildConfigurationParams() {
    let configurationParams = [];
    if (this.hasConfigParams()) {
       for (let input of this.configDynamicFormModel) {
         let value: string = input['value'];

         let param = this.benchmark.configurationParams[input.id.substr(6)];
         let paramValue = new ConfigurationParameterValue(param.id, param.name, param.datatype, value, param.range);
         configurationParams.push(paramValue);
       }
    }
    return configurationParams;
  }
}
