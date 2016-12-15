import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Query } from '../models/query';
import { Http } from '@angular/http';

@Injectable()
export class MonitoringService {

  constructor(private http: Http) {
  }

  getQueries(): Observable<Query[]> {
    let url = 'http://localhost:9000/monitoring/queries';

    return this.http.get(url)
      .map((response) => <Query[]> (response.json()))
      .catch((error) => {
        return Observable.throw(error)
      });
  }

}
