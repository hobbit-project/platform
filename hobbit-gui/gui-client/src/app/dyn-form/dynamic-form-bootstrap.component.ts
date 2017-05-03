import { Component, Input, ContentChild, TemplateRef } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { DynamicFormControlComponent, DynamicFormControlModel } from '@ng2-dynamic-forms/core';

export const DYNAMIC_FORM_UI_BOOTSTRAP = 'BOOTSTRAP';

@Component({
    selector: 'dynamic-form-bootstrap-control',
    template: require('./dynamic-form-bootstrap.component.html')
})

export class DynamicFormBootstrapComponent extends DynamicFormControlComponent {

    @Input() controlGroup: FormGroup;
    @Input() model: DynamicFormControlModel;

    @Input() hasSuccessStyles: boolean = false;
    @Input() hasErrorStyles: boolean = false;

    @ContentChild(TemplateRef) customTemplate;

    type: string = DYNAMIC_FORM_UI_BOOTSTRAP;

    constructor() {
        super();
    }
}
