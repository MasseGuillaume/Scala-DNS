package dns

object Standalone {
  def main(args: Array[String]): Unit = {
    ClientServer.server(53).repeat.run.run
  }
}
