import { Pipe, PipeTransform } from '@angular/core';
import { Query } from './query';

@Pipe({
  name: 'ownQueries'
})

export class OwnQueriesPipe implements PipeTransform {
  transform(queries: Query[], author: string): Query[] {
    return queries.filter(query => query.author === author);
  }
}