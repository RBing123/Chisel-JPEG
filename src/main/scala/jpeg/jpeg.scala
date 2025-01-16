package jpeg

import chisel3._
import chisel3.internal.firrtl.Width
import chisel3.util._

/**
  * Performs JPEG Compression
  *
  * @param p JPEG Parameters
  * 
  * IO
  * @param pixelDataIn Pixel data to be encoded
  * 
  * @return dctOut DCT output used in testing
  * @return quantOut Quantization output used in testing
  * @return zigzagOut Zigzag output used in testing
  * @return encoded Encoded pixel data
  * 
  */
class JPEGEncodeChisel(p: JPEGParams) extends Module {
    val io = IO(new Bundle {
        val in = Flipped(Valid(new Bundle {
            val pixelDataIn = Input(Vec(p.givenRows, Vec(p.givenCols, SInt(9.W))))
        }))
        
        // Testing Outputs
        val dctOut = Output(Vec(8, Vec(8, SInt(32.W))))
        val quantOut = Output(Vec(p.numRows, Vec(p.numCols, SInt(32.W))))
        val zigzagOut = Output(Vec(p.totalElements, SInt(9.W)))

        // Final Encoded Output
        val encodedRLE = Output(Vec(p.maxOutRLE, SInt(p.w8)))
        val encodedDelta = Output(Vec(p.totalElements, SInt(p.w8)))
    })

    // Dontcare for output yet
    io.encodedRLE := DontCare
    io.encodedDelta := DontCare

    // Discrete Cosine Transform Module
    val dctModule = Module(new DCTChisel)
    dctModule.io.in.valid := io.in.valid
    dctModule.io.in.bits.matrixIn := io.in.bits.pixelDataIn
    io.dctOut := dctModule.io.dctOut.bits

    // Quantization Module
    val quantModule = Module(new QuantizationChisel(p))
    quantModule.io.in.valid := dctModule.io.dctOut.valid
    quantModule.io.in.bits.data := dctModule.io.dctOut.bits
    io.quantOut := quantModule.io.out.bits

    // Converts Quantization Table to SInt
    quantModule.io.quantTable.zipWithIndex.foreach { case (row, i) =>
        row.zipWithIndex.foreach { case (element, j) =>
            element := p.getQuantTable(i)(j).S
        }
    }

    // Zig Zag Module
    val zigzagModule = Module(new ZigZagChisel(p))
    zigzagModule.io.in.valid := quantModule.io.out.valid
    zigzagModule.io.in.bits.matrixIn := quantModule.io.out.bits
    io.zigzagOut := zigzagModule.io.zigzagOut.bits

    // Encoding Module
    when(zigzagModule.io.zigzagOut.valid){
        when(p.encodingChoice.B){
            val encodeModule = Module(new RLEChiselEncode(p))
            encodeModule.io.in.valid := zigzagModule.io.zigzagOut.valid
            encodeModule.io.in.bits.data := zigzagModule.io.zigzagOut.bits
            io.encodedRLE := encodeModule.io.out.bits
            io.encodedDelta := DontCare
        }
        .otherwise{
            val encodeModule = Module(new DeltaChiselEncode(p))
            encodeModule.io.in.valid := zigzagModule.io.zigzagOut.valid
            encodeModule.io.in.bits.data := zigzagModule.io.zigzagOut.bits
            io.encodedRLE := DontCare
            io.encodedDelta := encodeModule.io.out.bits
        }
    }

}