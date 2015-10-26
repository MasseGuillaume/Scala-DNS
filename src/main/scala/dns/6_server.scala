package dns

import scodec.{bits => _, _}
import scodec.codecs._
import scodec.bits._
import scalaz.stream._
import scalaz.concurrent.Task
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch

object ClientServer extends DnsRequest with DnsResponse {

  val port = 7890
  val addr = new InetSocketAddress("127.0.0.1", port)
  val latch = new CountDownLatch(1)
  val maxSize = 1024
  
  def asProcess[T](attempt: Attempt[T]): Process0[T] = 
    Process.emit(
      attempt.fold(
        f => throw new Exception(f.messageWithContext),
        v => v       
      )
    )
  
  def dnsLogic(request: Request): Response = {
    println(s"dnsLogic $request")
    val catz = IPV4(104, 131, 51, 57)
    val res = Response(request.transactionID , request.name, catz)
    println(res)
    res
  }
  
  def server(p: Int) = udp.listen(p) {
    udp.eval_(Task.delay { latch.countDown }) ++
    (for {
      packet <- udp.receive(maxSize)
      request <- {
        println(packet.bytes.bits)
        val res = asProcess(requestCodec.decode(packet.bytes.bits)).map(_.value)
        println(res)
        res
      }
      response <- asProcess(responseCodec.encode(dnsLogic(request)))
      _ <- udp.send(to = packet.origin, response.bytes) 
    } yield Nil)
  }
  def client(p: Int) = udp.listen(p + 1) {
    udp.eval_(Task.delay { latch.await }) ++
    (for {
      request <- asProcess(requestCodec.encode(Request(30144, List("www", "netbsd", "org"))))
      _ <- udp.send(to = addr, request.bytes)
      packet <- udp.receive(maxSize)
      response <- asProcess(responseCodec.decode(packet.bytes.bits))
    } yield List(response))
  }
  // server(port).merge(client(port)).runLog.run
}