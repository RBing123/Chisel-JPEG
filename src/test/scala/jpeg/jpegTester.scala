package jpeg

import chisel3._
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
    def doJPEGEncodeChiselTest(bmpPath: String, p: JPEGParams): Unit = {
        val data = readbmp(bmpPath)
        val blocks = splitInto8x8Blocks(data)
        println(s"Total blocks: ${blocks.length}")

        test(new JPEGEncodeChisel(p)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
            dut.clock.setTimeout(0)
            var previousDcValue = 0
            for ((block, blockIndex) <- blocks.zipWithIndex) {
                println(s"Processing block $blockIndex")
                println("Starting Encode")
                
                // Testing DCT
                val jpegEncoder = new jpegEncode(false, List.empty, 0)
                val expectedDCT = jpegEncoder.DCT(block)
                val expectedDCTInt: Seq[Seq[Int]] = expectedDCT.map(_.map(_.toInt))
                val convertedMatrix: Seq[Seq[SInt]] = expectedDCT.map(row => row.map(value => value.toInt.S))
                
                // Initialize input
                dut.io.in.valid.poke(true.B)
                // Set input pixel data
                for (i <- 0 until 8) {
                    for (j <- 0 until 8) {
                        dut.io.in.bits.pixelDataIn(i)(j).poke(block(i)(j).S)
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
                println("Passed Zigzag")

            }
        }
    }

    behavior of "Top-level JPEG Encode Chisel"

    it should "Encode BMP using RLE - QT1" in {
        val p = JPEGParams(8, 8, 1, true)
        val bmpPath = "output.bmp"
        doJPEGEncodeChiselTest(bmpPath, p)
    }

    it should "Encode BMP using Delta Encoding - QT1" in {
        val p = JPEGParams(8, 8, 1, false)
        val bmpPath = "output.bmp"
        doJPEGEncodeChiselTest(bmpPath, p)
    }
}
