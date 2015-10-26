package dns

import scodec.{bits => _, _}
import scodec.codecs._
import scodec.bits._

trait DnsString {
  def dnsString = new Codec[List[String]] {

    def sizeBound = SizeBound.unknown

    override def encode(as: List[String]) = as.foldLeft(Attempt.successful(BitVector.empty)) { (acc, v) =>
      for {
        cur <- acc
        a   <- uint8.encode(v.length)
        b   <- fixedSizeBytes(v.length.toLong, utf8).encode(v)
      } yield cur ++ a ++ b
    }.map(_ ++ BitVector.lowByte)

    override def decode(bits0: BitVector) = {
      def go(acc: List[String], bits: BitVector): Attempt[DecodeResult[List[String]]] = {
        uint8.decode(bits) match {
          case Attempt.Successful(DecodeResult(v, rem)) =>
            if(v == 0) Attempt.Successful(DecodeResult(acc.reverse, rem))
            else fixedSizeBytes(v.toLong, utf8).decode(rem) match {
              case Attempt.Successful(DecodeResult(vs, rem2)) => go(vs :: acc, rem2)
              case f: Attempt.Failure => f
            }
          case f: Attempt.Failure => f
        }
      }
      go(Nil, bits0)
    }
  }
  def roundTrip[T](v: T, codec: Codec[T]) = codec.encode(v).flatMap(codec.decode).map(_.value)  
  
  roundTrip(List("www", "netbsd", "org"), dnsString)
}