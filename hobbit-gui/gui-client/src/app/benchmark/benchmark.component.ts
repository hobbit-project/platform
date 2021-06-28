import {MessageService} from 'primeng/components/common/messageservice';
import {Benchmark, BenchmarkOverview, System} from '../model';
import {Router} from '@angular/router';
import {BackendService} from '../backend.service';
import {Component, OnInit, ViewChild} from '@angular/core';
import {FormGroup} from '@angular/forms';
import {BsModalService} from 'ngx-bootstrap/modal';

@Component({
  selector: 'app-benchmark',
  templateUrl: './benchmark.component.html',
  styleUrls: ['./benchmark.component.less']
})
export class BenchmarkComponent implements OnInit {

  configFormGroup: FormGroup;

  benchmarks: BenchmarkOverview[];

  configModel: any = {};
  selectedBenchmark: Benchmark;
  selectedSystem: System;

  displaySubmitting: boolean;
  successfullySubmitted: boolean;

  @ViewChild('submitModal')
  submitModal: any;

  constructor(private bs: BackendService, private router: Router, private modalService: BsModalService,
    private messageService: MessageService) {
  }

  ngOnInit() {
    this.displaySubmitting = false;
    this.successfullySubmitted = false;

    this.configFormGroup = new FormGroup({});
    this.bs.listBenchmarks().subscribe(data => {
      this.benchmarks = data;
      this.benchmarks.sort((a, b) => a.name.toLowerCase().localeCompare(b.name.toLowerCase()));
      this.benchmarks = this.benchmarks.filter(b => b.errorMessage === undefined && !b.errorMessage);

      if (this.benchmarks.length === 0)
        this.messageService.add({ severity: 'warn', summary: 'No Benchmarks', detail: 'Did not find any benchmarks.' });

      if (window['repeatExperiment']) {
        const benchmark = this.benchmarks.find(b => b.id === window['repeatExperiment'].benchmark.id);
        if (benchmark) {
          this.configModel.benchmark = benchmark.id;
          this.onChangeBenchmark(benchmark.id);
        }
      }
    });
  }

  onChangeBenchmark(event) {
    this.configFormGroup = new FormGroup({});
    this.selectedSystem = null;

    const selectedBenchmarkOverview = this.benchmarks.find(b => b.id === event);
    if (selectedBenchmarkOverview) {
      this.bs.getBenchmarkDetails(event).subscribe(data => {
        this.selectedBenchmark = data;
        this.selectedBenchmark.systems.sort((a, b) => a.name.toLowerCase().localeCompare(b.name.toLowerCase()));

        this.selectedBenchmark.systems = this.filterInvalidSystems(this.selectedBenchmark.systems);

        if (window['repeatExperiment']) {
          const system = this.selectedBenchmark.systems.find(s => s.id === window['repeatExperiment'].system.id);
          if (system) {
            this.selectedSystem = system;
          }

          this.selectedBenchmark.configurationParams.forEach(param => {
            const experimentParamValue = window['repeatExperiment'].benchmark.configurationParamValues.find(v => v.id === param.id);
            if (experimentParamValue) {
              param.defaultValue = experimentParamValue.value;
            }
          });

          delete window['repeatExperiment'];
        }
      });
    }
  }

  private filterInvalidSystems(systems: System[]): System[] {
    const messages = [];
    for (const system of systems) {
      if (system.errorMessage !== undefined && !!system.errorMessage)
        messages.push({ severity: 'warn', summary: system.id, detail: system.errorMessage });
    }
    if (messages.length > 0)
      messages.unshift({ severity: 'warn', summary: 'Invalid Systems', detail: '' });
    this.messageService.addAll(messages);
    return systems.filter(s => s.errorMessage === undefined && !s.errorMessage);
  }

  onSubmitConfig(event) {
    this.configModel = {};
    this.configModel['benchmark'] = this.selectedBenchmark.id;
    this.configModel['system'] = this.selectedSystem.id;
    this.configModel['configurationParams'] = event;
    this.configModel['benchmarkName'] = this.selectedBenchmark.name;
    this.configModel['systemName'] = this.selectedSystem.name;

    this.bs.submitBenchmark(this.configModel).subscribe(data => {
      this.configModel['response'] = data;
      this.submitModal.show();
    }, error => {
      this.messageService.add({ severity: 'warn', summary: 'Error', detail: 'Failed to submit benchmark: ' + error.message });
    });
  }
}
