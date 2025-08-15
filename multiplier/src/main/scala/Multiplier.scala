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

case class MultiplierConfig(
    val name: String,
    val width: Int,
    val latency: Int
)

class Multiplier(config: MultiplierConfig) extends Module {
  override def desiredName = config.name
  val io = IO(new Bundle {
    val in = Input(Vec(2, UInt(config.width.W)))
    val out = Output(UInt(config.width.W))
  })

  // Large fanout synchronous reset and pipelining
  withReset(ShiftRegister(reset.asBool, config.latency)) {
    // Input and output are always registered, we are plotting reg2reg paths.
    // Latency is w.r.t. how many pipeline stages we have for multiply operation
    // and reset.
    val in = RegNext(io.in)
    io.out := RegNext(
      ShiftRegister(in.reduce(_ * _), config.latency, 0.U, true.B)
    )
  }
}

class Top(configs: Seq[MultiplierConfig]) extends Module {

  val multipliers = configs.map { config =>
    Module(new Multiplier(config))
  }

  val io = IO(new Bundle {
    val dut =
      MixedVec(multipliers.map(multiplier => chiselTypeOf(multiplier.io)).toSeq)
  })

  for ((regfile, i) <- multipliers.zipWithIndex) {
    io.dut(i) <> regfile.io
  }
}

object GenerateMultiplierStudy extends App {
  val jsonInput = Paths.get(args(0))
  val (chiselArgs, delimiter) = args.drop(1).span(_ != "--")
  val firtoolArgs = delimiter.tail

  implicit val MultiplierConfigEncoder: Encoder[MultiplierConfig] =
    deriveEncoder[MultiplierConfig]
  // read in the json file, a map of string to MultiplierConfig using circe
  val configs = io.circe.yaml.parser
    .parse(new String(java.nio.file.Files.readAllBytes(jsonInput), "UTF-8"))
    .getOrElse(throw new RuntimeException("Failed to parse YAML"))
    .as[Seq[MultiplierConfig]]
    .getOrElse(
      throw new RuntimeException("Failed to decode YAML to MultiplierConfig")
    )

  ChiselStage.emitSystemVerilog(
    new Top(configs),
    chiselArgs,
    firtoolArgs
  )
}
