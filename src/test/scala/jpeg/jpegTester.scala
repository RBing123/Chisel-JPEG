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
    def readbmp(filepath: String): Seq[Seq[Int]]={
        val img = ImageIO.read(new File(filepath))
        if (img == null) {
            throw new RuntimeException(s"Failed to read image: $filepath")
        }
        val width = img.getWidth
        val height = img.getHeight
        require(width % 8 == 0 && height % 8 == 0, 
                s"Image dimensions must be multiples of 8. Current size: ${width}x${height}")
        val data = for (y <- 0 until height) yield {
            for (x <- 0 until width) yield {
                val pixel = img.getRGB(x, y)
                val red = (pixel >> 16) & 0xff
                val green = (pixel >> 8) & 0xff
                val blue = pixel & 0xff
                
                (red * 0.299 + green * 0.587 + blue * 0.114).toInt
            }
        }
        data.toSeq
    }
    def splitInto8x8Blocks(data: Seq[Seq[Int]]): Seq[Seq[Seq[Int]]] = {
        val height = data.length
        val width = data.head.length
        
        val blocks = for {
            y <- 0 until height by 8
            x <- 0 until width by 8
        } yield {
            for (i <- 0 until 8) yield {
                for (j <- 0 until 8) yield {
                    data(y + i)(x + j)
                }
            }
        }
        blocks.map(_.toSeq).toSeq
    }
    def doJPEGEncodeChiselTest(data: Seq[Seq[Int]], p: JPEGParams): Unit = {
        test(new JPEGEncodeChisel(p)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
            dut.clock.setTimeout(0)
            val outputDir = "hw_output"
            new java.io.File(outputDir).mkdirs()
            
            println("Starting Encode")
            // Testing DCT
            val jpegEncoder = new jpegEncode(false, List.empty, 0)
            val expectedDCT = jpegEncoder.DCT(data)
            val expectedDCTInt: Seq[Seq[Int]] = expectedDCT.map(_.map(_.toInt))
            val convertedMatrix: Seq[Seq[SInt]] = expectedDCT.map(row => row.map(value => value.toInt.S))

            // Initialize input
            dut.io.in.valid.poke(true.B)
            // Set input pixel data
            for (i <- 0 until p.givenRows) {
                for (j <- 0 until p.givenCols) {
                dut.io.in.bits.pixelDataIn(i)(j).poke(data(i)(j).S)
                }
            }

            // Take step
            dut.clock.step(3)
            for (i <- 0 until 8) {
                for (j <- 0 until 8) {
                    dut.io.dctOut(i)(j).expect(convertedMatrix(i)(j))
                }
            }
            println("Passed Discrete Cosine Transform")
            
            // Testing Quant
            val expectedQuant = jpegEncoder.scaledQuantization(expectedDCTInt, p.getQuantTable)
            dut.clock.step()
            dut.clock.step(64)
            for (r <- 0 until p.numRows) {
                for (c <- 0 until p.numCols) {
                    dut.io.quantOut(r)(c).expect(expectedQuant(r)(c).S)
                }
            }
            println("Passed Quantization")

            // Testing Zigzag
            val expectedZigzag = jpegEncoder.zigzagParse(expectedQuant)
            dut.clock.step()
            dut.clock.step(p.totalElements)

            for(i <- 0 until expectedZigzag.length){
                dut.io.zigzagOut(i).expect(expectedZigzag(i).S)
            }
            println("\n=== Zigzag Output ===")
            println("Index | Value")
            println("--------------")
            for (i <- 0 until p.totalElements) {
                val zigzagValue = dut.io.zigzagOut(i).peek().litValue
                println(f"$i%3d   | $zigzagValue")
            }
            println("Passed Zigzag")
            
            // Testing Encode
            if(p.encodingChoice){
    val outputDir = new java.io.File("hw_output")
    outputDir.mkdirs()
    val outputFilePath = s"${outputDir.getPath}/rle_output.txt"
    val fw = new java.io.FileWriter(outputFilePath)
    val bw = new java.io.BufferedWriter(fw)
    
    val expectedEncode = jpegEncoder.RLE(expectedZigzag)
    dut.clock.step()
    dut.clock.step(p.totalElements)

    println("\n=== RLE Encoding Output ===")
    // Check the output
    var i=0
    while (i < p.maxOutRLE - 1 && i < expectedEncode.length - 1) {
        val runLength = dut.io.encodedRLE(i).peek().litValue
        val rleValue = dut.io.encodedRLE(i+1).peek().litValue
        
        // Skip writing if both values are 0 (except for the first pair)
        if (i == 0 || runLength != 0 || rleValue != 0) {
            println(f"$runLength%d | $rleValue")
            bw.write(f"$runLength%d $rleValue\n")
        }
        
        dut.io.encodedRLE(i).expect(expectedEncode(i).S)
        dut.io.encodedRLE(i + 1).expect(expectedEncode(i + 1).S) 
        
        i+=2
    }
    
    bw.close()
    fw.close()
    println(s"RLE Encoding output written to: $outputFilePath")
    println("Passed Run Length Encoding")
}
            else{
                val outputFilePath = s"ch" +
                  s"chisel_output/dpcm_output_${System.currentTimeMillis()}.txt"
                val fw = new java.io.FileWriter(outputFilePath)
                val bw = new java.io.BufferedWriter(fw)
                val expectedEncode = jpegEncoder.delta(expectedZigzag)
                dut.clock.step()
                dut.clock.step(p.totalElements)

                // Check the output
                println("\n=== DPCM Encoding Output ===")
                
                for (i <- 0 until p.totalElements) {
                    val deltaValue = dut.io.encodedDelta(i).peek().litValue
                    println(f"$i%d | $deltaValue%d")
                    bw.write(f"$i%d $deltaValue%d\n")
                    dut.io.encodedDelta(i).expect(expectedEncode(i).S)
                }
                bw.close()
                fw.close()
                println(s"DPCM Encoding output written to: $outputFilePath")
                println("Passed Delta Encoding")
            }
            println("Completed Encoding\n")
        }
    }

    
    behavior of "Top-level JPEG Encode Chisel"
    it should "Encodes using RLE - IN1 - QT1" in {
        val p = JPEGParams(8, 8, 1, true)
        val inputData = DCTData.in1 
        doJPEGEncodeChiselTest(inputData, p)
    }

    it should "Encodes using Delta Encoding - IN1 - QT1" in {
        val p = JPEGParams(8, 8, 1, false)
        val inputData = DCTData.in1 
        doJPEGEncodeChiselTest(inputData, p)
    }
}