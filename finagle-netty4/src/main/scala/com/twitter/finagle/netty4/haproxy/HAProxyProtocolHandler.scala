package com.twitter.finagle.netty4.haproxy

import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.handler.codec.haproxy.HAProxyMessage
import io.netty.util.AttributeKey

private[finagle] object HAProxyProtocolHandler {
  val HandlerName: String = "haproxyHandler"

  val SourceAddressAttribute: AttributeKey[String] = AttributeKey.newInstance("source-address")
  val SourcePortAttribute: AttributeKey[Int] = AttributeKey.newInstance("source-port")
  val DestinationAddressAttribute: AttributeKey[String] = AttributeKey.newInstance("destination-address")
  val DestinationPortAttribute: AttributeKey[Int] = AttributeKey.newInstance("destination-port")
}

@Sharable
private[finagle] class HAProxyProtocolHandler extends ChannelInboundHandlerAdapter {
  import HAProxyProtocolHandler._
  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
    msg match {
      case m: HAProxyMessage =>
        println("------------------------ HA PROXY MESSAGE --------------------------------------", m)

        ctx.channel().attr(SourceAddressAttribute).set(m.sourceAddress())
        ctx.channel().attr(SourcePortAttribute).set(m.sourcePort())
        ctx.channel().attr(DestinationAddressAttribute).set(m.destinationAddress())
        ctx.channel().attr(DestinationPortAttribute).set(m.destinationPort())

        m.release()
        ctx.pipeline().remove(this)
      case _ =>
        ctx.fireChannelRead(msg)
    }
  }
}
