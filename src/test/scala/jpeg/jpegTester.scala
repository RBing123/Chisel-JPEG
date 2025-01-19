package jpeg

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import java.beans.beancontext.BeanContextChildSupport
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.File
import chisel3.util.log2Ceil
import scala.io.Source

/**
  * Top level test harness
  */
class JPEGEncodeChiselTest extends AnyFlatSpec with ChiselScalatestTester {
    /**
        * Tests the functionality of jpegEncodeChisel
        *
        * @param data Input pixel data
        * @param encoded Expected encoded output
        */
    def readDataFromFile(fileName: String): Seq[Seq[Int]] = {
        val lines = Source.fromFile(fileName).getLines().toSeq
        require(lines.size == 64, s"File $fileName must contain 64 lines")
        val data = lines.map(_.toInt)
        data.grouped(8).toSeq
    }
    def doJPEGEncodeChiselTest(dataYFile: String, p: JPEGParams): Unit = {
        val dataY = readDataFromFile(dataYFile)
        // dataY.foreach(row => println(row.mkString(", ")))
        test(new JPEGEncodeChisel(p)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
            dut.clock.setTimeout(0)
            val outputDir = "hw_output"
            new java.io.File(outputDir).mkdirs()
            
            println("Starting Encode")
            dut.io.in.valid.poke(true.B)
            for (i <- 0 until 8; j <- 0 until 8) {
                dut.io.in.bits.yComponent(i)(j).poke(dataY(i)(j).S)
            }
            dut.clock.step(200)
            // println("Y Component DCT Output:")
            // for (i <- 0 until 8; j <- 0 until 8) {
            //     println(s"Y($i, $j): ${dut.io.dctOutY(i)(j).peek().litValue}")
            // }
            println("\n=== Quantization Output ===")
            for (i <- 0 until p.numRows) {
                for (j <- 0 until p.numCols) {
                    val quantValue = dut.io.quantOutY(i)(j).peek().litValue
                    print(f"$quantValue%5d ")
                }
                println()
            }
            // println("\n=== Zigzag Output ===")
            // for (i <- 0 until p.totalElements) {
            //     val zigzagValue = dut.io.zigzagOutY(i).peek().litValue
            //     println(s"Index $i: $zigzagValue")
            // }
            // println("\n=== RLE Encoding Output ===")
            // for (i <- 0 until p.maxOutRLE) {
            //     val runLength = dut.io.encodedRLEY(i).peek().litValue
            //     println(s"Index $i: $runLength")
            // }
            // println("\n=== RLE Encoding Output ===")
            
            val componentDir = s"${outputDir}/RLE"
            new File(componentDir).mkdirs()
            val componentType = dataYFile match {
                case s if s.contains("y_block") => "Y"
                case s if s.contains("cb_block") => "Cb"
                case s if s.contains("cr_block") => "Cr"
            }
            val blockNum = dataYFile.split("_block_")(1).split("\\.")(0)
            val rleOutputFile = s"${componentDir}/${componentType}_block_${blockNum}_rle.txt"
            val writer = new java.io.FileWriter(rleOutputFile)
            try {
                var i = 0
                while (i < p.maxOutRLE - 1) {
                    val runLength = dut.io.encodedRLEY(i).peek().litValue
                    val rleValue = dut.io.encodedRLEY(i+1).peek().litValue
                    
                    if (i == 0 || runLength != 0 || rleValue != 0) {
                        writer.write(f"$runLength $rleValue\n")
                    }
                    i += 2
                }
                println(s"RLE Encoding output written to: $rleOutputFile")
            } finally {
                writer.close()
            }
            println("\n=== Delta Encoding Output ===")
            // for (i <- 0 until p.totalElements) {
            //     val deltaValue = dut.io.encodedDeltaY(i).peek().litValue
            //     println(s"Index $i: $deltaValue")
            // }
            val deltaDir = s"${outputDir}/Delta"
            new File(deltaDir).mkdirs()
            val deltaOutputFile = s"${deltaDir}/${componentType}_block_${blockNum}_delta.txt"
            val deltaWriter = new java.io.FileWriter(deltaOutputFile)
            try {
                val dcDiff = dut.io.encodedDeltaY(0).peek().litValue
                deltaWriter.write(f"$dcDiff\n")
                println(s"Delta Encoding output written to: $deltaOutputFile")
            } finally {
                deltaWriter.close()
            }
            // Write DCT output
        val dctOutputFile = s"${outputDir}/chisel_dct_${componentType}.txt"
        val dctWriter = new java.io.FileWriter(dctOutputFile)
        try {
            for (i <- 0 until 8; j <- 0 until 8) {
                val dctValue = dut.io.dctOutY(i)(j).peek().litValue
                dctWriter.write(s"$dctValue\n")
            }
        } finally {
            dctWriter.close()
        }

        // Write Quantization output
        val quantOutputFile = s"${outputDir}/chisel_quant_${componentType}.txt"
        val quantWriter = new java.io.FileWriter(quantOutputFile)
        try {
            for (i <- 0 until p.numRows; j <- 0 until p.numCols) {
                val quantValue = dut.io.quantOutY(i)(j).peek().litValue
                quantWriter.write(s"$quantValue\n")
            }
        } finally {
            quantWriter.close()
        }

        // Write Zigzag output
        val zigzagOutputFile = s"${outputDir}/chisel_zigzag_${componentType}.txt"
        val zigzagWriter = new java.io.FileWriter(zigzagOutputFile)
        try {
            for (i <- 0 until p.totalElements) {
                val zigzagValue = dut.io.zigzagOutY(i).peek().litValue
                zigzagWriter.write(s"$zigzagValue\n")
            }
        } finally {
            zigzagWriter.close()
        }
//             // Testing DCT
//             val jpegEncoder = new jpegEncode(false, List.empty, 0)
//             val expectedDCT = jpegEncoder.DCT(data)
//             val expectedDCTInt: Seq[Seq[Int]] = expectedDCT.map(_.map(_.toInt))
//             val convertedMatrix: Seq[Seq[SInt]] = expectedDCT.map(row => row.map(value => value.toInt.S))

//             // Initialize input
//             dut.io.in.valid.poke(true.B)
//             // Set input pixel data
//             for (i <- 0 until p.givenRows) {
//                 for (j <- 0 until p.givenCols) {
//                 dut.io.in.bits.pixelDataIn(i)(j).poke(data(i)(j).S)
//                 }
//             }

//             // Take step
//             dut.clock.step(3)
//             for (i <- 0 until 8) {
//                 for (j <- 0 until 8) {
//                     dut.io.dctOut(i)(j).expect(convertedMatrix(i)(j))
//                 }
//             }
//             println("Passed Discrete Cosine Transform")
            
//             // Testing Quant
//             val expectedQuant = jpegEncoder.scaledQuantization(expectedDCTInt, p.getQuantTable)
//             dut.clock.step()
//             dut.clock.step(64)
//             for (r <- 0 until p.numRows) {
//                 for (c <- 0 until p.numCols) {
//                     dut.io.quantOut(r)(c).expect(expectedQuant(r)(c).S)
//                 }
//             }
//             println("Passed Quantization")

//             // Testing Zigzag
//             val expectedZigzag = jpegEncoder.zigzagParse(expectedQuant)
//             dut.clock.step()
//             dut.clock.step(p.totalElements)

//             for(i <- 0 until expectedZigzag.length){
//                 dut.io.zigzagOut(i).expect(expectedZigzag(i).S)
//             }
//             println("\n=== Zigzag Output ===")
//             println("Index | Value")
//             println("--------------")
//             for (i <- 0 until p.totalElements) {
//                 val zigzagValue = dut.io.zigzagOut(i).peek().litValue
//                 println(f"$i%3d   | $zigzagValue")
//             }
//             println("Passed Zigzag")
            
//             // Testing Encode
//             if(p.encodingChoice){
//     val outputDir = new java.io.File("hw_output")
//     outputDir.mkdirs()
//     val outputFilePath = s"${outputDir.getPath}/rle_output.txt"
//     val fw = new java.io.FileWriter(outputFilePath)
//     val bw = new java.io.BufferedWriter(fw)
    
//     val expectedEncode = jpegEncoder.RLE(expectedZigzag)
//     dut.clock.step()
//     dut.clock.step(p.totalElements)

//     println("\n=== RLE Encoding Output ===")
//     // Check the output
//     var i=0
//     while (i < p.maxOutRLE - 1 && i < expectedEncode.length - 1) {
//         val runLength = dut.io.encodedRLE(i).peek().litValue
//         val rleValue = dut.io.encodedRLE(i+1).peek().litValue
        
//         // Skip writing if both values are 0 (except for the first pair)
//         if (i == 0 || runLength != 0 || rleValue != 0) {
//             println(f"$runLength%d | $rleValue")
//             bw.write(f"$runLength%d $rleValue\n")
//         }
        
//         dut.io.encodedRLE(i).expect(expectedEncode(i).S)
//         dut.io.encodedRLE(i + 1).expect(expectedEncode(i + 1).S) 
        
//         i+=2
//     }
    
//     bw.close()
//     fw.close()
//     println(s"RLE Encoding output written to: $outputFilePath")
//     println("Passed Run Length Encoding")
// }
//             else{
//                 val outputFilePath = s"ch" +
//                   s"chisel_output/dpcm_output_${System.currentTimeMillis()}.txt"
//                 val fw = new java.io.FileWriter(outputFilePath)
//                 val bw = new java.io.BufferedWriter(fw)
//                 val expectedEncode = jpegEncoder.delta(expectedZigzag)
//                 dut.clock.step()
//                 dut.clock.step(p.totalElements)

//                 // Check the output
//                 println("\n=== DPCM Encoding Output ===")
                
//                 for (i <- 0 until p.totalElements) {
//                     val deltaValue = dut.io.encodedDelta(i).peek().litValue
//                     println(f"$i%d | $deltaValue%d")
//                     bw.write(f"$i%d $deltaValue%d\n")
//                     dut.io.encodedDelta(i).expect(expectedEncode(i).S)
//                 }
//                 bw.close()
//                 fw.close()
//                 println(s"DPCM Encoding output written to: $outputFilePath")
//                 println("Passed Delta Encoding")
//             }
//             println("Completed Encoding\n")
        }
    }
    behavior of "Top-level JPEG Encode Chisel"

    it should "Encodes Y" in {
        val p = JPEGParams(8, 8, 1, true)
        val yDataFile = "hw_output/y_block_0.txt"
        
        doJPEGEncodeChiselTest(yDataFile, p)
    }
    it should "Encodes Cb" in {
        val p = JPEGParams(8, 8, 2, true)
        val yDataFile = "hw_output/cb_block_0.txt"
        
        doJPEGEncodeChiselTest(yDataFile, p)
    }
    it should "Encodes Cr" in {
        val p = JPEGParams(8, 8, 2, true)
        val yDataFile = "hw_output/cr_block_0.txt"
        doJPEGEncodeChiselTest(yDataFile, p)
    }
    
    // behavior of "Top-level JPEG Encode Chisel"
    // it should "Encodes using RLE - IN1 - QT1" in {
    //     val p = JPEGParams(8, 8, 1, true)
    //     val inputData = DCTData.in1 
    //     doJPEGEncodeChiselTest(inputData, p)
    // }

    // it should "Encodes using Delta Encoding - IN1 - QT1" in {
    //     val p = JPEGParams(8, 8, 1, false)
    //     val inputData = DCTData.in1 
    //     doJPEGEncodeChiselTest(inputData, p)
    // }
}