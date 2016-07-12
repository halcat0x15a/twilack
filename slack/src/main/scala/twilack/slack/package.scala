package twilack

import org.asynchttpclient.DefaultAsyncHttpClient

package object slack {

  val httpClient: DefaultAsyncHttpClient = new DefaultAsyncHttpClient

}
