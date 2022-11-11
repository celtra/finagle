package com.twitter.finagle.netty4

import io.netty.handler.codec.haproxy.{
  HAProxyCommand,
  HAProxyMessage,
  HAProxyProtocolVersion,
  HAProxyProxiedProtocol
}

package object haproxy {
  def defaultHAProxyMessage(
    version: HAProxyProtocolVersion = HAProxyProtocolVersion.V1,
    command: HAProxyCommand = HAProxyCommand.PROXY,
    protocol: HAProxyProxiedProtocol = HAProxyProxiedProtocol.TCP4,
    sourceAddr: String = "1.1.1.1",
    destAddr: String = "2.2.2.2",
    sourcePort: Int = 1024,
    destPort: Int = 8080
  ): HAProxyMessage = {
    new HAProxyMessage(version, command, protocol, sourceAddr, destAddr, sourcePort, destPort)
  }
}
