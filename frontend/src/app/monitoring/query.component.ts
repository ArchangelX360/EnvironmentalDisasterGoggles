import { Component, OnInit, Input } from '@angular/core';
import { Query } from '../models/query';

@Component({
  selector: 'app-query',
  templateUrl: './query.component.html'
})
export class QueryComponent implements OnInit {

  @Input()
  private query: Query;

  constructor() {
  }

  ngOnInit() {
  }

}
