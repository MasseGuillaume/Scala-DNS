import scodec.{bits => _, _}
import scodec.codecs._
import scodec.bits._
import scalaz.stream._
import scalaz.concurrent.Task
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch

object DnsServer extends DnsRequest with DnsResponse {

  val port = 7890
  dnsString.encode(List("www", "example", "org")).map{ msg =>
    val addr = new InetSocketAddress("127.0.0.1", port)
    val latch = new CountDownLatch(1)
    val maxSize = 1024

    val client = udp.listen(port + 1) {
      udp.eval_(Task.delay { latch.await }) ++
      udp.sends_(to = addr, Process.emit(msg.bytes)) ++
      udp.receive(maxSize)
    }
    val server = udp.listen(port) {
      udp.eval_(Task.delay { latch.countDown }) ++
      udp.receive(maxSize).map{packet => 
        dnsString.decode(packet.bytes.bits).fold(
          _ => Task.now(()),
          request => request.value
        )
        packet.origin
      }
    }
    server.merge(client).runLog.run
  }
}