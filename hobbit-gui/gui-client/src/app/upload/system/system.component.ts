import { BenchmarkOverview, SystemMetaFile, User } from './../../model';
import { BackendService } from './../../backend.service';
import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-system',
  templateUrl: './system.component.html'
})
export class SystemComponent implements OnInit {

  public benchmarks: BenchmarkOverview[];
  public selectedBenchmark: BenchmarkOverview;
  public system: SystemMetaFile;
  public metaFile = {
    error: false,
    msg: ''
  };

  private user: User;

  constructor(private bs: BackendService) { }

  ngOnInit() {
    this.bs.listBenchmarks().subscribe(data => {
      this.benchmarks = data;
    });
    this.bs.userInfo().subscribe(data => {
      this.user = data;
    });

    this.system = new SystemMetaFile('', '', '', '');
  }

  onSubmit() {
    this.metaFile.error = false;
    this.metaFile.msg = '';
    this.bs.getSystemMetaFile(this.system).subscribe(res => {
      this.metaFile.msg = res;
    }, error => {
      this.metaFile.error = true;
      this.metaFile.msg = error._body;
    });
  }

  generateURI(event) {
    this.system.id = 'http://www.example.org/' + encodeURIComponent(this.user.name) + '/' + encodeURIComponent(this.system.name);
    event.preventDefault();
  }

}
