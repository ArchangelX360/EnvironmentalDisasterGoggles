import javax.inject.Inject

import play.api.http.DefaultHttpFilters
import play.filters.cors.CORSFilter

/**
  * Inject cors filter into the play http handler, this allow http configuration to be set into application.conf
  */
class Filters @Inject() (corsFilter: CORSFilter) extends DefaultHttpFilters(corsFilter) {


}
