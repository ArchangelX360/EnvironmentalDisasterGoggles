import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Query } from './query';
import { AUTHOR } from './mock-author';
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

  getAuthor(): Observable<string> {
    return Observable.from([AUTHOR]);
  }

}
