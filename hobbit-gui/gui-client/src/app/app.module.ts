import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { CommonModule, LocationStrategy, PathLocationStrategy, LocationChangeListener } from '@angular/common';
import { XHRBackend, Http, RequestOptions, HttpModule } from '@angular/http';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { DataTableModule, ConfirmDialogModule, ConfirmationService, CalendarModule, TooltipModule, DialogModule } from 'primeng/primeng';
import { DynamicFormsCoreModule } from '@ng2-dynamic-forms/core';
import { DynamicFormsBootstrapUIModule } from './dyn-form/ui-bootstrap.module';

import { rootRouterConfig } from './app.routes';
import { AppComponent } from './app.comp';
import { MenuItemComponent, NavbarComponent } from './navbar.comp';
import { HomeComponent } from './home.comp';
import { UploadBenchmarkComponent } from './upload-benchmark.comp';
import { UploadSystemComponent } from './upload-system.comp';
import { BenchmarkSubmitComponent } from './benchmark-submit.comp';
import { BenchmarkSubmitResponseComponent } from './benchmark-submit-response.comp';
import { BenchmarkStatusComponent } from './benchmark-status.comp';
import { BenchmarkResultDisplayComponent } from './benchmark-result-display.comp';
import { BenchmarkConfigParamsComponent } from './benchmark-configparams.comp';
import { SubmissionDetailsComponent } from './submission-details.comp';
import { ChallengesListComponent } from './challenges-list.comp';
import { ChallengeEditComponent } from './challenge-edit.comp';
import { ChallengeTaskEditComponent } from './challenge-task-edit.comp';
import { ExperimentsComponent } from './experiments.comp';
import { ExperimentsWrapperComponent } from './experiments-wrapper.comp';
import { ExperimentsDetailsComponent } from './experiments-details.comp';
import { ExperimentsDetailsWrapperComponent } from './experiments-details-wrapper.comp';
import { ChallengeRegisterSystemsComponent } from './challenge-register-systems.comp';
import { ChallengeShowRegistrationsComponent } from './challenge-show-registrations.comp';
import { ChallengeTasksExperimentsComponent } from './challenge-tasks-experiments.comp';
import { ChallengeTasksLeaderboardsComponent } from './challenge-tasks-leaderboards.comp';
import { LeaderboardComponent } from './leaderboard.comp';

import { PageHeaderComponent } from './shared/pageHeader.comp';
import { WaitLoadingComponent } from './shared/waitLoading.comp';
import { ShowErrorComponent } from './shared/showError.comp';
import { CheckboxComponent } from './shared/checkbox.comp';

import { BackendService } from './services/backend.service';
import { CustomHttp } from './services/customHttp';
import { KeycloakService } from './services/keycloakService';

const httpProvide = { provide: Http,
        useFactory: (backend: XHRBackend, defaultOptions: RequestOptions, keycloakService: KeycloakService) => new CustomHttp(backend, defaultOptions, keycloakService),
        deps: [XHRBackend, RequestOptions, KeycloakService] };

// merge initial path and hash (if it looks suitable)
class MergeLocationStrategy extends PathLocationStrategy {
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

const mergeStrategyProvide = { provide: LocationStrategy, useClass: MergeLocationStrategy };

@NgModule({
  imports:      [ BrowserModule, CommonModule, FormsModule, ReactiveFormsModule,
                  HttpModule, RouterModule.forRoot(rootRouterConfig),
                  DynamicFormsCoreModule.forRoot(),
                  DynamicFormsBootstrapUIModule, DataTableModule, ConfirmDialogModule, CalendarModule, TooltipModule, DialogModule ],
  providers:    [ BackendService, KeycloakService, httpProvide, mergeStrategyProvide, ConfirmationService ],
  declarations: [ AppComponent, MenuItemComponent, NavbarComponent, HomeComponent,
                  UploadBenchmarkComponent, UploadSystemComponent,
                  PageHeaderComponent, WaitLoadingComponent, ShowErrorComponent, CheckboxComponent,
                  BenchmarkSubmitComponent, BenchmarkSubmitResponseComponent,
                  BenchmarkConfigParamsComponent,
                  BenchmarkStatusComponent, BenchmarkResultDisplayComponent, SubmissionDetailsComponent,
                  ChallengesListComponent, ChallengeEditComponent, ChallengeTaskEditComponent,
                  ExperimentsComponent,
                  ExperimentsWrapperComponent,
                  ExperimentsDetailsComponent,
                  ExperimentsDetailsWrapperComponent,
                  ChallengeRegisterSystemsComponent, ChallengeShowRegistrationsComponent,
                  ChallengeTasksExperimentsComponent,
                  ChallengeTasksLeaderboardsComponent,
                  LeaderboardComponent,
                ],
  bootstrap:    [ AppComponent ]
})
export class AppModule { }
