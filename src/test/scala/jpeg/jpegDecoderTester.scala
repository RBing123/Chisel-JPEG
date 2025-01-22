package jpeg

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import java.beans.beancontext.BeanContextChildSupport

/**
  * JPEG 解碼的測試程式
  */
class JPEGDecodeChiselTest extends AnyFlatSpec with ChiselScalatestTester {

    /**
        * 測試 JPEG 解碼過程
        *
        * @param encodedData RLE 或 Delta 編碼的輸入數據
        * @param expectedDecodedPixels 預期的解碼後像素數據
        * @param p JPEG 參數
        */

    // Note RLE,和delta解碼各自獨立tester和Decoder需要修改

    def doJPEGDecodeChiselTest(encodedRLEData: Seq[Int], encodedDeltaData: Seq[Int], length: Int, p: JPEGParams): Unit = {
        test(new JPEGDecodeChisel(p)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        dut.clock.setTimeout(0)

        println("Starting Decode")
        
        // Initialize Input 
        val jpegDecoder = new jpegEncode(false, List.empty, 0)
        val expectedRLE = jpegDecoder.decodeRLE(encodedRLEData)
        //dut.io.in.valid.encodedRLE.poke(true.B)
        dut.io.in.valid.poke(true.B)
        for (i <- 0 until encodedRLEData.length) {
            dut.io.in.bits.encodedRLE(i).poke(encodedRLEData(i).S)
            //println(s"encodedRLE: ${dut.io.in.bits.encodedRLE(i).peek()}")

        }
        //println(encodedRLEData.mkString(", "))
        dut.io.length.poke(length.U)

    
        val expectedDelta = jpegDecoder.decodeDelta(encodedDeltaData)
        //dut.io.in.valid.encodedDelta.poke(true.B)
        for (i <- 0 until encodedDeltaData.length) {
            dut.io.in.bits.encodedDelta(i).poke(encodedDeltaData(i).S)
        }
        //println(encodedDelta.mkString(", "))

        // Testing RLE
        dut.clock.step()
        dut.clock.step(p.totalElements)
        for(i <- 0 until length){
            val actual = dut.io.decodeRLEOut(i).peek()
            dut.io.decodeRLEOut(i).expect(expectedRLE(i).S)
            //println(s"Expected: ${expectedRLE(i).S} Actual: $actual")
        }
        println("Passed RLE decode")

        // Testing Delta
        for(i <- 0 until p.totalElements){
            val actual = dut.io.decodeDeltaOut(i).peek()
            dut.io.decodeDeltaOut(i).expect(expectedDelta(i).S)
            //println(s"Expected: ${expectedDelta(i).S} Actual: $actual")
        }
        println("Passed Delta decode")


        // Testing inverse ZigZag
        val expectedInverseZigzag = jpegDecoder.zigzagDecode(expectedDelta)
        dut.clock.step()
        dut.clock.step(64)

        for(i <- 0 until p.numRows){
            for(j <- 0 until p.numCols){
                dut.io.inverseZigzagOut(i)(j).expect(expectedInverseZigzag(i)(j).S)
                val actual = dut.io.inverseZigzagOut(i)(j).peek()
                println(s"InverseZigzag Expected: ${expectedInverseZigzag(i)(j).S} Actual: $actual")
            }
        }
        println("Passed inverse Zigzag")


        // Testing inverse Quantization
        val expectedInverseQuant = jpegDecoder.inverseQuantization(expectedInverseZigzag, p.getQuantTable)
        dut.clock.step()
        dut.clock.step(64)

        for(i <- 0 until p.numRows){
            for(j <- 0 until p.numCols){
                dut.io.inverseQuantOut(i)(j).expect(expectedInverseQuant(i)(j).S)
                val actual = dut.io.inverseQuantOut(i)(j).peek()
                println(s"InverseQuant Expected: ${expectedInverseQuant(i)(j).S} Actual: $actual")
            }
        }
        println("Passed inverse Quantization")


        // Testing inverse DCT
        val expectedIDCT = jpegDecoder.IDCT(expectedInverseQuant)
        dut.clock.step(3)
        for (i <- 0 until 8) {
            for (j <- 0 until 8) {
                dut.io.inverseDCTOut(i)(j).expect(expectedIDCT(i)(j))
                val actual = dut.io.inverseDCTOut(i)(j).peek()
                println(s"IDCT Expected: ${expectedIDCT(i)(j)} Actual: $actual")
            }
        }
        println("Passed Inverse Discrete Cosine Transform\n")
    
        

        
        }
    }

  behavior of "JPEG decoder"

  it should "JPEG decoder test1, decoding by RLE" in {
    val p = JPEGParams(8, 8, 1, true)

    //DCTData.in1
    val RLEInputData = Seq(1, -23, 1, -3, 1, -19, 2, 4, 2, 0, 1, 2, 2, 1, 1, 0, 1, -1, 52, 0)
    val DeltaInputData = Seq(
        -23, 20, -16, 23, 0, -4, 0, 2, 
        -1, 0, -1, -1, 1, 0, 0, 0, 
        0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 
        0, 0, 0, 0, 0, 0, 0, 0, 
        0, 0, 0, 0, 0, 0, 0, 0, 
        0, 0, 0, 0, 0, 0, 0, 0, 
        0, 0, 0, 0, 0, 0, 0, 0
    )
    doJPEGDecodeChiselTest(RLEInputData, DeltaInputData, 64, p)
  }

  //it should "JPEG decoder test2, decoding by Delta" in {
   // val p = JPEGParams(8, 8, 1, false)

    //DCTData.in1
   // val RLEInputData = Seq(1, -23, 1, -3, 1, -19, 2, 4, 2, 0, 1, 2, 2, 1, 1, 0, 1, -1, 52, 0)
   // val DeltaInputData = Seq(
   //     -23, 20, -16, 23, 0, -4, 0, 2, 
   //     -1, 0, -1, -1, 1, 0, 0, 0, 
   //     0, 0, 0, 0, 0, 0, 0, 0,
   //     0, 0, 0, 0, 0, 0, 0, 0, 
   //     0, 0, 0, 0, 0, 0, 0, 0, 
   //     0, 0, 0, 0, 0, 0, 0, 0, 
   //     0, 0, 0, 0, 0, 0, 0, 0, 
   //     0, 0, 0, 0, 0, 0, 0, 0
   // )
   // doJPEGDecodeChiselTest(RLEInputData, DeltaInputData, 64, p)
  //}

  
}
