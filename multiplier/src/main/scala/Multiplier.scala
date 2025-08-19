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

class Multiplier(config: MultiplierConfig) extends Module {
  override def desiredName = config.top
  val io = IO(new Bundle {
    val in = Input(Vec(2, UInt(config.width.W)))
    val out = Output(UInt(config.width.W))
  })

  val core = Module(new Module {
    override def desiredName = config.top + "_core"
    val core_io = IO(chiselTypeOf(io))
    // Large fanout synchronous reset and pipelining,
    // great for testing retiming.
    withReset(ShiftRegister(reset.asBool, config.latency)) {
      // the pipeline registers are free-running, thus not hooked up to the
      // synchronous reset
      core_io.out := ShiftRegister(core_io.in.reduce(_ * _), config.latency)
    }
  })

  // Input and output are always registered at this top level,
  // but only the core is retimed, as we want to plot reg2reg paths.
  io.out := RegNext(core.core_io.out)
  core.core_io.in := RegNext(io.in)
}
