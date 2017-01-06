import {Component, OnInit} from "@angular/core";
import {Query} from "../models/query";
import {ResultsService} from '../services/results.service';
import {ActivatedRoute, Params} from "@angular/router";
import { MdSnackBar, MdSlider } from '@angular/material';

@Component({
  selector: 'app-results',
  templateUrl: './results.component.html'
})
export class ResultsComponent implements OnInit {

  private query: Query;
  private imgOpacity: number;

  constructor(private route: ActivatedRoute,
              private resultsService: ResultsService,
              private mdSnackBar: MdSnackBar) {
  }

  ngOnInit() {
    this.route.params
      .map((params: Params) => params['id'])
      .subscribe(id => {
        this.getQueryResult(id);
      })
  }

  /**
   * Get Query in order to display search result
   */
  getQueryResult(queryID: string) {
    this.resultsService.getQuery(queryID).subscribe(
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
