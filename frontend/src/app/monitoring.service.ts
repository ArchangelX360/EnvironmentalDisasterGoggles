import { Injectable } from '@angular/core';
import {QUERIES} from './mock-queries';
import { Observable } from 'rxjs';
import { Query } from './query';


@Injectable()
export class MonitoringService {

    constructor() { }

    getQueries() : Observable<Query[]> {
      return Observable.from([QUERIES]);
    }

}