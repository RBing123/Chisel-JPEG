package jpeg

import chisel3._
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.File
import chisel3.stage.{ChiselStage, ChiselGeneratorAnnotation}
import firrtl.options.TargetDirAnnotation 
// object Main {
//     def processImage(inputPath: String, outputPath: String, encoding: Boolean = true, quantChoice: Int = 1) = {
        
//         def readBMP(filepath: String): Seq[Seq[Int]] = {
//             val img = ImageIO.read(new File(filepath))
//             if (img == null) {
//                 throw new RuntimeException(s"Failed to read image: $filepath")
//             }
//             val width = img.getWidth
//             val height = img.getHeight
            
//             require(width % 8 == 0 && height % 8 == 0, 
//                     s"Image dimensions must be multiples of 8. Current size: ${width}x${height}")
            
//             println(s"Reading image: ${width}x${height}")
            
//             val data = for (y <- 0 until height) yield {
//                 for (x <- 0 until width) yield {
//                     val pixel = img.getRGB(x, y)
//                     val red = (pixel >> 16) & 0xff
//                     val green = (pixel >> 8) & 0xff
//                     val blue = pixel & 0xff
//                     (red * 0.299 + green * 0.587 + blue * 0.114).toInt
//                 }
//             }
//             data.toSeq
//         }

        
//         def splitInto8x8Blocks(data: Seq[Seq[Int]]): Seq[Seq[Seq[Int]]] = {
//             val height = data.length
//             val width = data.head.length
            
//             println(s"Splitting image into ${width/8}x${height/8} blocks")
            
//             val blocks = for {
//                 y <- 0 until height by 8
//                 x <- 0 until width by 8
//             } yield {
//                 for (i <- 0 until 8) yield {
//                     for (j <- 0 until 8) yield {
//                         data(y + i)(x + j)
//                     }
//                 }
//             }
//             blocks.map(_.toSeq).toSeq
//         }

//         try {
            
//             val imageData = readBMP(inputPath)
//             val blocks = splitInto8x8Blocks(imageData)
//             println(s"Total blocks: ${blocks.length}")

            
//             val params = JPEGParams(8, 8, quantChoice, encoding)
//             val jpegEncoder = new jpegEncode(false, List.empty, 0)

            
//             for ((block, index) <- blocks.zipWithIndex) {
//                 println(s"Processing block $index")
                
//                 val dctResult = jpegEncoder.DCT(block)
//                 // Quantization
//                 val quantResult = jpegEncoder.scaledQuantization(
//                     dctResult.map(_.map(_.toInt)), 
//                     params.getQuantTable
//                 )
//                 // Zigzag
//                 val zigzagResult = jpegEncoder.zigzagParse(quantResult)
//                 // encode
//                 if (encoding) {
//                     val encoded = jpegEncoder.RLE(zigzagResult)
//                     // println(s"Block $index RLE encoded size: ${encoded.length}")
//                 } else {
//                     val encoded = jpegEncoder.delta(zigzagResult)
//                     // println(s"Block $index Delta encoded size: ${encoded.length}")
//                 }
//             }
            
//             println("Image processing completed")
            
//         } catch {
//             case e: Exception => 
//                 println(s"Error processing image: ${e.getMessage}")
//                 e.printStackTrace()
//         }
//     }

//     def main(args: Array[String]): Unit = {
//         if (args.length < 1) {
//             println("Usage: Main <input_bmp> [output_file] [encoding_type] [quant_table]")
//             println("encoding_type: true for RLE, false for Delta")
//             println("quant_table: 1 or 2")
//             System.exit(1)
//         }

//         val inputFile = args(0)
//         val outputFile = if (args.length > 1) args(1) else "output.jpg"
//         val encoding = if (args.length > 2) args(2).toBoolean else true
//         val quantChoice = if (args.length > 3) args(3).toInt else 1

//         processImage(inputFile, outputFile, encoding, quantChoice)
//     }
// }
// object VerilogGenerator extends App {
  
//   val params = JPEGParams(8,8,1,true)
  
  
//   (new ChiselStage).execute(
//     args,
//     Seq(
//       ChiselGeneratorAnnotation(() => new JPEGEncodeChisel(params)),
//       TargetDirAnnotation("verilog")
//     )
//   )
// }