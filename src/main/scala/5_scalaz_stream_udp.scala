import scodec.bits._
import scalaz.stream._
import scalaz.concurrent.Task
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch

trait ScalazUdp {
  val port = 7890  
  val msg = hex"deadbeaf"
  val addr = new InetSocketAddress("127.0.0.1", port)
  val latch = new CountDownLatch(1)

  val client: Process[Task,Nothing] = udp.listen(port+1) {
    udp.eval_(Task.delay { latch.await }) ++
    udp.sends_(to = addr, Process.emit(msg))
  }
  val server: Process[Task,ByteVector] = udp.listen(port) {
    udp.eval_(Task.delay { latch.countDown }) ++
    udp.receive(1024).map(_.bytes)
  }
  server.merge(client).runLog.run.toSet
}