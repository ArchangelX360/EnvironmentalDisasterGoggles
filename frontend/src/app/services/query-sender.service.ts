import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Http, Response, Headers, RequestOptions } from '@angular/http';
import { InterpretedQuery } from '../models/interpreted-query';
import { MonitoringService } from './monitoring.service';

@Injectable()
export class QuerySenderService {

  constructor(private http: Http, private monitoringService: MonitoringService) {
  }

  /**
   * Request NLP interpretation from user search-bar input string
   * @param query the natural language string to interpret
   * @returns {Observable<R>} NLP interpretation of query
   */
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

  /**
   * Request to start processing the user-approved query
   * @param queryID id of the query to process
   */
  sendStart(queryID : string) : Observable<any> {
    let headers = new Headers({'Content-Type': 'application/json'});
    let options = new RequestOptions({headers: headers});
    let url = 'http://localhost:9000/process-start/'+queryID;

    return this.http.post(url, options)
      .map(response => response)
      .catch(error => Observable.throw(error))
  }

}
