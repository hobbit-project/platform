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
  public openChallenges: Challenge[] = [];
  public closedChallenges: Challenge[] = [];
  public user: User;
  public selectedChallenge: Challenge;
  public loaded: Boolean;

  constructor(private bs: BackendService, private router: Router, private messageService: MessageService) { }

  ngOnInit() {
    this.loaded = false;
    this.bs.userInfo().subscribe(data => {
      this.user = data;
    });

    this.bs.listChallenges().subscribe(data => {
      if (data.length === 0)
        this.messageService.add({ severity: 'warn', summary: 'No Challenges', detail: 'Did not find any challenges.' });
      for (let i = 0; i < data.length; i++) {
        if (data[i].closed)
          this.closedChallenges.push(data[i]);
        else
          this.openChallenges.push(data[i]);
      }
      this.loaded = true;
    });
  }

  addChallenge() {
    this.router.navigate(['/challenges', EditComponent.ADD_CHALLENGE]);
  }

  onSelect(event) {
    this.router.navigate(['/challenges', this.selectedChallenge.id]);
  }
}
