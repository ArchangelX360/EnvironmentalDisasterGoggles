import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';

import { MaterialModule } from '@angular/material';

import { AppComponent } from './app.component';
import { SearchBarComponent } from './search-bar/search-bar.component';
import { QuerySenderService } from './services/query-sender.service';
import { routing, appRoutingProviders } from './app.routing';
import { PageNotFoundComponent } from './page-not-found/page-not-found.component';
import { ViewerComponent } from './monitoring/viewer.component';
import { MonitoringComponent } from './monitoring/monitoring.component';
import { MonitoringService } from './services/monitoring.service';
import { ResultsComponent } from './results/results.component';
import { ResultsService } from './services/results.service';
import { AuthorQueriesPipe } from './pipes/author-queries.pipe';
import { QueryComponent } from './monitoring/query.component';
import { TaskComponent } from './monitoring/task.component';
import {SearchResultDialog} from "./search-bar/search-result.component";

@NgModule({
  declarations: [
    AppComponent,
    SearchBarComponent,
    SearchResultDialog,
    PageNotFoundComponent,
    ViewerComponent,
    MonitoringComponent,
    ResultsComponent,
    AuthorQueriesPipe,
    QueryComponent,
    TaskComponent
  ],
  imports: [
    BrowserModule,
    FormsModule,
    HttpModule,
    routing,
    MaterialModule.forRoot(),
  ],
  providers: [
    QuerySenderService,
    appRoutingProviders,
    MonitoringService,
    ResultsService
  ],
  entryComponents: [
    SearchResultDialog
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
}
