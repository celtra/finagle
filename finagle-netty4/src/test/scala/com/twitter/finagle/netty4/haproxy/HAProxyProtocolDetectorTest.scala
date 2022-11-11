package com.twitter.finagle.netty4.haproxy

import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.haproxy.HAProxyMessage
import io.netty.util.CharsetUtil
import org.scalatest.funsuite.AnyFunSuite

class HAProxyProtocolDetectorTest extends AnyFunSuite {

  test("Detects HA proxy protocol message") {
    val channel = new EmbeddedChannel(new HAProxyProtocolDetector())
    val msg = defaultHAProxyMessage()
    channel.writeInbound(msg)
    channel.flushInbound()

    assert(channel.inboundMessages().size() == 1)
    assert(channel.readInbound[HAProxyMessage]() == msg)
  }

  test("Propagates message down the channel if not HA proxy message") {
    val channel = new EmbeddedChannel(new HAProxyProtocolDetector())
    val msg = Unpooled.buffer().writeBytes("hello".getBytes(CharsetUtil.UTF_8))
    channel.writeInbound(msg)

    assert(channel.inboundMessages().size() == 1)
    assert(!channel.inboundMessages().peek().isInstanceOf[HAProxyMessage])
    assert(channel.readInbound[ByteBuf]() == msg)
  }
}
