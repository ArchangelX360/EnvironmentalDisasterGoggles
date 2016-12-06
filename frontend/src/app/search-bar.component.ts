import { Component, OnInit } from '@angular/core';
import { QuerySenderService } from './query-sender.service';

@Component({
  selector: 'app-search-bar',
  templateUrl: './search-bar.component.html'
})
export class SearchBarComponent implements OnInit {

  private query: string;

  constructor(private querySender: QuerySenderService) {
  }

  ngOnInit() {
  }

  sendRequest() {
    console.log("Query sent:" + this.query);
    this.querySender.send(this.query).subscribe(
      // TODO: real handling
      response => console.log(response),
      error => console.log(<any>error)
    );
  }
}
