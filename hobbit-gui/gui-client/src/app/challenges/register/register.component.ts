import { Observable } from 'rxjs/Observable';
import { Location } from '@angular/common';
import { Challenge, ExtendedChallengeRegistration, ChallengeRegistration } from './../../model';
import { BackendService } from './../../backend.service';
import { ActivatedRoute } from '@angular/router';
import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.less']
})
export class RegisterComponent implements OnInit {

  public challenge: Challenge;
  private registrations: ExtendedChallengeRegistration[];
  private display: { [id: string]: ExtendedChallengeRegistration[]; } = {};

  private loadedChallenge = false;
  private loadedSystems = false;
  public loaded = false;

  constructor(private activatedRoute: ActivatedRoute, private bs: BackendService, private location: Location) { }

  ngOnInit() {
    const id = this.activatedRoute.snapshot.params['id'];
    this.bs.getChallenge(id).subscribe(data => {
      this.challenge = data;
      this.loadedChallenge = true;
      this.updateLoaded();
    });
    this.bs.getChallengeRegistrations(id).subscribe(data => {
      this.registrations = data;
      this.loadedSystems = true;
      this.updateLoaded();
    });
  }

  private updateLoaded() {
    if (this.loadedChallenge && this.loadedSystems) {
      for (const task of this.challenge.tasks) {
        if (!this.display[task.id])
          this.display[task.id] = [];

        for (const reg of this.registrations) {
          if (reg.taskId === task.id)
            this.display[task.id].push(reg);
        }
      }
      this.loaded = true;
    }
  }

  cancel() {
    this.location.back();
  }

  submit() {
    const batch = [];
    for (const task of Object.keys(this.display)) {
      const taskRegistrations: ChallengeRegistration[] = [];
      for (const reg of this.display[task]) {
        if (reg.registered)
          taskRegistrations.push(new ChallengeRegistration(this.challenge.id, task, reg.systemId));
      }
      batch.push(this.bs.updateChallengeTaskRegistrations(this.challenge.id, task, taskRegistrations));
    }
    Observable.forkJoin(batch).subscribe(res => this.cancel);
  }
}
