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
            // val pixelDataIn = Input(Vec(p.givenRows, Vec(p.givenCols, SInt(9.W))))
            val yComponent = Input(Vec(p.givenRows, Vec(p.givenCols, SInt(9.W))))
            val cbComponent = Input(Vec(p.givenRows, Vec(p.givenCols, SInt(9.W))))
            val crComponent = Input(Vec(p.givenRows, Vec(p.givenCols, SInt(9.W))))
        }))
        
        // // Testing Outputs
        // val dctOut = Output(Vec(8, Vec(8, SInt(32.W))))
        // val quantOut = Output(Vec(p.numRows, Vec(p.numCols, SInt(32.W))))
        // val zigzagOut = Output(Vec(p.totalElements, SInt(9.W)))

        // // Final Encoded Output
        // val encodedRLE = Output(Vec(p.maxOutRLE, SInt(p.w8)))
        // val encodedDelta = Output(Vec(p.totalElements, SInt(p.w8)))
        // Y output
        val dctOutY = Output(Vec(8, Vec(8, SInt(32.W))))
        val quantOutY = Output(Vec(p.numRows, Vec(p.numCols, SInt(32.W))))
        val zigzagOutY = Output(Vec(p.totalElements, SInt(9.W)))
        val encodedRLEY = Output(Vec(p.maxOutRLE, SInt(p.w8)))
        val encodedDeltaY = Output(Vec(p.totalElements, SInt(p.w8)))
        // Cb output
        val dctOutCb = Output(Vec(8, Vec(8, SInt(32.W))))
        val quantOutCb = Output(Vec(p.numRows, Vec(p.numCols, SInt(32.W))))
        val zigzagOutCb = Output(Vec(p.totalElements, SInt(9.W)))
        val encodedRLECb = Output(Vec(p.maxOutRLE, SInt(p.w8)))
        val encodedDeltaCb = Output(Vec(p.totalElements, SInt(p.w8)))
        // Cr output
        val dctOutCr = Output(Vec(8, Vec(8, SInt(32.W))))
        val quantOutCr = Output(Vec(p.numRows, Vec(p.numCols, SInt(32.W))))
        val zigzagOutCr = Output(Vec(p.totalElements, SInt(9.W)))
        val encodedRLECr = Output(Vec(p.maxOutRLE, SInt(p.w8)))
        val encodedDeltaCr = Output(Vec(p.totalElements, SInt(p.w8)))
    })

    // Dontcare for output yet
    // io.encodedRLE := DontCare
    // io.encodedDelta := DontCare
    io.encodedRLEY := DontCare
    io.encodedRLECb := DontCare
    io.encodedRLECr := DontCare
    io.encodedDeltaY := DontCare
    io.encodedDeltaCb := DontCare
    io.encodedDeltaCr := DontCare

    // Y channel
    // Discrete Cosine Transform Module
    // val dctModule = Module(new DCTChisel)
    // dctModule.io.in.valid := io.in.valid
    // dctModule.io.in.bits.matrixIn := io.in.bits.pixelDataIn
    // io.dctOut := dctModule.io.dctOut.bits
    val dctModuleY = Module(new DCTChisel)
    val quantModuleY = Module(new QuantizationChisel(p.copy(qtChoice = 1)))
    val zigzagModuleY = Module(new ZigZagChisel(p))
    // val encodeModuleY = Module(new RLEChiselEncode(p))
    dctModuleY.io.in.valid := io.in.valid
    dctModuleY.io.in.bits.matrixIn := io.in.bits.yComponent
    quantModuleY.io.in.valid := dctModuleY.io.dctOut.valid
    quantModuleY.io.in.bits.data := dctModuleY.io.dctOut.bits
    zigzagModuleY.io.in.valid := quantModuleY.io.out.valid
    zigzagModuleY.io.in.bits.matrixIn := quantModuleY.io.out.bits
    quantModuleY.io.quantTable.zipWithIndex.foreach { case (row, i) =>
        row.zipWithIndex.foreach { case (element, j) =>
            element := p.getQuantTable(i)(j).S
        }
    }
    // Cb channel
    val dctModuleCb = Module(new DCTChisel)
    val quantModuleCb = Module(new QuantizationChisel(p.copy(qtChoice = 2)))
    val zigzagModuleCb = Module(new ZigZagChisel(p))
    // val encodeModuleCb = Module(new RLEChiselEncode(p))
    dctModuleCb.io.in.valid := io.in.valid
    dctModuleCb.io.in.bits.matrixIn := io.in.bits.cbComponent
    quantModuleCb.io.in.valid := dctModuleCb.io.dctOut.valid
    quantModuleCb.io.in.bits.data := dctModuleCb.io.dctOut.bits
    zigzagModuleCb.io.in.valid := quantModuleCb.io.out.valid
    zigzagModuleCb.io.in.bits.matrixIn := quantModuleCb.io.out.bits
    quantModuleCb.io.quantTable.zipWithIndex.foreach { case (row, i) =>
        row.zipWithIndex.foreach { case (element, j) =>
            element := p.getQuantTable(i)(j).S
        }
    }
    // Cr channel
    val dctModuleCr = Module(new DCTChisel)
    val quantModuleCr = Module(new QuantizationChisel(p.copy(qtChoice = 2)))
    val zigzagModuleCr = Module(new ZigZagChisel(p))
    // val encodeModuleCr = Module(new RLEChiselEncode(p))
    dctModuleCr.io.in.valid := io.in.valid
    dctModuleCr.io.in.bits.matrixIn := io.in.bits.crComponent
    quantModuleCr.io.in.valid := dctModuleCr.io.dctOut.valid
    quantModuleCr.io.in.bits.data := dctModuleCr.io.dctOut.bits
    zigzagModuleCr.io.in.valid := quantModuleCr.io.out.valid
    zigzagModuleCr.io.in.bits.matrixIn := quantModuleCr.io.out.bits
    quantModuleCr.io.quantTable.zipWithIndex.foreach { case (row, i) =>
        row.zipWithIndex.foreach { case (element, j) =>
            element := p.getQuantTable(i)(j).S
        }
    }
    
    when(zigzagModuleY.io.zigzagOut.valid) {
        when(p.encodingChoice.B) {
            val encodeModuleY = Module(new RLEChiselEncode(p))
            encodeModuleY.io.in.valid := zigzagModuleY.io.zigzagOut.valid
            encodeModuleY.io.in.bits.data := zigzagModuleY.io.zigzagOut.bits
            io.encodedRLEY := encodeModuleY.io.out.bits
        } .otherwise {
            val encodeModuleY = Module(new DeltaChiselEncode(p))
            encodeModuleY.io.in.valid := zigzagModuleY.io.zigzagOut.valid
            encodeModuleY.io.in.bits.data := zigzagModuleY.io.zigzagOut.bits
            io.encodedDeltaY := encodeModuleY.io.out.bits
        }
    }

    when(zigzagModuleCb.io.zigzagOut.valid) {
        when(p.encodingChoice.B) {
            val encodeModuleCb = Module(new RLEChiselEncode(p))
            encodeModuleCb.io.in.valid := zigzagModuleCb.io.zigzagOut.valid
            encodeModuleCb.io.in.bits.data := zigzagModuleCb.io.zigzagOut.bits
            io.encodedRLECb := encodeModuleCb.io.out.bits
        // } .otherwise {
            val encodeModuleCb = Module(new DeltaChiselEncode(p))
            encodeModuleCb.io.in.valid := zigzagModuleCb.io.zigzagOut.valid
            encodeModuleCb.io.in.bits.data := zigzagModuleCb.io.zigzagOut.bits
            io.encodedDeltaCb := encodeModuleCb.io.out.bits
        }
    }

    when(zigzagModuleCr.io.zigzagOut.valid) {
        when(p.encodingChoice.B) {
            val encodeModuleCr = Module(new RLEChiselEncode(p))
            encodeModuleCr.io.in.valid := zigzagModuleCr.io.zigzagOut.valid
            encodeModuleCr.io.in.bits.data := zigzagModuleCr.io.zigzagOut.bits
            io.encodedRLECr := encodeModuleCr.io.out.bits
        } .otherwise {
            val encodeModuleCr = Module(new DeltaChiselEncode(p))
            encodeModuleCr.io.in.valid := zigzagModuleCr.io.zigzagOut.valid
            encodeModuleCr.io.in.bits.data := zigzagModuleCr.io.zigzagOut.bits
            io.encodedDeltaCr := encodeModuleCr.io.out.bits
        }
    }

    // 測試輸出連接
    io.dctOutY := dctModuleY.io.dctOut.bits
    io.dctOutCb := dctModuleCb.io.dctOut.bits
    io.dctOutCr := dctModuleCr.io.dctOut.bits

    io.quantOutY := quantModuleY.io.out.bits
    io.quantOutCb := quantModuleCb.io.out.bits
    io.quantOutCr := quantModuleCr.io.out.bits

    io.zigzagOutY := zigzagModuleY.io.zigzagOut.bits
    io.zigzagOutCb := zigzagModuleCb.io.zigzagOut.bits
    io.zigzagOutCr := zigzagModuleCr.io.zigzagOut.bits
    // // Quantization Module
    // val quantModule = Module(new QuantizationChisel(p))
    // quantModule.io.in.valid := dctModule.io.dctOut.valid
    // quantModule.io.in.bits.data := dctModule.io.dctOut.bits
    // io.quantOut := quantModule.io.out.bits

    // // Converts Quantization Table to SInt
    // quantModule.io.quantTable.zipWithIndex.foreach { case (row, i) =>
    //     row.zipWithIndex.foreach { case (element, j) =>
    //         element := p.getQuantTable(i)(j).S
    //     }
    // }

    // // Zig Zag Module
    // val zigzagModule = Module(new ZigZagChisel(p))
    // zigzagModule.io.in.valid := quantModule.io.out.valid
    // zigzagModule.io.in.bits.matrixIn := quantModule.io.out.bits
    // io.zigzagOut := zigzagModule.io.zigzagOut.bits

    // // Encoding Module
    // when(zigzagModule.io.zigzagOut.valid){
    //     when(p.encodingChoice.B){
    //         val encodeModule = Module(new RLEChiselEncode(p))
    //         encodeModule.io.in.valid := zigzagModule.io.zigzagOut.valid
    //         encodeModule.io.in.bits.data := zigzagModule.io.zigzagOut.bits
    //         io.encodedRLE := encodeModule.io.out.bits
    //         io.encodedDelta := DontCare
    //     }
    //     .otherwise{
    //         val encodeModule = Module(new DeltaChiselEncode(p))
    //         encodeModule.io.in.valid := zigzagModule.io.zigzagOut.valid
    //         encodeModule.io.in.bits.data := zigzagModule.io.zigzagOut.bits
    //         io.encodedRLE := DontCare
    //         io.encodedDelta := encodeModule.io.out.bits
    //     }
    // }

}