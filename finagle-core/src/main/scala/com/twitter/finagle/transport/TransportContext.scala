package com.twitter.finagle.transport

import com.twitter.finagle.ssl.session.{NullSslSessionInfo, SslSessionInfo}
import com.twitter.util.Updatable
import java.net.{InetAddress, SocketAddress}

/**
 * Exposes a way to control the transport, and read off properties from the
 * transport.
 */
abstract class TransportContext {

  /**
   * The locally bound address of this transport.
   */
  def localAddress: SocketAddress

  /**
   * The remote address to which the transport is connected.
   */
  def remoteAddress: SocketAddress

  /**
   * SSL/TLS session information associated with the transport.
   *
   * @note If SSL/TLS is not being used a `NullSslSessionInfo` will be returned instead.
   */
  def sslSessionInfo: SslSessionInfo

  /**
   * Initial, typically client, source address of this transport.
   *
   * Typically HTTP server is placed behind load balancer and initial transport protocol
   * information is therefore lost.
   *
   * @note Property is fulfilled only if HAProxyProtocol message is received through channel.
   */
  def clientSourceAddress: Option[InetAddress] = None

  /**
   * Initial, typically client, source port of this transport.
   *
   * @see For more information check [[clientSourceAddress]]
   */
  def clientSourcePort: Option[Int] = None

  /**
   * Initial, typically client, destination address of this transport.
   *
   * @see For more information check [[clientSourceAddress]]
   */
  def clientDestinationAddress: Option[InetAddress] = None

  /**
   * Initial, typically client, destination port of this transport.
   *
   * @see For more information check [[clientSourceAddress]]
   */
  def clientDestinationPort: Option[Int] = None
}

private[finagle] class SimpleTransportContext(
  val localAddress: SocketAddress = new SocketAddress {},
  val remoteAddress: SocketAddress = new SocketAddress {},
  val sslSessionInfo: SslSessionInfo = NullSslSessionInfo)
    extends TransportContext

private[finagle] class UpdatableContext(first: TransportContext)
    extends TransportContext
    with Updatable[TransportContext] {
  @volatile private[this] var underlying: TransportContext = first

  def update(context: TransportContext): Unit = {
    underlying = context
  }

  def localAddress: SocketAddress = underlying.localAddress
  def remoteAddress: SocketAddress = underlying.remoteAddress
  def sslSessionInfo: SslSessionInfo = underlying.sslSessionInfo
  override def clientSourceAddress: Option[InetAddress] = underlying.clientSourceAddress
  override def clientSourcePort: Option[Int] = underlying.clientSourcePort
  override def clientDestinationAddress: Option[InetAddress] = underlying.clientDestinationAddress
  override def clientDestinationPort: Option[Int] = underlying.clientDestinationPort
}
