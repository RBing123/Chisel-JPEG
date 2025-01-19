package jpeg

import chisel3._
import chisel3.util._

/**
  * Object for RLEChiselEncode
  */
object RLEChiselEncode {
    def apply(params: JPEGParams, data: Valid[Vec[SInt]], out: Valid[Vec[SInt]], length: Valid[UInt]) = {
        val mod = Module(new RLEChiselEncode(params))
        mod.io.in := data
        mod.io.out := out
        mod.io.length := length
        mod
    }
}

/**
  * Object for DeltaChiselEncode
  */
object DeltaChiselEncode {
    def apply(params: JPEGParams, data: Valid[Vec[SInt]], out: Valid[Vec[SInt]]) = {
        val mod = Module(new DeltaChiselEncode(params))
        mod.io.in := data
        mod.io.out := out
        mod
    }
}

/** 
  * Creates states for encoding in RLE and Delta
  */
object EncodingState extends ChiselEnum {
    val idle, encode = Value
}

/** Performs Run Length Encoding
  * 
  * @param p JPEG Parameters
  * 
  * IO
  * @param data Data to perform RLE on
  * 
  * @return out Result of performing RLE
  * @return length Length of RLE output in out since we cannot determine final width dynamically in hardware
  * @return state Current state of state machine
  */
class RLEChiselEncode(p: JPEGParams) extends Module{
    val io = IO(new Bundle {
        val in = Flipped(Valid(new Bundle{
            val data = Vec(p.totalElements, SInt(p.w8))
        }))
        val out = Valid(Vec(p.maxOutRLE, SInt(p.w8)))
        val length = Valid(UInt())
        val state = Output(EncodingState())
    })
    
    val stateReg = RegInit(EncodingState.idle)

    // Initialize to all zeros
    val dataReg = RegInit(VecInit(Seq.fill(p.totalElements)(0.S(p.w8))))
    val dataIndex = RegInit(1.U(log2Ceil(p.totalElements+1).W))

    // Initialize output register
    val outValid = RegInit(false.B)
    val lenValid = RegInit(false.B)
    val outputReg = RegInit(VecInit(Seq.fill(128)(0.S(p.w8)))) 
    
    // counters for keeping track of index
    val consecutiveCounter = RegInit(0.U(log2Ceil(p.totalElements+1).W))
    val valueCounter = RegInit(1.U(log2Ceil(p.totalElements+1).W))
    val consecutive = RegInit(1.S(log2Ceil(p.totalElements+1).W))

    val lenCounter = RegInit(0.U(log2Ceil(p.maxOutRLE+1).W))
    val current = RegInit(0.S(p.w8))

    // assign output 
    io.out.valid := outValid
    io.out.bits := outputReg

    // to keep track of how many elements in out are used and assign length output
    io.length.valid := lenValid
    io.length.bits := lenCounter
    io.state := stateReg

    // Statemachine logic
    switch(stateReg){
        is(EncodingState.idle){
            when(io.in.fire){
                current := io.in.bits.data(0)
                dataReg := io.in.bits.data
                stateReg := EncodingState.encode
                outValid := false.B
                lenValid := false.B
            }
        }

        is(EncodingState.encode){
            when (dataIndex < p.totalElements.U) {
                when(dataReg(dataIndex) === current){
                    // counts the consecutive indices
                    consecutive := consecutive + 1.S
                }
                .otherwise {
                    // assigns out consecutive and the value
                    outputReg(consecutiveCounter) := consecutive
                    outputReg(valueCounter) := current
                    consecutiveCounter := consecutiveCounter + 2.U
                    valueCounter := valueCounter + 2.U
                    current := dataReg(dataIndex)
                    consecutive := 1.S
                    lenCounter := lenCounter + 2.U
                }
                dataIndex := dataIndex + 1.U
            }
            .otherwise{
                outputReg(consecutiveCounter) := consecutive
                outputReg(valueCounter) := current
                lenCounter := lenCounter + 2.U
                outValid := true.B
                lenValid := true.B
                stateReg := EncodingState.idle
            }
        }
    }
}

/** Performs Delta Encoding
  * 
  * @param p JPEG Parameters
  * 
  * IO
  * @param data Data to perform Delta Encoding on
  * 
  * @return out Result of performing Delta Encoding
  * @return state Current state of state machine
  * 
  */
class DeltaChiselEncode(p: JPEGParams) extends Module{
    val io = IO(new Bundle {
        val in = Flipped(Valid(new Bundle{
            val data = Vec(p.totalElements, SInt(p.w8))
        }))
        val out = Valid(Vec(p.totalElements, SInt(p.w8)))
        val state = Output(EncodingState())
    })
    
    val stateReg = RegInit(EncodingState.idle)

    // Initialize to all zeros
    val dataReg = RegInit(VecInit(Seq.fill(p.totalElements)(0.S(p.w8))))
    val dataIndex = RegInit(1.U(log2Ceil(p.totalElements+1).W))

    // Initialize output register
    val outValid = RegInit(false.B)
    val outputReg = RegInit(VecInit(Seq.fill(p.totalElements)(0.S(p.w8)))) 
    // Initialize the previous top left element
    val prevTopLeft = RegInit(0.S(p.w8))

    // assign outputs
    io.state := stateReg
    io.out.valid := outValid
    io.out.bits := outputReg

    // Statemachine logic
    switch(stateReg){
        is(EncodingState.idle){
            when(io.in.fire){
                dataReg := io.in.bits.data
                stateReg := EncodingState.encode
                outValid := false.B
            }
        }

        is(EncodingState.encode){
            // outputReg(0) := dataReg(0)
            when(dataIndex === 1.U) {
                val topLeftDiff = dataReg(0) - prevTopLeft // Current block's top-left - Previous block's top-left
                outputReg(0) := topLeftDiff
                prevTopLeft := dataReg(0) // Update the previous top-left value
            }
            when(dataIndex > 0.U && dataIndex < p.totalElements.U) {
                val diff = dataReg(dataIndex) - dataReg(dataIndex - 1.U)
                outputReg(dataIndex) := diff
                dataIndex := dataIndex + 1.U
            }
            .otherwise {
                outValid := true.B
                stateReg := EncodingState.idle
            }
            // when (dataIndex < p.totalElements.U) {
            //     // calcuates the difference and assigns to output
            //     val diff = dataReg(dataIndex) - dataReg(dataIndex - 1.U)
            //     outputReg(dataIndex) := diff
            //     dataIndex := dataIndex + 1.U
            // }
            // .otherwise{
            //     outValid := true.B
            //     stateReg := EncodingState.idle
            // }
        }
    }

}