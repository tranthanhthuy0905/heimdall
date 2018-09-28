package controllers

import javax.inject._
import play.api.libs.json.Json
import play.api.mvc._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 * TODO [okroshkina]: deleteme
 */
@Singleton
class HealthController @Inject()(cc: ControllerComponents)(implicit assetsFinder: AssetsFinder)
  extends AbstractController(cc) {

  def isItUp = Action {
    Ok(Json.obj("status" -> "ok"))
  }

}
