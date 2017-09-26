import { EditComponent } from './edit/edit.component';
import { Router } from '@angular/router';
import { Challenge, User, Role } from './../model';
import { BackendService } from './../backend.service';
import { Component, OnInit, ViewEncapsulation } from '@angular/core';

@Component({
  selector: 'app-challenges',
  templateUrl: './challenges.component.html',
  styleUrls: ['./challenges.component.less']
})
export class ChallengesComponent implements OnInit {

  public Role = Role;
  private challenges: Challenge[];
  private user: User;
  private selectedChallenge: Challenge;


  constructor(private bs: BackendService, private router: Router) { }

  ngOnInit() {
    this.bs.userInfo().subscribe(data => {
      this.user = data;
    });

    this.bs.listChallenges().subscribe(data => {
      this.challenges = data;
    });
  }

  addChallenge() {
    this.router.navigate(['/challenges', EditComponent.ADD_CHALLENGE]);
  }

  onSelect(event) {
    this.router.navigate(['/challenges', this.selectedChallenge.id]);
  }
}
