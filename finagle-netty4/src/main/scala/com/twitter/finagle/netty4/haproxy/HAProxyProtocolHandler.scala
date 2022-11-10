package com.twitter.finagle.netty4.haproxy

import com.twitter.util.Try
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.handler.codec.haproxy.HAProxyMessage
import io.netty.util.AttributeKey
import java.net.InetAddress

private[finagle] object HAProxyProtocolHandler {
  val HandlerName: String = "haproxyHandler"

  val SourceAddressAttribute: AttributeKey[InetAddress] = AttributeKey.newInstance("source-address")
  val SourcePortAttribute: AttributeKey[Int] = AttributeKey.newInstance("source-port")
  val DestinationAddressAttribute: AttributeKey[InetAddress] = AttributeKey.newInstance("destination-address")
  val DestinationPortAttribute: AttributeKey[Int] = AttributeKey.newInstance("destination-port")
}

@Sharable
private[finagle] class HAProxyProtocolHandler extends ChannelInboundHandlerAdapter {
  import HAProxyProtocolHandler._

  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
    msg match {
      case m: HAProxyMessage =>
        val sourceAddr = Try(InetAddress.getByName(m.sourceAddress())).getOrElse(null)
        val destinationAddr = Try(InetAddress.getByName(m.destinationAddress())).getOrElse(null)

        ctx.channel().attr(SourceAddressAttribute).set(sourceAddr)
        ctx.channel().attr(SourcePortAttribute).set(m.sourcePort())
        ctx.channel().attr(DestinationAddressAttribute).set(destinationAddr)
        ctx.channel().attr(DestinationPortAttribute).set(m.destinationPort())

        // Remove ourselves from the channel now, as no more work to do.
        ctx.pipeline().remove(this)

        // Release the reference counted object so that it can be returned to the pool.
        m.release()

      // Do not continue propagating the message.
      case _ =>
        ctx.fireChannelRead(msg)
    }
  }
}
