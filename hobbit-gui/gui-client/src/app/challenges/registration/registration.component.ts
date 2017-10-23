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

  public challenge: Challenge;
  private registrations: ChallengeRegistration[];
  private systems: System[];
  private display = {};

  private loadedChallenge = false;
  private loadedRegistrations = false;
  private loadedSystems = false;
  public loaded = false;

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
      for (const reg of this.registrations) {
        if (!(reg.taskId in this.display))
          this.display[reg.taskId] = [];
        this.display[reg.taskId].push({ id: reg.systemId, fixed: true, selected: true });
      }
      this.updateLoaded();
    });
  }

  private updateLoaded() {
    this.loaded = this.loadedChallenge && this.loadedRegistrations;
  }

  cancel() {
    this.router.navigate(['/challenges', this.challenge.id]);
  }

}
