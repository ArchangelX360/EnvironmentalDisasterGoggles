import { Task } from './task';

export interface Query {
  id: string;
  name: string;
  author: string;
  status: string;
  tasks: Task[];
}