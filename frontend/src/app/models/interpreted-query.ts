export interface InterpretedQuery {
  place: string;
  type: string;
  // TODO(archangel): dates needs to be typed
  from: any;
  to: any;
}
