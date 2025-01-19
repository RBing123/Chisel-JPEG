package jpeg

import chisel3._
import chisel3.internal.firrtl.Width
import chisel3.util._
import javax.imageio.ImageIO
import java.io.File

/**
  * JPEG Parameters for Chisel Modules
  *
  * @param givenRows Pixel Data in row dimensions
  * @param givenCols Pixel Data in col dimensions
  * @param qtChoice Quantization Table Choice, 0 for empty, 1 for Luminance, 2 for Chrominance
  * @param encodingChoice True for Run Length Encoding, False for Delta Encoding
  */
case class JPEGParams(val givenRows: Int, val givenCols: Int, val qtChoice: Int, val encodingChoice: Boolean = true){
    val numRows = 8
    val numCols = 8
    val totalElements = numRows * numCols
    val givenTotalElements = givenRows * givenCols
    val maxOutRLE = totalElements * 2

    val w16: Width = 16.W
    val w8: Width = 8.W

    /**
      * Helper function to return Quant table
      */
    def getQuantTable: Seq[Seq[Int]] = qtChoice match {
        case 0 => QuantizationTables.qEmpty
        case 1 => QuantizationTables.qt1
        case 2 => QuantizationTables.qt2
        case _ => throw new IllegalArgumentException("Invalid qtChoice, input 0, 1, or 2")
    }
}

object QuantizationTables {
    val qEmpty = Seq(Seq(0, 0, 0, 0, 0, 0, 0, 0),
                  Seq(0, 0, 0, 0, 0, 0, 0, 0),
                  Seq(0, 0, 0, 0, 0, 0, 0, 0),
                  Seq(0, 0, 0, 0, 0, 0, 0, 0),
                  Seq(0, 0, 0, 0, 0, 0, 0, 0),
                  Seq(0, 0, 0, 0, 0, 0, 0, 0),
                  Seq(0, 0, 0, 0, 0, 0, 0, 0),
                  Seq(0, 0, 0, 0, 0, 0, 0, 0))

    val qt1 = Seq(Seq(16, 11, 10, 16, 24, 40, 51, 61),
                  Seq(12, 12, 14, 19, 26, 58, 60, 55),
                  Seq(14, 13, 16, 24, 40, 57, 69, 56),
                  Seq(14, 17, 22, 29, 51, 87, 80, 62),
                  Seq(18, 22, 37, 56, 68, 109, 103, 77),
                  Seq(24, 35, 55, 64, 81, 104, 113, 92),
                  Seq(49, 64, 78, 87, 103, 121, 120, 101),
                  Seq(72, 99, 95, 98, 112, 100, 103, 99))

    val qt2 = Seq(Seq(17, 18, 24, 47, 99, 99, 99, 99),
                  Seq(18, 21, 26, 66, 99, 99, 99, 99),
                  Seq(24, 26, 56, 99, 99, 99, 99, 99),
                  Seq(47, 66, 99, 99, 99, 99, 99, 99),
                  Seq(99, 99, 99, 99, 99, 99, 99, 99),
                  Seq(99, 99, 99, 99, 99, 99, 99, 99),
                  Seq(99, 99, 99, 99, 99, 99, 99, 99),
                  Seq(99, 99, 99, 99, 99, 99, 99, 99))
}