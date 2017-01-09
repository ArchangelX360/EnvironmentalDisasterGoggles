import { Routes, RouterModule } from '@angular/router';
import { PageNotFoundComponent } from './page-not-found/page-not-found.component';
import { ModuleWithProviders } from '@angular/core';
import { ViewerComponent } from './monitoring/viewer.component';
import { MonitoringComponent } from './monitoring/monitoring.component';
import { ResultsComponent } from './results/results.component';

const appRoutes: Routes = [
  {path: '', redirectTo: '/viewer', pathMatch: 'full'},
  {path: 'viewer', component: ViewerComponent},
  {path: 'monitoring', component: MonitoringComponent},
  {path: 'requests/:id', component: ResultsComponent},
  {path: '**', component: PageNotFoundComponent}
];

export const appRoutingProviders: any[] = [];

export const routing: ModuleWithProviders = RouterModule.forRoot(appRoutes, {
  useHash: true
});
