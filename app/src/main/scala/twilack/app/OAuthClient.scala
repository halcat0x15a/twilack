package twilack.app

import com.twitter.finagle.{Http, Service}
import com.twitter.finagle.http
import com.twitter.util.{Await, Future}

import javafx.application.{Application, Platform}
import javafx.concurrent.Worker
import javafx.scene.Scene
import javafx.scene.web.WebView
import javafx.stage.Stage

import twilack.slack.SlackAPI

import twitter4j._

class OAuthClient extends Application {

  def start(stage: Stage): Unit = {
    val consumerToken = "bT4HNS87r4ja9m9SdXIOBzsTL"
    val consumerSecret = "0wIAkacjcqha1POVQSu8Uvwtlb9xnBSYdlGzvHUk3aHPLKCOzW"
    val twitter = new TwitterFactory().getInstance()
    twitter.setOAuthConsumer(consumerToken, consumerSecret)
    val requestToken = twitter.getOAuthRequestToken("http://localhost:8080/")
    val service = new Service[http.Request, http.Response] {
      def apply(req: http.Request): Future[http.Response] = {
        val code = req.getParam("code")
        val verifier = req.getParam("oauth_verifier")
        if (code != null) {
          val response = http.Response(req.version, http.Status.TemporaryRedirect)
          response.location = requestToken.getAuthorizationURL
          Future.value(response)
        } else if (verifier != null) {
          twitter.getOAuthAccessToken(requestToken, verifier)
          Future.value(http.Response(req.version, http.Status.Ok))
        } else {
          Future.value(http.Response(req.version, http.Status.BadRequest))
        }
      }
    }
    val server = Http.serve(":8080", service)
    val webView = new WebView
    val scene = new Scene(webView)
    stage.setScene(scene)
    val clientId = "54269703027.54891801366"
    val clientSecret = "77963be250fee467804cbe436c526ee9"
    webView.getEngine.load(SlackAPI.authorize(clientId, "client"))
    webView.getEngine.getLoadWorker.stateProperty.addListener { (observable, oldState, newState) =>
      println(newState)
      if (newState == Worker.State.SUCCEEDED && webView.getEngine.getLocation.contains("oauth_verifier")) {
        Platform.exit()
      }
    }
    stage.show()
  }

}
