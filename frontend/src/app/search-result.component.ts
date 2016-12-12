import {Component, Input} from "@angular/core";
import {MdDialogRef} from "@angular/material";

@Component({
  selector: 'search-result-dialog',
  templateUrl: './search-result.component.html'
})
export class SearchResultDialog {

  @Input() response: any;

  constructor(public dialogRef: MdDialogRef<SearchResultDialog>) { }

  ngOnInit() {
    console.log(this.response)
  }

}
