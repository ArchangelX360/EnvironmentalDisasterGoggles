import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Http, Response } from '@angular/http';

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
    // TODO: change API URL to match the actual one
    let url = 'localhost:9000/search?query='+ encodeURI(query);
    return this.http.get(url)
      .map(QuerySenderService.responseParser)
      .catch(QuerySenderService.errorHandler);
  }

}