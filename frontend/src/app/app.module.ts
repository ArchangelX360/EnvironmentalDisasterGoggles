import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';

import { MaterialModule } from '@angular/material';

import { AppComponent } from './app.component';
import { SearchBarComponent } from './search-bar.component';
import { QuerySenderService } from './query-sender.service';
import { routing, appRoutingProviders } from './app.routing';
import { PageNotFoundComponent } from './page-not-found.component';
import { ViewerComponent } from './viewer.component';
import { MonitoringComponent } from './monitoring.component';
import { MonitoringService } from './monitoring.service';
import { AuthorQueriesPipe } from './author-queries.pipe';
import { QueryComponent } from './query.component';
import { TaskComponent } from './task.component';
import {SearchResultDialog} from "./search-result.component";

@NgModule({
  declarations: [
    AppComponent,
    SearchBarComponent,
    SearchResultDialog,
    PageNotFoundComponent,
    ViewerComponent,
    MonitoringComponent,
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
    MonitoringService
  ],
  entryComponents: [
    SearchResultDialog
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
}
