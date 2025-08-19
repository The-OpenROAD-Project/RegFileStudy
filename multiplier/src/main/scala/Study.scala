import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.yaml.syntax._
import io.circe.Encoder
import io.circe.generic.semiauto._
import circt.stage.ChiselStage
import chisel3._
import chisel3.util._
import chisel3.stage._
import chisel3.experimental._
import chisel3.util.HasBlackBoxResource
import scopt.OParser
import System.err
import scopt.RenderingMode
import scala.collection.immutable.SeqMap
import java.nio.file.Paths

case class Info(
    val stage: String,
    val study: Seq[MultiplierConfig]
)

case class MultiplierConfig(
    val name: String,
    val top: String,
    val width: Int,
    val latency: Int,
    val retime: Int
)

class Top[T <: Module { val io: Bundle }](
    configs: Seq[MultiplierConfig],
    gen: MultiplierConfig => T
) extends Module {
  val modules = configs.filter(_.retime == 1).map(config => Module(gen(config)))
  val io = IO(new Bundle {
    val dut = MixedVec(modules.map(a => chiselTypeOf(a.io)))
  })
  for ((mod, i) <- modules.zipWithIndex) {
    io.dut(i) <> mod.io
  }
}

object GenerateStudy extends App {
  val (beforeFirstDash, afterFirstDash) = args.span(_ != "--")
  val (chiselArgs, afterSecondDash) = afterFirstDash.drop(1).span(_ != "--")
  val firtoolArgs = afterSecondDash.drop(1)

  println(s"Args: ${beforeFirstDash.mkString(" ")}")
  println(s"Chisel Args: ${chiselArgs.mkString(" ")}")
  println(s"Firtool Args: ${firtoolArgs.mkString(" ")}")

  val moduleName = beforeFirstDash(0)
  val jsonInput = Paths.get(beforeFirstDash(1))

  implicit val MultiplierConfigEncoder: Encoder[MultiplierConfig] =
    deriveEncoder[MultiplierConfig]
  // read in the json file, a map of string to MultiplierConfig using circe
  val configs = io.circe.yaml.parser
    .parse(new String(java.nio.file.Files.readAllBytes(jsonInput), "UTF-8"))
    .getOrElse(throw new RuntimeException("Failed to parse YAML"))
    .as[Info]
    .getOrElse(
      throw new RuntimeException("Failed to decode YAML to MultiplierConfig")
    )
    .study

  val constructor = Class.forName(moduleName).getConstructors().head

  ChiselStage.emitHWDialect(
    new Top(
      configs,
      config =>
        constructor.newInstance(config).asInstanceOf[Module { val io: Bundle }]
    ),
    chiselArgs,
    firtoolArgs
  )
}
