import { Component, OnInit } from '@angular/core';
import { SearchBarComponent } from './search-bar.component';

@Component({
  selector: 'app-viewer',
  templateUrl: './viewer.component.html',
})
export class ViewerComponent implements OnInit {

  response: any;

  constructor() {
  }

  ngOnInit() {
  }

  queryResponse(response) {
    console.log("Response in viewer : ", response);
    this.response = response;
  }
}
