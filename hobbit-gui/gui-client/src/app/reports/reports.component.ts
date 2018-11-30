import {MessageService} from 'primeng/components/common/messageservice';
import {BenchmarkOverview} from '../model';
import {Router} from '@angular/router';
import {BackendService} from '../backend.service';
import {Component, OnInit, ViewChild} from '@angular/core';

@Component({
  selector: 'app-reports',
  templateUrl: './reports.component.html',
  styleUrls: ['./reports.component.less']
})
export class ReportsComponent implements OnInit {

  benchmarks: BenchmarkOverview[];

  configModel: any = {};
  selectedBenchmark: BenchmarkOverview;

  constructor(private bs: BackendService, private router: Router,
    private messageService: MessageService) {
  }

  ngOnInit() {
    this.bs.listBenchmarks().subscribe(data => {
      this.benchmarks = data;
      this.benchmarks.sort((a, b) => a.name.toLowerCase().localeCompare(b.name.toLowerCase()));
      this.benchmarks = this.benchmarks.filter(b => b.errorMessage === undefined && !b.errorMessage);

      if (this.benchmarks.length === 0)
        this.messageService.add({ severity: 'warn', summary: 'No Benchmarks', detail: 'Did not find any benchmarks.' });
    });
  }

  onChangeBenchmark(event) {
    this.selectedBenchmark = this.benchmarks.find(b => b.id === event);
  }
}
