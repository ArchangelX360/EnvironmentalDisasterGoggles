import { Component, OnInit, Output, EventEmitter } from '@angular/core';
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
    console.log("[INFO] Query sent: " + this.query);
    this.querySender.send(this.query).subscribe(
      response => this.queryResponse.emit(response),
      error => this.errorHandler(error)
    );
  }

  errorHandler(error: any) {
    let logStr = '[ERROR] [QUERY SENDER SERVICE] ' + error;
    console.log(logStr);
    this.mdSnackBar.open(logStr, 'Close');
  }

}
