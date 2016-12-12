import { Routes, RouterModule } from '@angular/router';
import { PageNotFoundComponent } from './page-not-found.component';
import { ModuleWithProviders } from '@angular/core';
import { ViewerComponent } from './viewer.component';
import { MonitoringComponent } from './monitoring.component';


const appRoutes: Routes = [
  {path: '', redirectTo: '/viewer', pathMatch: 'full'},
  {path: 'viewer', component: ViewerComponent},
  {path: 'monitoring', component: MonitoringComponent},
  {path: '**', component: PageNotFoundComponent}
];

export const appRoutingProviders: any[] = [];

export const routing: ModuleWithProviders = RouterModule.forRoot(appRoutes);
