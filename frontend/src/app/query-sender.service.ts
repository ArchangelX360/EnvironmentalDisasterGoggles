import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Http, Response, Headers, RequestOptions } from '@angular/http';

@Injectable()
export class QuerySenderService {

  static responseParser(response: Response) {
    return response.json();
  }

  static errorHandler(error: any) {
    return error;
  }

  constructor(private http: Http) { }

  send(query: string): Observable<any> {
    let headers = new Headers({'Content-Type': 'application/json'});
    let options = new RequestOptions({headers: headers});
    let url = 'http://localhost:9000/search';

    return this.http.post(url, {query}, options)
      .map(QuerySenderService.responseParser)
      .catch(QuerySenderService.errorHandler);
  }

}