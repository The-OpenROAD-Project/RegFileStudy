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

object Architecture {
  object Representation extends Enumeration {
    type Representation = Value

    protected case class RepresentationVal(
        signed: Boolean,
        expBits: Int,
        fracBits: Int
    ) extends super.Val {
      def int: Boolean = !float
      def float = expBits != 0
      def halfWordsLog2 = log2Ceil(width / F32.width)
      def width = expBits + fracBits
    }

    import scala.language.implicitConversions
    implicit def valueToRepresentationVal(x: Value): RepresentationVal =
      x.asInstanceOf[RepresentationVal]

    val F32 = RepresentationVal(true, 8, 24)
    val F64 = RepresentationVal(true, 11, 53)
    val S64 = RepresentationVal(true, 0, 64)
    val U64 = RepresentationVal(false, 0, 64)
  }
}

object FloatingPointOperation extends ChiselEnum {
  val Add, Compare, Convert, Div, Mul, Inject = Value
}

object FloatingPointComparison extends ChiselEnum {
  val Eq, Lt, Le = Value
}

class FloatingPointUnitIO(data: UInt) extends Bundle {
  val operation = Input(FloatingPointOperation())
  val comparison = Input(FloatingPointComparison())
  val aluWidthHalfWordsLog2 = Input(UInt(1.W))
  val negate = Input(Bool())
  val xor = Input(Bool())
  val fromInt = Input(Bool())
  val toInt = Input(Bool())
  val fromSigned = Input(Bool())
  val toSigned = Input(Bool())
  val fromWidthHalfWordsLog2 = Input(Bool())
  val toWidthHalfWordsLog2 = Input(Bool())
  val srcs = Input(Vec(2, data))
  val dst = Output(data)
}

class MockFloatingPointUnit(data: UInt) extends Module {
  val io = IO(new FloatingPointUnitIO(data))

  io.dst := DontCare
  for (
    float <- Seq(
      Architecture.Representation.F32,
      Architecture.Representation.F64
    )
  ) {
    when(io.fromWidthHalfWordsLog2 === float.halfWordsLog2.U) {
      val recs =
        io.srcs.map(hardfloat.recFNFromFN(float.expBits, float.fracBits, _))
      switch(io.operation) {
        is(FloatingPointOperation.Add) {
          val adder =
            Module(new hardfloat.AddRecFN(float.expBits, float.fracBits))
          adder.io.a := recs(0)
          adder.io.b := recs(1)
          adder.io.subOp := io.negate
          adder.io.detectTininess := hardfloat.consts.tininess_afterRounding
          adder.io.roundingMode := hardfloat.consts.round_near_even
          io.dst := hardfloat.fNFromRecFN(
            float.expBits,
            float.fracBits,
            adder.io.out
          )
        }
        is(FloatingPointOperation.Mul) {
          val mul =
            Module(new hardfloat.MulRecFN(float.expBits, float.fracBits))
          mul.io.a := recs(0)
          mul.io.b := recs(1)
          mul.io.detectTininess := hardfloat.consts.tininess_afterRounding
          mul.io.roundingMode := hardfloat.consts.round_near_even
          io.dst := hardfloat.fNFromRecFN(
            float.expBits,
            float.fracBits,
            mul.io.out
          )
        }
        is(FloatingPointOperation.Compare) {
          val comparator =
            Module(new hardfloat.CompareRecFN(float.expBits, float.fracBits))
          comparator.io.a := recs(0)
          comparator.io.b := recs(1)
          comparator.io.signaling := false.B
          io.dst := Mux1H(
            Seq(
              (io.comparison === FloatingPointComparison.Eq) -> comparator.io.eq,
              (io.comparison === FloatingPointComparison.Lt) -> comparator.io.lt,
              (io.comparison === FloatingPointComparison.Le) -> !comparator.io.gt
            )
          )
        }
        is(FloatingPointOperation.Convert) {
          when(io.toInt) {
            val toInt = Module(
              new hardfloat.RecFNToIN(
                float.expBits,
                float.fracBits,
                data.getWidth
              )
            )
            toInt.io.in := hardfloat.recFNFromFN(
              float.expBits,
              float.fracBits,
              io.srcs(0)
            )
            toInt.io.roundingMode := hardfloat.consts.round_minMag
            toInt.io.signedOut := io.toSigned
            io.dst := toInt.io.out
          }
        }
        is(FloatingPointOperation.Inject) {
          val dst =
            (Mux(io.xor, recs(0).head(1), io.negate) ^ recs(1).head(1)) ## recs(
              0
            ).tail(1)
          io.dst := hardfloat.fNFromRecFN(float.expBits, float.fracBits, dst)
        }
      }
    }

