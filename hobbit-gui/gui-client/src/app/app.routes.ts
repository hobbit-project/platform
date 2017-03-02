import { Routes } from '@angular/router';

import { HomeComponent } from './home.comp';
import { UploadBenchmarkComponent } from './upload-benchmark.comp';
import { UploadSystemComponent } from './upload-system.comp';
import { BenchmarkSubmitComponent } from './benchmark-submit.comp';
import { BenchmarkSubmitResponseComponent } from './benchmark-submit-response.comp';
import { BenchmarkStatusComponent } from './benchmark-status.comp';
import { BenchmarkResultDisplayComponent } from './benchmark-result-display.comp';
import { SubmissionDetailsComponent } from './submission-details.comp';
import { ChallengesListComponent } from './challenges-list.comp';
import { ChallengeEditComponent } from './challenge-edit.comp';
import { ChallengeTaskEditComponent } from './challenge-task-edit.comp';
import { ExperimentsComponent } from './experiments.comp';
import { ExperimentsDetailsWrapperComponent } from './experiments-details-wrapper.comp';
import { ChallengeRegisterSystemsComponent } from './challenge-register-systems.comp';
import { ChallengeShowRegistrationsComponent } from './challenge-show-registrations.comp';
import { ChallengeTasksExperimentsComponent } from './challenge-tasks-experiments.comp';

export const rootRouterConfig: Routes = [
  {path: '', redirectTo: 'home', pathMatch: 'full'},
  {path: 'home', component: HomeComponent},
  {path: 'upload/benchmarks', component: UploadBenchmarkComponent},
  {path: 'upload/systems', component: UploadSystemComponent},
  {path: 'benchmarks/submit', component: BenchmarkSubmitComponent},
  {path: 'benchmarks/submitted', component: BenchmarkSubmitResponseComponent},
  {path: 'benchmarks/validate', component: BenchmarkStatusComponent},
  {path: 'benchmarks/display', component: BenchmarkResultDisplayComponent},
  {path: 'submissions/:id', component: SubmissionDetailsComponent},
  {path: 'challenges', component: ChallengesListComponent},
  {path: 'challenges/:id', component: ChallengeEditComponent},
  {path: 'challenges/:id/edit/:task', component: ChallengeTaskEditComponent},
  {path: 'challenges/:id/experiments', component: ChallengeTasksExperimentsComponent},
  {path: 'experiments', component: ExperimentsComponent},
  {path: 'experiments/details', component: ExperimentsDetailsWrapperComponent},
  {path: 'challenges/:id/register', component: ChallengeRegisterSystemsComponent},
  {path: 'challenges/:id/registrations', component: ChallengeShowRegistrationsComponent},
];

