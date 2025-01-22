package jpeg

import chisel3._
import chisel3.internal.firrtl.Width
import chisel3.util._
import scala.math._
import chisel3.experimental._

/**
  * Object for IDCT
  */
object IDCTChisel {
    def apply(matrixIn: Valid[Vec[Vec[SInt]]], shiftedOut: Vec[Vec[SInt]], idctOut: Valid[Vec[Vec[SInt]]]) = {
        val mod = Module(new IDCTChisel)
        mod.io.in := matrixIn
        mod.io.shiftedOut := shiftedOut
        mod.io.idctOut := idctOut
        mod
    }
}

/**
  * Creates FSM states for IDCT
  */
object IDCTState extends ChiselEnum {
  val loading, shifting, calculating, waiting = Value
}

/** Performs IDCT on 8x8 Matrix with scaling
  *
  * IO
  * @param dctIn Input matrix to perform IDCT on
  *
  * @return shiftedOut Re-shifted version of output matrix by +128
  * @return idctOut Resulting restored output of IDCT
  */
class IDCTChisel extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Valid(new Bundle {
      val matrixIn = Input(Vec(8, Vec(8, SInt(32.W))))
    }))
    val shiftedOut = Output(Vec(8, Vec(8, SInt(9.W)))) // Test output to check re-shifted block
    val idctOut = Valid(Vec(8, Vec(8, SInt(9.W))))
  })

  // Initializes registers for matrix and Valid bit
  val dctInput = Reg(Vec(8, Vec(8, SInt(32.W))))
  val shiftedBlock = Reg(Vec(8, Vec(8, SInt(9.W))))
  val matrixOutput = Reg(Vec(8, Vec(8, SInt(9.W))))
  val validOut = RegInit(false.B)

  // Assigns outputs
  io.idctOut.valid := validOut
  io.idctOut.bits := matrixOutput
  io.shiftedOut := DontCare

  // Function to compute IDCT values for each element of input matrix
  def IDCT(matrix: Vec[Vec[SInt]]): Vec[Vec[SInt]] = {
    val idctMatrix = Wire(Vec(8, Vec(8, SInt(9.W))))

    // Compute IDCT
    for (i <- 0 until 8) {
      for (j <- 0 until 8) {
        var sum = 0.S
        for (u <- 0 until 8) {
          for (v <- 0 until 8) {
            val coefValue = matrix(u)(v)
            val alphaU = if (u == 0) (1.0 / math.sqrt(2)) * 100 else 100
            val alphaV = if (v == 0) (1.0 / math.sqrt(2)) * 100 else 100
            
            val cosVal = (math.cos((2 * i + 1) * u * Pi / 16) * math.cos((2 * j + 1) * v * Pi / 16) * 100).toInt.S  
            val divSum = (coefValue * cosVal * alphaU.toInt.S * alphaV.toInt.S) / 1000000.S
            val divSum2 = divSum / 1000000.S
            sum = sum +& divSum / 1000000.S
            //if(i == 1 && j == 1)
              //printf(p"coefValue: ${coefValue}, cosVal: ${cosVal}, divSum, ${divSum}, divSum2, ${divSum2}, sum: ${sum}\n")
          }
        }
        idctMatrix(i)(j) := (sum / 4.S)
        //printf(p"idctMatrix: ${idctMatrix(i)(j)}\n")
      }
    }

    idctMatrix
  }

  // Initializes state and defines FSM
  val state = RegInit(IDCTState.waiting)
  switch(state) {
    is(IDCTState.waiting) {
      when(io.in.valid) {
        //printf("Entering calculating state\n")
        dctInput := io.in.bits.matrixIn
        state := IDCTState.calculating
        validOut := false.B
      }
    }
    is(IDCTState.calculating) {
      //printf("Calculating IDCT\n")

      // Assigns output matrix to calculated IDCT values
      val idctResult = IDCT(dctInput)

      for (i <- 0 until 8) {
        for (j <- 0 until 8) {
          matrixOutput(i)(j) := idctResult(i)(j) +& 128.S // Re-shifting to restore original range
        }
      }
      io.shiftedOut := matrixOutput
      state := IDCTState.waiting
      validOut := true.B
    }
  }
}
