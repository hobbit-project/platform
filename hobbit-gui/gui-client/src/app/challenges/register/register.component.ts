import { Location } from '@angular/common';
import { Challenge, ChallengeRegistration, System } from './../../model';
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
  private registrations: ChallengeRegistration[];
  private systems: System[];
  private display = {};

  private loadedChallenge = false;
  private loadedRegistrations = false;
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
    this.bs.getAllChallengeRegistrations(id).subscribe(data => {
      this.registrations = data;
      this.loadedRegistrations = true;
      this.updateLoaded();
    });
    this.bs.getSystemProviderSystems().subscribe(data => {
      this.systems = data;
      this.loadedSystems = true;
      this.updateLoaded();
    });
  }

  private updateLoaded() {
    if (this.loadedChallenge && this.loadedSystems && this.loadedRegistrations) {
      for (const system of this.systems) {
        for (const task of this.challenge.tasks) {
          if (!this.display[task.id])
            this.display[task.id] = [];
          this.display[task.id].push({ id: system.id, selected: false });
        }
      }

      for (const reg of this.registrations) {
        if (this.getEntry(this.display[reg.taskId], reg.systemId))
          this.getEntry(this.display[reg.taskId], reg.systemId).selected = true;
      }
      this.loaded = true;
    }
  }

  private getEntry(list, id: string): any {
    for (let i = 0; i < list.length; i++) {
      if (list[i].id === id)
        return list[i];
    }
    return null;
  }

  cancel() {
    this.location.back();
  }

  submit() {
    const taskRegistrations: ChallengeRegistration[] = [];
    for (const task of Object.keys(this.display)) {
      for (const sel of this.display[task]) {
        if (sel.selected)
          taskRegistrations.push(new ChallengeRegistration(this.challenge.id, task, sel.id));
      }
      this.bs.updateChallengeTaskRegistrations(this.challenge.id, task, taskRegistrations).subscribe();
    }
    this.cancel();
  }
}
