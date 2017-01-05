import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Query } from '../models/query';
import { Http } from '@angular/http';

@Injectable()
export class ResultsService {

  constructor(private http: Http) {
  }

  getQuery(queryID: string): Observable<Query> {
    let url = 'http://localhost:9000/monitoring/queries/'+queryID;

    return this.http.get(url)
      .map((response) => <Query> response.json())
      .catch((error) => {
        return Observable.throw(error)
      });
  }

}
