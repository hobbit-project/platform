import { RegisterComponent } from './challenges/register/register.component';
import { ExperimentsComponent } from './experiments/experiments.component';
import { PathLocationStrategy, LocationChangeListener, LocationStrategy } from '@angular/common';
import { EditComponent } from './challenges/edit/edit.component';
import { CustomHttp } from './custom-http.service';
import { BackendService } from './backend.service';
import { KeycloakService } from './auth/keycloak.service';
import { AuthGuardService } from './auth/auth-guard.service';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NgModule } from '@angular/core';
import { RouterModule, Routes, Router } from '@angular/router';
import { HttpClientModule, HttpClient } from '@angular/common/http';
import { SlimLoadingBarModule, SlimLoadingBarService } from 'ng2-slim-loading-bar';
import { ModalModule } from 'ngx-bootstrap/modal';
import { DataTableModule, CalendarModule, ConfirmationService, ConfirmDialogModule, MessagesModule, TabViewModule } from 'primeng/primeng';
import { MessageService } from 'primeng/components/common/messageservice';
import { AppComponent } from './app.component';
import { NavbarComponent } from './navbar/navbar.component';
import { FooterComponent } from './footer/footer.component';
import { HomeComponent } from './home/home.component';
import { NotFoundComponent } from './not-found/not-found.component';
import { BenchmarkComponent } from './benchmark/benchmark.component';
import { BenchmarkComponent as UploadBenchmarkComponent } from './upload/benchmark/benchmark.component';
import { PageHeaderComponent } from './page-header/page-header.component';
import { SystemComponent } from './upload/system/system.component';
import { ConfigComponent } from './benchmark/config/config.component';
import { DynFormComponent } from './dyn-form/dyn-form.component';
import { ChallengesComponent } from './challenges/challenges.component';
import { EditComponent as ChallengesEditComponent } from './challenges/edit/edit.component';
import { ExperimentsComponent as ChallengeExperimentsComponent } from './challenges/experiments/experiments.component';
import { DetailsComponent as ExperimentDetailsComponent } from './experiments/details/details.component';
import { ReportsComponent } from './reports/reports.component';
import { TaskComponent } from './challenges/task/task.component';
import { RegistrationComponent } from './challenges/registration/registration.component';
import { DetailsWrapperComponent } from './experiments/details-wrapper/details-wrapper.component';
import { StatusComponent } from './benchmark/status/status.component';
import { LeaderboardsComponent } from './challenges/leaderboards/leaderboards.component';
import { LeaderboardDetailsComponent } from './challenges/leaderboards/details/details.component';
import { ViewComponent } from './benchmark/status/view/view.component';
import { RoundProgressModule } from 'angular-svg-round-progressbar';
import { AnalysisComponent } from './analysis/analysis.component';
import { CompareComponent } from './experiments/details/compare/compare.component';
import { PlotComponent } from './experiments/details/plot/plot.component';
import { BackButtonComponent } from './common/back-button.component';

const appRoutes: Routes = [
  { path: '', redirectTo: 'home', pathMatch: 'full' },
  { path: 'home', component: HomeComponent, canActivate: [AuthGuardService] },
  { path: 'upload/benchmarks', component: UploadBenchmarkComponent, canActivate: [AuthGuardService] },
  { path: 'upload/systems', component: SystemComponent, canActivate: [AuthGuardService] },
  { path: 'benchmarks', component: BenchmarkComponent, canActivate: [AuthGuardService] },
  { path: 'benchmarks/status', component: StatusComponent, canActivate: [AuthGuardService] },
  { path: 'challenges', component: ChallengesComponent, canActivate: [AuthGuardService] },
  { path: 'challenges/:id', component: ChallengesEditComponent, canActivate: [AuthGuardService] },
  { path: 'challenges/:id/experiments', component: ChallengeExperimentsComponent, canActivate: [AuthGuardService] },
  { path: 'challenges/:id/experiments/:task', component: ChallengeExperimentsComponent, canActivate: [AuthGuardService] },
  { path: 'challenges/:id/register', component: RegisterComponent, canActivate: [AuthGuardService] },
  { path: 'challenges/:id/registrations', component: RegistrationComponent, canActivate: [AuthGuardService] },
  { path: 'challenges/:id/edit/:task', component: TaskComponent, canActivate: [AuthGuardService] },
  { path: 'challenges/:id/leaderboards', component: LeaderboardsComponent, canActivate: [AuthGuardService] },
  { path: 'challenges/:id/leaderboards/:task', component: LeaderboardsComponent, canActivate: [AuthGuardService] },
  { path: 'experiments', component: ExperimentsComponent, canActivate: [AuthGuardService] },
  { path: 'experiments/:id', component: DetailsWrapperComponent, canActivate: [AuthGuardService] },
  { path: 'experiments/task/:task', component: DetailsWrapperComponent, canActivate: [AuthGuardService] },
  { path: 'reports', component: ReportsComponent, canActivate: [AuthGuardService] },
  { path: '**', component: NotFoundComponent }
];



export const httpProvide = {
  provide: CustomHttp,
  useFactory: httpClientFactory,
  deps: [HttpClient, KeycloakService, SlimLoadingBarService, Router, MessageService]
};
export function httpClientFactory(http: HttpClient, keycloakService: KeycloakService,
  slimLoadingBarService: SlimLoadingBarService, router: Router, messageService: MessageService): CustomHttp {
  return new CustomHttp(http, keycloakService, slimLoadingBarService, router, messageService);
}

// merge initial path and hash (if it looks suitable)
export class MergeLocationStrategy extends PathLocationStrategy {
  onPopState(fn: LocationChangeListener): void {
    const oldURL = this.path(true);
    // merge "#/...", "#<ID>,<ID>..." and "#<UUID>" into path
    const newURL = oldURL.replace(/#(?:\/(.*)|([\d,%C]+)|(\w{8}-\w{4}-\w{4}-\w{4}-\w{12}))$/, '$1$2$3');
    if (newURL !== oldURL) {
      this.replaceState(null, '', newURL, '');
    }
    super.onPopState(fn);
  }
}
export const mergeStrategyProvide = { provide: LocationStrategy, useClass: MergeLocationStrategy };


@NgModule({
  declarations: [
    AppComponent,
    NavbarComponent,
    FooterComponent,
    HomeComponent,
    NotFoundComponent,
    BenchmarkComponent,
    UploadBenchmarkComponent,
    PageHeaderComponent,
    SystemComponent,
    ConfigComponent,
    DynFormComponent,
    ChallengesComponent,
    ChallengesEditComponent,
    ChallengeExperimentsComponent,
    ExperimentsComponent,
    ExperimentDetailsComponent,
    ReportsComponent,
    TaskComponent,
    RegisterComponent,
    RegistrationComponent,
    DetailsWrapperComponent,
    LeaderboardsComponent,
    LeaderboardDetailsComponent,
    StatusComponent,
    ViewComponent,
    AnalysisComponent,
    CompareComponent,
    PlotComponent,
    BackButtonComponent
  ],
  imports: [
    BrowserAnimationsModule,
    RouterModule.forRoot(appRoutes),
    BrowserModule,
    HttpClientModule,
    FormsModule,
    SlimLoadingBarModule.forRoot(),
    ModalModule.forRoot(),
    ReactiveFormsModule,
    DataTableModule,
    CalendarModule,
    ConfirmDialogModule,
    MessagesModule,
    TabViewModule,
    RoundProgressModule
  ],
  providers: [
    AuthGuardService,
    KeycloakService,
    BackendService,
    mergeStrategyProvide,
    httpProvide,
    ConfirmationService,
    MessageService
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
