import scodec.{bits => _, _}
import scodec.codecs._
import scodec.bits._

trait DnsRequest extends DnsString {
  case class Request(transactionID: Int, name: List[String])

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

  val requestMessage = (
    hex"75c0"                             ++  // Transaction ID
    hex"0100"                             ++  // Flags (Response, ..., Non-authenticated data)
    hex"0001"                             ++  // Questions
    hex"0000"                             ++  // Answer RRs
    hex"0000"                             ++  // Authority RRs
    hex"0000"                             ++  // Additional RRs
    hex"03777777066e6574627364036f726700" ++  // Name
    hex"0001"                             ++  // Type
    hex"0001"                                 // Class
  ).bits

  requestCodec.decode(requestMessage)
  
  roundTrip(Request(30144, List("www", "netbsd", "org")), requestCodec)
}