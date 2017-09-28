import { MessageService } from 'primeng/components/common/messageservice';
import { plainToClass } from 'class-transformer';
import { System, Benchmark, BenchmarkOverview } from './../model';
import { Router } from '@angular/router';
import { BackendService } from './../backend.service';
import { Component, OnInit, ViewChild, TemplateRef } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { BsModalService, BsModalRef } from 'ngx-bootstrap/modal';

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
      this.benchmarks.sort((a, b) => a.name > b.name ? 1 : (b.name > a.name ? -1 : 0));

      if (this.benchmarks.length === 0)
        this.messageService.add({ severity: 'warn', summary: 'No Benchmarks', detail: 'Did not find any benchmarks.' });
    });
  }

  onChangeBenchmark(event) {
    this.configFormGroup = new FormGroup({});

    const selectedBenchmarkOverview = this.benchmarks.find(b => b.id === event);
    if (selectedBenchmarkOverview) {
      this.bs.getBenchmarkDetails(event).subscribe(data => {
        this.selectedBenchmark = data;
        this.selectedBenchmark.systems.sort((a, b) => a.name > b.name ? 1 : (b.name > a.name ? -1 : 0));
      });
    }
  }

  onChangeSystem(event) {
    if (this.selectedBenchmark && event) {
      this.selectedSystem = this.selectedBenchmark.systems.find(x => x.id === event);
    } else {
      this.selectedSystem = undefined;
    }
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
    });
  }
}
