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

case class RegFileConfig(
    val name: String,
    val rows: Int,
    val width: Int,
    val read_ports: Int,
    val write_ports: Int
)

class RegFile(config: RegFileConfig) extends Module {
  override def desiredName = config.name
  val io = IO(new Bundle {
    val reads = Vec(
      config.read_ports,
      new Bundle {
        val addr = Input(UInt(log2Ceil(config.rows).W))
        val data = Output(UInt(config.width.W))
      }
    )
    val writes = Vec(
      config.write_ports,
      new Bundle {
        val addr = Input(UInt(log2Ceil(config.rows).W))
        val data = Input(UInt(config.width.W))
        val en = Input(Bool())
      }
    )
  })

  val mem = SyncReadMem(config.rows, UInt(config.width.W))
  for (read <- io.reads) {
    read.data := mem.read(read.addr)
  }
  for (write <- io.writes) {
    when(write.en) {
      mem.write(write.addr, write.data)
    }
  }
}

class Top(configs: Seq[RegFileConfig]) extends Module {

  val regfiles = configs.map { config =>
    Module(new RegFile(config))
  }

  val io = IO(new Bundle {
    val dut = MixedVec(regfiles.map(regfile => chiselTypeOf(regfile.io)).toSeq)
  })

  for ((regfile, i) <- regfiles.zipWithIndex) {
    io.dut(i) <> regfile.io
  }
}

object GenerateRegFileStudy extends App {
  val jsonInput = Paths.get(args(0))
  val (chiselArgs, delimiter) = args.drop(1).span(_ != "--")
  val firtoolArgs = delimiter.tail

  implicit val regFileConfigEncoder: Encoder[RegFileConfig] =
    deriveEncoder[RegFileConfig]
  // read in the json file, a map of string to RegFileConfig using circe
  val configs = io.circe.yaml.parser
    .parse(new String(java.nio.file.Files.readAllBytes(jsonInput), "UTF-8"))
    .getOrElse(throw new RuntimeException("Failed to parse YAML"))
    .as[Seq[RegFileConfig]]
    .getOrElse(
      throw new RuntimeException("Failed to decode YAML to RegFileConfig")
    )

  ChiselStage.emitSystemVerilog(
    new Top(configs),
    chiselArgs,
    firtoolArgs
  )

}
