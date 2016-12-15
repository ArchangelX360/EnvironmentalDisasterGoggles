import { Pipe, PipeTransform } from '@angular/core';
import { Query } from '../models/query';

@Pipe({
  name: 'authorQueries'
})
export class AuthorQueriesPipe implements PipeTransform {

  transform(queries: Query[]): Query[] {
    return queries.filter(query => query.author === localStorage.getItem('authorID'));
  }

}
