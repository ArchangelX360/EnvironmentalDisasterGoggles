package controllers

import play.api.mvc._

/**
  * Created by cassan on 06/12/16.
  */
class Main extends Controller {
  def echo = Action {
    Ok("hello")
  }
}
