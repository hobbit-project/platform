import { Component, OnInit } from '@angular/core';
import { Router, NavigationExtras, ActivatedRoute } from '@angular/router';
import { ConfirmationService } from 'primeng/primeng';

import {
  SelectOption, ConfigurationParameter, ConfigurationParameterValue, BenchmarkLight, System, Benchmark,
  Challenge, ChallengeTask, isChallengeOrganiser, isSystemProvider
} from './model';
import { BackendService } from './services/backend.service';



@Component({
  selector: 'sg-challenge-edit',
  template: require('./challenge-edit.comp.html')
})
export class ChallengeEditComponent implements OnInit {
  challenge: Challenge;
  selectedTask: ChallengeTask;
  loaded = false;
  editPermission = false;
  registerPermission = false;
  adding = false;
  error: string;

  constructor(private bs: BackendService, private activatedRoute: ActivatedRoute, private router: Router, private confirmationService: ConfirmationService) {
  }

  ngOnInit() {
    let id = this.activatedRoute.snapshot.params['id'];
    this.adding = (id === 'add-challenge');
    if (this.adding) {
      this.challenge = new Challenge('', '');
      this.challenge.tasks = [];
      this.loaded = true;
      this.bs.userInfo().subscribe(userInfo => {
        this.challenge.organizer = userInfo.name;
      });
    } else {
      this.bs.getChallenge(id).subscribe(data => {
        this.challenge = data;
        this.loaded = true;
      });
    }
    this.bs.userInfo().subscribe(userInfo => {
      this.editPermission = isChallengeOrganiser(userInfo);
      this.registerPermission = isSystemProvider(userInfo);
    });
  }

  onSelect(event) {
    this.router.navigate(['/challenges', this.challenge.id, 'edit', this.selectedTask.id]);
  }

  saveChallenge() {
    this.error = undefined;
    if (this.adding) {
      this.bs.addChallenge(this.challenge).subscribe(ok => {
        this.cancel();
      }, err => {
        this.showError(err);
      });
    } else {
      this.bs.updateChallenge(this.challenge).subscribe(ok => {
        this.cancel();
      }, err => {
        this.showError(err);
      });
    }
  }

  showError(err) {
    this.error = err;
  }

  deleteChallenge() {
    this.confirmationService.confirm({
      message: 'Do you want to delete this challenge?',
      header: 'Delete Confirmation',
      icon: 'fa fa-trash',
      accept: () => {
        this.bs.deleteChallenge(this.challenge.id).subscribe(ok => {
          this.cancel();
        }, err => {
          this.showError(err);
        });
      }
    });
  }

  cancel() {
    this.router.navigate(['/challenges']);
  }

  addTask() {
    this.router.navigate(['/challenges', this.challenge.id, 'edit', 'add-task']);
  }

  showSystemRegistration() {
    this.router.navigate(['challenges', this.challenge.id, 'registrations']);
  }

  registerSystems() {
    this.router.navigate(['challenges', this.challenge.id, 'register']);
  }
}
