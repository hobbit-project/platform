import { Challenge, User, Role, ChallengeTask } from './../../model';
import { BackendService } from './../../backend.service';
import { ActivatedRoute, ParamMap, Router } from '@angular/router';
import { Component, OnInit } from '@angular/core';

class ChallengeDates {
  private executionDate: Date;
  private publishDate: Date;

  constructor(private challenge: Challenge) {
    this.executionDate = this.asDate(challenge.executionDate);
    this.publishDate = this.asDate(challenge.publishDate);
  }

  public updateChallenge(challenge: Challenge): Challenge {
    challenge.executionDate = this.asString(this.executionDate);
    challenge.publishDate = this.asString(this.publishDate);
    return challenge;
  }

  private asDate(input: string) {
    if (input)
      return new Date(input);
    return undefined;
  }

  private asString(input: Date) {
    if (input)
      return input.toISOString();
    return undefined;
  }
}

@Component({
  selector: 'app-edit',
  templateUrl: './edit.component.html'
})
export class EditComponent implements OnInit {

  public static readonly ADD_CHALLENGE = 'add-challenge';

  private challenge: Challenge;
  private userInfo: User;
  private dates: ChallengeDates;
  private selectedTask: ChallengeTask;

  constructor(private bs: BackendService, private route: ActivatedRoute, private router: Router) { }

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');

    this.bs.userInfo().subscribe(data => {
      this.userInfo = data;
    });

    if (id === EditComponent.ADD_CHALLENGE) {
      this.challenge = new Challenge('', '');
      this.dates = new ChallengeDates(this.challenge);
      this.bs.userInfo().subscribe(userInfo => {
        this.challenge.organizer = userInfo.name;
      });
    } else {
      this.bs.getChallenge(id).subscribe(data => {
        this.challenge = data;
        this.dates = new ChallengeDates(this.challenge);
      });
    }
  }

  canEdit() {
    return this.userInfo && this.userInfo.hasRole(Role.CHALLENGE_ORGANISER) && this.challenge && !this.challenge.closed;
  }

  canRegister() {
    return this.userInfo && this.userInfo.hasRole(Role.SYSTEM_PROVIDER) && this.challenge && !this.challenge.closed;
  }

  cancel() {
    this.router.navigate(['/challenges']);
  }

  delete() {
    // TODO implement
  }

  save() {
    this.dates.updateChallenge(this.challenge);
    if (this.challenge.id === '') {
      this.bs.addChallenge(this.challenge).subscribe(ok => {
        this.cancel();
      });
    } else {
      this.bs.updateChallenge(this.challenge).subscribe(ok => {
        this.cancel();
      });
    }
  }

  addTask() {
    this.router.navigate(['challenges', this.challenge.id, 'edit', 'add-task']);
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

  onSelect(event) {
    this.router.navigate(['challenges', this.challenge.id, 'edit', this.selectedTask.id]);
  }

}
