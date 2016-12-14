import { Task } from './task';

export interface QueryDetails {
  place: string;
  event: string;
  from: any;
  to: any;
}

export interface Query {
  id: string;
  name: string;
  author: string;
  status: string;
  details: QueryDetails;
  tasks: Task[];
}
