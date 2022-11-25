package com.twitter.finagle.netty4.haproxy

import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.haproxy.HAProxyMessage
import io.netty.util.CharsetUtil
import org.scalatest.funsuite.AnyFunSuite

class HAProxyProtocolHandlerTest extends AnyFunSuite {

  test("Saves HA proxy protocol properties to channel attributes") {
    val channel = new EmbeddedChannel(new HAProxyProtocolHandler())
    val msg = defaultHAProxyMessage()
    channel.writeInbound(msg)
    channel.flushInbound()

    // Message should not be propagated down the channel
    assert(channel.inboundMessages.size() == 0)

    assert(channel.attr(HAProxyProtocolHandler.SourceAddressAttribute).get().getHostAddress == "1.1.1.1")
    assert(channel.attr(HAProxyProtocolHandler.SourcePortAttribute).get() == 1024)
    assert(channel.attr(HAProxyProtocolHandler.DestinationAddressAttribute).get().getHostAddress == "2.2.2.2")
    assert(channel.attr(HAProxyProtocolHandler.DestinationPortAttribute).get() == 8080)
  }

  test("Propagates message down the channel if not HA proxy message") {
    val channel = new EmbeddedChannel(new HAProxyProtocolHandler())
    val msg = Unpooled.buffer().writeBytes("hello".getBytes(CharsetUtil.UTF_8))

    assert(channel.writeInbound(msg))
    assert(channel.inboundMessages.size() == 1)
    assert(!channel.inboundMessages.peek().isInstanceOf[HAProxyMessage])

    // Channel attributes should not be set
    Seq(
      HAProxyProtocolHandler.SourceAddressAttribute,
      HAProxyProtocolHandler.SourcePortAttribute,
      HAProxyProtocolHandler.DestinationAddressAttribute,
      HAProxyProtocolHandler.DestinationPortAttribute
    ).foreach { attr =>
      assert(channel.attr(attr).get() == null)
    }
  }
}