    when(
      io.toWidthHalfWordsLog2 === float.halfWordsLog2.U && io.operation === FloatingPointOperation.Convert
    ) {
      when(io.fromInt) {
        val fromInt =
          Module(
            new hardfloat.INToRecFN(
              data.getWidth,
              float.expBits,
              float.fracBits
            )
          )

        fromInt.io.in := io.srcs(0)
        fromInt.io.signedIn := io.fromSigned
        fromInt.io.detectTininess := hardfloat.consts.tininess_afterRounding
        fromInt.io.roundingMode := hardfloat.consts.round_near_even
        io.dst := hardfloat.fNFromRecFN(
          float.expBits,
          float.fracBits,
          fromInt.io.out
        )
      }

      when(!io.fromInt && !io.toInt) {
        val fromFloat =
          if (float != Architecture.Representation.F32)
            Architecture.Representation.F32
          else Architecture.Representation.F64

        val converter =
          Module(
            new hardfloat.RecFNToRecFN(
              fromFloat.expBits,
              fromFloat.fracBits,
              float.expBits,
              float.fracBits
            )
          )

        converter.io.in := hardfloat.recFNFromFN(
          fromFloat.expBits,
          fromFloat.fracBits,
          io.srcs(0)
        )
        converter.io.roundingMode := hardfloat.consts.round_near_even
        converter.io.detectTininess := hardfloat.consts.tininess_afterRounding

        io.dst := hardfloat.fNFromRecFN(
          float.expBits,
          float.fracBits,
          converter.io.out
        )
      }
    }
  }
}

class FloatingPointUnit(config: MultiplierConfig) extends Module {
  override def desiredName = config.top
  val dataWord = UInt(64.W)
  val io = IO(new FloatingPointUnitIO(dataWord))

  def pipelineMockFloatIO(
      core_io: FloatingPointUnitIO,
      mockFloat_io: FloatingPointUnitIO,
      latency: Int
  ): Unit = {
    mockFloat_io.operation := ShiftRegister(core_io.operation, latency)
    mockFloat_io.comparison := ShiftRegister(core_io.comparison, latency)
    mockFloat_io.aluWidthHalfWordsLog2 := ShiftRegister(
      core_io.aluWidthHalfWordsLog2,
      latency
    )
    mockFloat_io.negate := ShiftRegister(core_io.negate, latency)
    mockFloat_io.xor := ShiftRegister(core_io.xor, latency)
    mockFloat_io.fromInt := ShiftRegister(core_io.fromInt, latency)
    mockFloat_io.toInt := ShiftRegister(core_io.toInt, latency)
    mockFloat_io.fromSigned := ShiftRegister(core_io.fromSigned, latency)
    mockFloat_io.toSigned := ShiftRegister(core_io.toSigned, latency)
    mockFloat_io.toWidthHalfWordsLog2 := ShiftRegister(
      core_io.toWidthHalfWordsLog2,
      latency
    )
    mockFloat_io.fromWidthHalfWordsLog2 := ShiftRegister(
      core_io.fromWidthHalfWordsLog2,
      latency
    )
    mockFloat_io.srcs := ShiftRegister(core_io.srcs, latency)
    core_io.dst := ShiftRegister(mockFloat_io.dst, latency)
  }

  val core = Module(new Module {
    override def desiredName = config.top + "_core"
    val core_io = IO(chiselTypeOf(io))
    // Large fanout synchronous reset and pipelining,
    // great for testing retiming.
    withReset(ShiftRegister(reset.asBool, config.latency)) {
      val mockFloat = Module(new MockFloatingPointUnit(dataWord))
      pipelineMockFloatIO(core_io, mockFloat.io, config.latency)
    }
  })

  // Input and output are always registered at this top level,
  // but only the core is retimed, as we want to plot reg2reg paths.
  // pipelineMockFloatIO(core_io, mockFloat.io, config.latency)
  pipelineMockFloatIO(io, core.core_io, 1)
}
