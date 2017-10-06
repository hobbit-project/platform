import { MessageService } from 'primeng/components/common/messageservice';
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
  public challenges: Challenge[];
  public user: User;
  public selectedChallenge: Challenge;

  constructor(private bs: BackendService, private router: Router, private messageService: MessageService) { }

  ngOnInit() {
    this.bs.userInfo().subscribe(data => {
      this.user = data;
    });

    this.bs.listChallenges().subscribe(data => {
      this.challenges = data;

      if (this.challenges.length === 0)
        this.messageService.add({ severity: 'warn', summary: 'No Challenges', detail: 'Did not find any challenges.' });
    });
  }

  addChallenge() {
    this.router.navigate(['/challenges', EditComponent.ADD_CHALLENGE]);
  }

  onSelect(event) {
    this.router.navigate(['/challenges', this.selectedChallenge.id]);
  }
}
