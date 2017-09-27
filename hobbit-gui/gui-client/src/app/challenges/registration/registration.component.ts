import { Challenge, ChallengeRegistration, System } from './../../model';
import { BackendService } from './../../backend.service';
import { ActivatedRoute, Router } from '@angular/router';
import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-registration',
  templateUrl: './registration.component.html',
  styleUrls: ['./registration.component.less']
})
export class RegistrationComponent implements OnInit {

  private challenge: Challenge;
  private registrations: ChallengeRegistration[];
  private systems: System[];
  private display = {};

  private loadedChallenge = false;
  private loadedRegistrations = false;
  private loadedSystems = false;
  private loaded = false;

  constructor(private activatedRoute: ActivatedRoute, private bs: BackendService, private router: Router) { }

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
      this.updateSelectedSystems();
      this.updateLoaded();
    });
    this.bs.getSystemProviderSystems().subscribe(data => {
      this.systems = data;
      this.loadedSystems = true;
      this.updateSelectedSystems();
      this.updateLoaded();
    });
  }

  private updateLoaded() {
    this.loaded = this.loadedChallenge && this.loadedSystems && this.loadedRegistrations;
  }

  private updateSelectedSystems() {
    if (this.loadedRegistrations && this.loadedSystems) {
      const map = {};

      for (const system of this.systems) {
        map[system.id] = system;

        for (const task of this.challenge.tasks) {
          if (!this.display[task.id])
            this.display[task.id] = [];
          this.display[task.id].push({ id: system.id, fixed: false, selected: false });
        }
      }

      for (const reg of this.registrations) {
        if (!this.getEntry(this.display[reg.taskId], reg.systemId))
          this.display[reg.taskId].push({ id: reg.systemId, fixed: true, selected: true });
        else
          this.getEntry(this.display[reg.taskId], reg.systemId).selected = true;
      }
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
    this.router.navigate(['/challenges', this.challenge.id]);
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
