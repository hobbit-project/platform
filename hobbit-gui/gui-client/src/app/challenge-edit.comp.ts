import { Component, OnInit } from '@angular/core';
import { Router, NavigationExtras, ActivatedRoute } from '@angular/router';
import { ConfirmationService } from 'primeng/primeng';

import {
  SelectOption, ConfigurationParameter, ConfigurationParameterValue, BenchmarkLight, System, Benchmark,
  Challenge, ChallengeTask, isChallengeOrganiser, isSystemProvider
} from './model';
import { BackendService } from './services/backend.service';

class ChallengeDates {
  constructor(public executionDate?: Date, public publishDate?: Date) {}
}

@Component({
  selector: 'sg-challenge-edit',
  template: require('./challenge-edit.comp.html')
})
export class ChallengeEditComponent implements OnInit {
  challenge: Challenge;
  dates: ChallengeDates;
  selectedTask: ChallengeTask;
  loaded = false;
  editPermission = false;
  registerPermission = false;
  adding = false;
  error: string;
  infoMessage: string;
  closeIsActive = false;

  constructor(private bs: BackendService, private activatedRoute: ActivatedRoute, private router: Router, private confirmationService: ConfirmationService) {
  }

  ngOnInit() {
    let id = this.activatedRoute.snapshot.params['id'];
    this.adding = (id === 'add-challenge');
    if (this.adding) {
      this.challenge = new Challenge('', '');
      this.dates = new ChallengeDates();
      this.challenge.tasks = [];
      this.loaded = true;
      this.bs.userInfo().subscribe(userInfo => {
        this.challenge.organizer = userInfo.name;
      });
    } else {
      this.bs.getChallenge(id).subscribe(data => {
        this.challenge = data;
        this.updateDatesFromChallenge();
        this.loaded = true;
      });
    }
    this.bs.userInfo().subscribe(userInfo => {
      this.editPermission = isChallengeOrganiser(userInfo);
      this.registerPermission = isSystemProvider(userInfo);
    });
  }

  private updateDatesFromChallenge() {
    this.dates = new ChallengeDates();
    if (this.challenge.executionDate) {
      this.dates.executionDate = new Date(this.challenge.executionDate);
    }
    if (this.challenge.publishDate) {
      this.dates.publishDate = new Date(this.challenge.publishDate);
    }
  }

  private updateChallengeFromDates() {
    if (this.dates.executionDate) {
      this.challenge.executionDate = this.dates.executionDate.toISOString();
    } else {
      this.challenge.executionDate = undefined;
    }
    if (this.dates.publishDate) {
      this.challenge.publishDate = this.dates.publishDate.toISOString();
    } else {
      this.challenge.publishDate = undefined;
    }
  }

  canEdit() {
    return this.editPermission && this.challenge && !this.challenge.closed;
  }

  canRegister() {
    return this.registerPermission && this.challenge && !this.challenge.closed;
  }

  onSelect(event) {
    this.router.navigate(['/challenges', this.challenge.id, 'edit', this.selectedTask.id]);
  }

  saveChallenge() {
    this.error = undefined;
    this.updateChallengeFromDates();
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

  showMessage(err) {
    this.infoMessage = err;
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

  closeChallenge() {
    this.confirmationService.confirm({
      message: 'Closing a challenge cannot be rolled back! Are you sure?',
      header: 'Close Confirmation',
      icon: 'fa fa-archive',
      accept: () => {
        this.closeIsActive = true;
        this.bs.closeChallenge(this.challenge.id).subscribe(data => {
          this.closeIsActive = false;
          this.challenge.closed = true;
          this.showMessage(data.message);
        }, err => {
          this.closeIsActive = false;
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

  showExperiments() {
    this.router.navigate(['challenges', this.challenge.id, 'experiments']);
  }
}
