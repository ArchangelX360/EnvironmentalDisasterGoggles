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
  private intervalID: number;
  private intervalValue: number = 3000;

  constructor(private monitoringService: MonitoringService,
              private mdSnackBar: MdSnackBar) {
  }

  ngOnInit() {
    this.queries = [];
    this.userToken = '';

    // Initialization of Monitoring data
    this.getMonitoringData();

    // Refreshing Monitoring data with {intervalValue} millisecond interval
    this.intervalID = window.setInterval(() => {
      this.getMonitoringData();
    }, this.intervalValue);
  }

  ngOnDestroy() {
    if (this.intervalID) {
      clearInterval(this.intervalID);
    }
  }

  /**
   * Get Queries and Authors in order to display Monitoring data
   */
  getMonitoringData() {
    this.monitoringService.getQueries().subscribe(
      response => this.queries = response,
      error => this.errorHandler(error)
    );
    this.monitoringService.getAuthor().subscribe(
      response => this.userToken = response,
      error => this.errorHandler(error)
    );
  }

  errorHandler(error: any) {
    let logStr = '[ERROR] [MONITORING SERVICE] ' + error;
    console.log(logStr);
    this.mdSnackBar.open(logStr, 'Close');
  }

}
