export interface InterpretedQuery {
  id: string
  place: string;
  event: string;
  // TODO(archangel): dates needs to be typed
  from: any;
  to: any;
}
