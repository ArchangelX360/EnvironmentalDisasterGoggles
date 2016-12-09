import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Http, Response, Headers, RequestOptions } from '@angular/http';
import { InterpretedQuery } from './interpreted-query';
import { MonitoringService } from './monitoring.service';

@Injectable()
export class QuerySenderService {

  constructor(private http: Http, private monitoringService: MonitoringService) {
  }

  send(query: string): Observable<InterpretedQuery> {
    let headers = new Headers({'Content-Type': 'application/json'});
    let options = new RequestOptions({headers: headers});
    let url = 'http://localhost:9000/search';
    let body = {
      "query": query,
      "author": this.monitoringService.getAuthor()
    };

    return this.http.post(url, body, options)
      .map(reponse => <InterpretedQuery> reponse.json())
      .catch(error => Observable.throw(error));
  }

}
