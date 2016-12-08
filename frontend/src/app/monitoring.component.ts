import { Component, OnInit } from '@angular/core';
import { Query } from './query';
import { MonitoringService } from './monitoring.service';

@Component({
  selector: 'app-monitoring',
  templateUrl: './monitoring.component.html'
})
export class MonitoringComponent implements OnInit {

  private queries: Query[];
  private userToken: string;

  constructor(private monitoringService: MonitoringService) {
  }

  ngOnInit() {
    this.monitoringService.getQueries().subscribe(
      response => this.queries = response,
      error => console.log(error)
    );
    this.monitoringService.getAuthor().subscribe(
      response => this.userToken = response,
      error => console.log(error)
    );
  }

}