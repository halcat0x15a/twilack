package twilack.slack

import akka.actor.{Actor, ActorRef, ActorRefFactory, Props}

import io.netty.bootstrap.Bootstrap
import io.netty.channel.{Channel, ChannelInitializer, EventLoopGroup}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.{DefaultHttpHeaders, HttpClientCodec, HttpObjectAggregator}
import io.netty.handler.codec.http.websocketx.{CloseWebSocketFrame, TextWebSocketFrame, WebSocketClientHandshakerFactory, WebSocketVersion}
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory

import java.net.URI

class WebSocketClient(channel: Channel, group: EventLoopGroup) extends Actor {

  def receive = {
    case Message.Text(value) =>
      channel.writeAndFlush(new TextWebSocketFrame(value))
    case Message.Close =>
      channel.writeAndFlush(new CloseWebSocketFrame)
      channel.closeFuture().sync()
      group.shutdownGracefully()
      context.stop(self)
  }

}

object WebSocketClient {

  def connect(actor: ActorRef, uri: URI)(implicit factory: ActorRefFactory): ActorRef = {
    val host = uri.getHost
    val port = uri.getPort
    val sslContext = SslContextBuilder.forClient.trustManager(InsecureTrustManagerFactory.INSTANCE).build
    val group = new NioEventLoopGroup
    try {
      val handler = new WebSocketClientHandler(actor, WebSocketClientHandshakerFactory.newHandshaker(uri, WebSocketVersion.V13, null, false, new DefaultHttpHeaders))
      val bootstrap = new Bootstrap()
        .group(group)
        .channel(classOf[NioSocketChannel])
        .handler(new ChannelInitializer[SocketChannel] {
          def initChannel(channel: SocketChannel): Unit = {
            val pipeline = channel.pipeline()
            pipeline.addLast(sslContext.newHandler(channel.alloc(), host, port))
            pipeline.addLast(
              new HttpClientCodec,
              new HttpObjectAggregator(8192),
              WebSocketClientCompressionHandler.INSTANCE,
              handler
            )
          }
        })
      val channel = bootstrap.connect(host, port).sync().channel()
      handler.sync()
      factory.actorOf(Props(classOf[WebSocketClient], channel, group))
    } catch {
      case e: Throwable =>
        group.shutdownGracefully()
        throw e
    }
  }

}
