import { Component, OnInit } from '@angular/core';
import { Query } from './query';
import { MonitoringService } from './monitoring.service';
import { MdSnackBar } from '@angular/material';

@Component({
  selector: 'app-monitoring',
  templateUrl: './monitoring.component.html'
})
export class MonitoringComponent implements OnInit {

  private queries: Query[];
  private userToken: string;

  constructor(private monitoringService: MonitoringService,
              private mdSnackBar: MdSnackBar) {
  }

  ngOnInit() {
    this.queries = [];
    this.userToken = '';

    this.monitoringService.getQueries().subscribe(
      response => this.queries = response,
      error => {
        this.errorHandler(error);
      }
    );

    this.monitoringService.getAuthor().subscribe(
      response => this.userToken = response,
      error => {
        this.errorHandler(error)
      }
    );
  }

  errorHandler(error: any) {
    let logStr = '[ERROR] [MONITORING SERVICE] ' + error;
    console.log(logStr);
    this.mdSnackBar.open(logStr, 'Close');
  }

}
