# Decoding DNS

Let's create our own dns server. Bring your laptop and you will learn how to handle udp connection, binary encoding.

Level: **Intermediate**

## Required Software:

* [jdk 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [sbt](http://www.scala-sbt.org/download.html)
* [git](https://git-scm.com/downloads)
* [chrome](https://www.google.com/chrome/browser/desktop/index.html) or any web browser
* [wireshark](https://www.wireshark.org/download.html) (*optional*)

## Preparation:

```
$ git clone https://github.com/MasseGuillaume/DNS.git
$ cd DNS
$ sbt
> kstart
this last command opens scalakata in your browser
```

## Optional reading:

* [scalaz-stream](https://github.com/scalaz/scalaz-stream/blob/master/src/test/scala/scalaz/stream/examples/StartHere.scala#L9)
* [scodec](http://scodec.org/guide/)
* [rfc1035](https://www.ietf.org/rfc/rfc1035.txt)
* [rfc1034](https://www.ietf.org/rfc/rfc1034.txt)