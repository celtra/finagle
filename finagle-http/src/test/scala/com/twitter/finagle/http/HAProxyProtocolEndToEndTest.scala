package com.twitter.finagle.http

import com.twitter.finagle.{Http, ListeningServer, Service}
import com.twitter.util.{Await, Future}
import com.twitter.conversions.DurationOps._
import org.scalatest.funspec.AnyFunSpec
import java.net.InetSocketAddress

class HAProxyProtocolEndToEndTest extends AnyFunSpec {
  describe("with HA proxy disabled") {
    it("does not contain client connection information") {
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

  describe("with HA proxy enabled") {
    it("does not contain client connection information if HA proxy message not passed") {
      def validate: Request => Unit = req => {
        assert(req.clientSourceAddress.isEmpty)
        assert(req.clientDestinationPort.isEmpty)
      }

      val server = httpServer(true, serverService(validate))
      val client = clientService(server)
      val response = Await.result(client(Request()), 1.second)

      assert(response.status == Status.Ok)
    }

    it("contains client connection information") {
      val serverPort = 8080

      def validate: Request => Unit = req => {
        assert(req.clientSourceAddress.map(_.getHostAddress).contains("127.0.0.0"))
        assert(req.clientDestinationPort.contains(serverPort))
      }

      val server = httpServer(true, serverService(validate), serverPort)

      // TODO: CLIENT SEND HA PROXY MESSAGE

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
