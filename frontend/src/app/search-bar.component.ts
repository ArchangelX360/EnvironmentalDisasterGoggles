import {Component, OnInit, Input, Output, EventEmitter} from '@angular/core';
import { QuerySenderService } from './query-sender.service';
import { MdSnackBar } from '@angular/material';

@Component({
  selector: 'app-search-bar',
  templateUrl: './search-bar.component.html'
})
export class SearchBarComponent implements OnInit {

  @Output()
  queryResponse = new EventEmitter<any>();

  private query: string;


  constructor(private querySender: QuerySenderService,
              private mdSnackBar: MdSnackBar) {
  }

  ngOnInit() {
  }

  sendRequest() {
    console.log("Query sent:" + this.query);
    this.querySender.send(this.query).subscribe(
      // TODO: real handling
      response => this.responseHandler(response),
      error => this.errorHandler(error)
    );
  }

  responseHandler(response: any) {
    // TODO : generify handler
    console.log('[SUCCESS] [QUERY SERVICE] ' + response);
    this.mdSnackBar.open('[QUERY SERVICE] ' + response, 'Close');

    this.queryResponse.emit(response);

  }

  errorHandler(error: any) {
    // TODO : generify handler
    console.log('[ERROR] [QUERY SERVICE] ' + error);
    this.mdSnackBar.open('[QUERY SERVICE] ' + error, 'Close');
  }

}
