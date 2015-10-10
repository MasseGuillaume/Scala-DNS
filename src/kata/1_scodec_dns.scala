import com.scalakata._

import scodec.{bits => _, _}
import scodec.codecs._
import scodec.bits._

@instrument class Dns {  
  def bit = bool(1)
  val requestCodec = (
    ("Transaction ID"         | uint16)               ::
    ("Response"               | constant(bin"0"))     :: 
    ignore(4)                                         :: // ("Opcode" | bits(4)) ::
    ignore(1)                                         ::
    ("Truncated"              | bit)                  ::
    ("Recursion"              | bit)                  ::
    ignore(3)                                         ::
    ("Non-authenticated data" | bit)                  ::
    ignore(4)                                         ::
    ("Questions"              | ignore(16))           :: // >>:~ ( questions =>
    ("Answer RRs"             | ignore(16))           ::
    ("Authority RRs"          | ignore(16))           ::
    ("Additional RRs"         | ignore(16))           ::
    ("Name"                   | cstring)              ::
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
  
  val name = 
    requestCodec.decode(message) match {
      case Attempt.Successful(DecodeResult(Request(_, _, _, _, name), _)) => name
      case _ => ""
    }

  name == "askubuntu.com" // 09 ? 03 ?
  
  case class Request(
    transactionID: Int,
    // opcode: Byte, ignored
    truncated: Boolean,
    recursive: Boolean,
    authorative: Boolean,
    name: String
  )
}