import {Component, OnInit} from "@angular/core";
import {Query} from "../models/query";
import {ResultsService} from '../services/results.service';
import {ActivatedRoute, Params} from "@angular/router";
import { MdSnackBar } from '@angular/material';

@Component({
  selector: 'app-results',
  templateUrl: './results.component.html'
})
export class ResultsComponent implements OnInit {

  private query: Query;
  private queryID: string;

  constructor(private route: ActivatedRoute,
              private resultsService: ResultsService,
              private mdSnackBar: MdSnackBar) {
  }

  ngOnInit() {
    this.route.params
      .map((params: Params) => params['id'])
      .subscribe(id => {
        this.queryID = id;
        this.getQueryResult();
      })
  }

  /**
   * Get Query in order to display search result
   */
  getQueryResult() {
    this.resultsService.getQuery(this.queryID).subscribe(
      response => this.query = response,
      error => this.errorHandler(error)
    );
  }

  errorHandler(error: any) {
    let logStr = '[ERROR] [RESULTS SERVICE] ' + error;
    console.log(logStr);
    this.mdSnackBar.open(logStr, 'Close');
  }

}
