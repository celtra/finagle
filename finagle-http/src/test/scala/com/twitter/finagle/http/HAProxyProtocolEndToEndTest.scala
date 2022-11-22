package com.twitter.finagle.http

import com.twitter.finagle.{Http, ListeningServer, Service}
import com.twitter.util.{Await, Future}
import com.twitter.conversions.DurationOps._
import io.netty.bootstrap.Bootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.buffer.Unpooled
import io.netty.channel.socket.SocketChannel
import io.netty.channel.{ChannelHandlerContext, ChannelInitializer, SimpleChannelInboundHandler}
import io.netty.handler.codec.http.{
  DefaultFullHttpRequest,
  HttpClientCodec,
  HttpMethod,
  HttpObject,
  HttpResponse,
  HttpResponseStatus,
  HttpVersion,
  LastHttpContent
}
import io.netty.handler.codec.haproxy.{
  HAProxyCommand,
  HAProxyMessage,
  HAProxyMessageEncoder,
  HAProxyProtocolVersion,
  HAProxyProxiedProtocol
}
import org.scalatest.funspec.AnyFunSpec
import java.net.InetSocketAddress

class HAProxyProtocolEndToEndTest extends AnyFunSpec {
  describe("When HA proxy disabled") {
    it("request should not contain client connection information") {
      def validate: Request => Unit = req => {
        assert(req.clientSourceAddress.isEmpty)
        assert(req.clientDestinationPort.isEmpty)
      }

      val server = httpServer(false, serverService(validate))
      val client = clientService(server)
      val response = Await.result(client(Request()), 1.second)

      assert(response.status == Status.Ok)
    }
  }

  describe("When HA proxy enabled") {
    it("request should not contain client connection information when HA proxy message not available") {
      def validate: Request => Unit = req => {
        assert(req.clientSourceAddress.isEmpty)
        assert(req.clientDestinationPort.isEmpty)
      }

      val server = httpServer(true, serverService(validate))
      val client = clientService(server)
      val response = Await.result(client(Request()), 1.second)

      assert(response.status == Status.Ok)
    }

    it("request should contain client connection information when HA proxy message available") {
      val serverPort = 8080

      def validate: Request => Unit = req => {
        assert(req.clientSourceAddress.map(_.getHostAddress).contains("127.0.0.0"))
        assert(req.clientDestinationPort.contains(serverPort))
      }

      val server = httpServer(true, serverService(validate), serverPort)

      // Initialize client request containing HA proxy protocol message
      val group = new NioEventLoopGroup()
      val b = new Bootstrap()
      b.group(group)
        .channel(classOf[NioSocketChannel])
        .handler(new ChannelInitializer[SocketChannel] {
          override def initChannel(ch: SocketChannel): Unit = {
            ch.pipeline
              .addLast("haproxyEncoder", HAProxyMessageEncoder.INSTANCE)
              .addLast("httpCodec", new HttpClientCodec())
              .addLast("clientHandler", new SimpleChannelInboundHandler[HttpObject]() {
                override def channelRead0(ctx: ChannelHandlerContext, msg: HttpObject): Unit = {
                  msg match {
                    case resp: HttpResponse => assert(resp.status() == HttpResponseStatus.OK)
                    case _: LastHttpContent => ctx.close()
                  }
                }
              })
          }
        })

      val ch = b.connect(server.boundAddress).sync.channel()

      val message = new HAProxyMessage(
        HAProxyProtocolVersion.V1, HAProxyCommand.PROXY, HAProxyProxiedProtocol.TCP4,
        "127.0.0.0", "127.0.0.1", 1024, serverPort
      )
      val req = new DefaultFullHttpRequest(
        HttpVersion.HTTP_1_1, HttpMethod.GET, s"http://127.0.0.1:${serverPort}/", Unpooled.EMPTY_BUFFER
      )

      ch.writeAndFlush(message).sync()
      ch.writeAndFlush(req).sync()
      ch.closeFuture().sync()
      group.shutdownGracefully()
    }
  }

  private def serverService(validate: Request => Unit): Service[Request, Response] = {
    new Service[Request, Response] {
      def apply(request: Request): Future[Response] = {
        validate(request)
        Future.value(Response())
      }
    }
  }

  private def clientService(server: ListeningServer): Service[Request, Response] = {
    Http.client
      .newService(s":${server.boundAddress.asInstanceOf[InetSocketAddress].getPort}")
  }

  private def httpServer(haProxyEnabled: Boolean, service: Service[Request, Response], port: Int = 0): ListeningServer = {
    Http.server
      .withHAProxyProtocol(haProxyEnabled)
      .serve(new InetSocketAddress(port), service)
  }
}
