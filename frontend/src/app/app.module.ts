import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';

import { MaterialModule } from '@angular/material';

import { AppComponent } from './app.component';
import { SearchBarComponent } from './search-bar.component';
import { QuerySenderService } from './query-sender.service';
import { ToasterModule } from 'angular2-toaster';
import { routing, appRoutingProviders } from './app.routing';
import { PageNotFoundComponent } from './page-not-found.component';
import { ViewerComponent } from './viewer.component';
import { MonitoringComponent } from './monitoring.component';

@NgModule({
  declarations: [
    AppComponent,
    SearchBarComponent,
    PageNotFoundComponent,
    ViewerComponent,
    MonitoringComponent
  ],
  imports: [
    BrowserModule,
    FormsModule,
    HttpModule,
    routing,
    MaterialModule.forRoot(),
    ToasterModule
  ],
  providers: [QuerySenderService, appRoutingProviders],
  bootstrap: [AppComponent]
})
export class AppModule {
}
