import scodec._
import scodec.bits._
import codecs._

class ScodecExample {
  case class Point(x: Int, y: Int, z: Int)
  val pointCodec = (int8 :: int8 :: int8).as[Point] 
  pointCodec.encode(Point(-5, 10, 1))
  pointCodec.decode(hex"0xfb0a01".bits)
}