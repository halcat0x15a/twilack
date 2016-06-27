package twilack.slack

import akka.actor.ActorRef

import io.netty.channel.{ChannelHandlerContext, ChannelPromise, SimpleChannelInboundHandler}
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.websocketx.{CloseWebSocketFrame, TextWebSocketFrame, WebSocketClientHandshaker}

import scala.concurrent.Future

class WebSocketClientHandler(actor: ActorRef, handshaker: WebSocketClientHandshaker) extends SimpleChannelInboundHandler[Any] {

  private var handshakeFuture: ChannelPromise = _

  def sync(): ChannelPromise = handshakeFuture.sync()

  override def handlerAdded(context: ChannelHandlerContext): Unit = {
    handshakeFuture = context.newPromise()
  }

  override def channelActive(context: ChannelHandlerContext): Unit = {
    handshaker.handshake(context.channel())
  }

  override def channelInactive(context: ChannelHandlerContext): Unit = {}

  override def channelRead0(context: ChannelHandlerContext, message: Any): Unit = {
    val channel = context.channel()
    if (!handshaker.isHandshakeComplete) {
      message match {
        case response: FullHttpResponse =>
          handshaker.finishHandshake(channel, response)
          handshakeFuture.setSuccess()
        case _ =>
      }
    } else {
      message match {
        case frame: TextWebSocketFrame =>
          actor ! Message.Text(frame.text)
        case _: CloseWebSocketFrame =>
          channel.close()
          actor ! Message.Close
        case _ =>
      }
    }
  }

  override def exceptionCaught(context: ChannelHandlerContext, cause: Throwable): Unit = {
    if (!handshakeFuture.isDone()) {
      handshakeFuture.setFailure(cause)
    }
    context.close()
  }

}
