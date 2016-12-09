import { Pipe, PipeTransform } from '@angular/core';
import { Query } from './query';

@Pipe({
  name: 'authorQueries'
})
export class AuthorQueriesPipe implements PipeTransform {

  transform(queries: Query[], author: string): Query[] {
    return queries.filter(query => query.author === author);
  }

}
