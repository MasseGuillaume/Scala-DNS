import com.scalakata._

import scodec.{bits => _, _}
import scodec.codecs._
import scodec.bits._

@instrument class Dns {  
  def bit = bool(1)
  case class Request(transactionID: Int, name: List[String])
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
  val requestCodec = (
    ("Transaction ID"         | uint16)               ::
    ("Response"               | constant(bin"0"))     :: 
    ("Opcode"                 | ignore(4))            ::
    ("Reserved"               | ignore(1))            ::
    ("Truncated"              | ignore(1))            ::
    ("Recursion"              | ignore(1))            ::
    ("Reserved"               | ignore(3))            ::
    ("Non-authenticated data" | ignore(1))            ::
    ("Reserved"               | ignore(4))            ::
    ("Questions"              | ignore(16))           ::
    ("Answer RRs"             | ignore(16))           ::
    ("Authority RRs"          | ignore(16))           ::
    ("Additional RRs"         | ignore(16))           ::
    ("Name"                   | dnsString)            ::
    ("Type"                   | constant(hex"00 01")) ::
    ("Class"                  | constant(hex"00 01"))
  ).dropUnits.as[Request]

  val message = hex"""edf8
                      0100
                      0010
                      0000
                      0000
                      0000
                      0961736b7562756e747503636f6d00
                      0001
                      0001""".bits
  
 requestCodec.decode(message)
}