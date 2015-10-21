import com.scalakata._

import scodec.{bits => _, _}
import scodec.codecs._
import scodec.bits._

@instrument class DnsResponse {  
  case class IPV4(a: Int, b: Int, c: Int, d: Int)
  case class Response(transactionID: Int, name: List[String], address: IPV4)

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
  def ipv4 = (uint8 :: uint8 :: uint8 :: uint8).as[IPV4]
  def flags = (
    ("Response"               | constant(bin"1"))     :: 
    ("Opcode"                 | ignore(4))            ::
    ("Authorative"            | ignore(1))            ::
    ("Truncated"              | ignore(1))            ::
    ("Recursion desired"      | ignore(1))            ::
    ("Recursion available"    | ignore(1))            ::
    ("Reserved"               | ignore(1))            ::
    ("Answer authenticated"   | ignore(1))            :: 
    ("Non-authenticated data" | ignore(1))            ::
    ("Reply code"             | ignore(4))
  ).dropUnits
  
  def part1 = (
    ("Transaction ID"         | uint16)               ::
    flags :::
    ("Questions"              | ignore(16))           ::
    ("Answer RRs"             | ignore(16))           ::
    ("Authority RRs"          | ignore(16))           ::
    ("Additional RRs"         | ignore(16))           ::
    ("Name"                   | dnsString)            ::
    ("Type"                   | constant(hex"00 01")) ::
    ("Class"                  | constant(hex"00 01")) ::
    ("Name"                   | constant(hex"c0 0c"))
  ).dropUnits
    
  def responseCodec = (
    part1 :::
    ("Type"                   | constant(hex"00 01")) ::
    ("Class"                  | constant(hex"00 01")) ::
    ("TTL"                    | ignore(32))           ::
    ("Data Length"            | ignore(16))           ::
    ("Address"                | ipv4)
  ).dropUnits.as[Response]
  
  val message = (
    hex"75c0"                                   ++ // Transaction ID
    hex"8180"                                   ++ // Flags (Response, ..., Non-authenticated data)
    hex"0001"                                   ++ // Questions: 1
    hex"0001"                                   ++ // Answer RRs: 1
    hex"0000"                                   ++ // Authority RRs: 0
    hex"0000"                                   ++ // Additional RRs: 0
    hex"03 777777 06 6e6574627364 03 6f7267 00" ++ // Name: www.netbsd.org
    hex"0001"                                   ++ // Type: A
    hex"0001"                                   ++ // Class: IN
    hex"c00c"                                   ++ // Name
    hex"0001"                                   ++ // Type: A
    hex"0001"                                   ++ // Class: IN
    hex"000140ef"                               ++ // TTL: 82159
    hex"0004"                                   ++ // Data Length: 4
    hex"cc98be0c"                                  // Address: 204.152.190.12
  ).bits

  responseCodec.decode(message)
  
  def roundTrip[T](v: T, codec: Codec[T]) = codec.encode(v).flatMap(codec.decode).map(_.value)
  
  roundTrip(Response(30144,List("www", "netbsd", "org"),IPV4(204,152,190,12)), responseCodec)
}