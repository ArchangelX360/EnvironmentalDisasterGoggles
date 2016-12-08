import {Component, OnInit, Input, Output, EventEmitter} from '@angular/core';
import { QuerySenderService } from './query-sender.service';
import { ToasterService } from 'angular2-toaster';

@Component({
  selector: 'app-search-bar',
  templateUrl: './search-bar.component.html'
})
export class SearchBarComponent implements OnInit {

  @Output()
  queryResponse = new EventEmitter<any>();

  private query: string;


  constructor(private querySender: QuerySenderService, private toasterService: ToasterService) {
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
    this.toasterService.pop('success', 'Query Service', response);

    this.queryResponse.emit(response);

  }

  errorHandler(error: any) {
    // TODO : generify handler
    console.log('[ERROR] [QUERY SERVICE] ' + error);
    this.toasterService.pop('error', 'Query Service', error);
  }

}
