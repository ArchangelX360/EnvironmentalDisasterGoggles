import { Injectable } from '@angular/core';
import { QUERIES } from './mock-queries';
import { Observable } from 'rxjs';
import { Query } from './query';
import { AUTHOR } from './mock-author';


@Injectable()
export class MonitoringService {

  constructor() {
  }

  getQueries(): Observable<Query[]> {
    return Observable.from([QUERIES]);
  }

  getAuthor(): Observable<string> {
    return Observable.from([AUTHOR]);
  }
}