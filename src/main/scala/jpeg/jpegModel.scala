package jpeg
import scala.math.ceil
import scala.math.round
import scala.math.{cos, Pi}
import scala.math._

/**
  * 
  *
  * @param decompress
  * @param quantTable
  * @param encoding
  */
class jpegEncode(decompress: Boolean, quantTable: List[List[Int]], encoding: Int){
    /**
      * Helper func for ZigZag to calculate indicies
      *
      * @param n
      * @param isUp
      * @param i
      * @param j
      * @return 
      */
    def updateIndices(n: Int, isUp: Boolean, i: Int, j: Int): (Int, Int, Boolean) = {
        if (isUp) {
            if (j == n - 1) {
                (i + 1, j, false)
            } else if (i == 0) {
                (i, j + 1, false)
            } else {
                (i - 1, j + 1, isUp)
            }
        } else {
            if (i == n - 1) {
                (i, j + 1, true)
            } else if (j == 0) {
                (i + 1, j, true)
            } else {
                (i + 1, j - 1, isUp)
            }
        }
    }

    /**
      * Performs ZigZag parsing on 8x8 matrix 
      *
      * @param matrix Matrix to perform parsing on
      */
    def zigzagParse(matrix: Seq[Seq[Int]]): Seq[Int] = {
        var result: Seq[Int] = Seq.empty
        var i = 0
        var j = 0
        var isUp = true
        val matSize = matrix.length

        for (num <- 0 until matSize * matSize) {
            result = result :+ matrix(i)(j)
            val (newI, newJ, newIsUp) = updateIndices(matSize, isUp, i, j)
            i = newI
            j = newJ
            isUp = newIsUp
        }
        result
    }

    /**
      * Performs inverse ZigZag on 1d array to return it to 8x8 matrix
      *
      * @param data Input 1d array
      */
    def zigzagDecode(data: Seq[Int]): Seq[Seq[Int]] = {
        var i = 0
        var j = 0
        var isUp = true
        val matSize = sqrt(data.length).toInt
        var result = Array.ofDim[Int](matSize, matSize)

        for (elem <- 0 until matSize * matSize) {
            result(i)(j) = data(elem)
            val (newI, newJ, newIsUp) = updateIndices(matSize, isUp, i, j)
            i = newI
            j = newJ
            isUp = newIsUp
        }
        result.map(_.toSeq).toSeq
    }

    /**
      * Computes DCT matrix output from input matrix 
      *
      * @param matrix 8x8 matrix input of pixel values 
      */
    def DCT(matrix: Seq[Seq[Int]]): Seq[Seq[Double]] = {
        val rows = matrix.length
        val cols = matrix.headOption.map(_.length).getOrElse(0)

        require(rows == 8 && cols == 8, "Input matrix must be 8x8")

        val dctMatrix = Array.ofDim[Double](8, 8)

        for (u <- 0 until 8) {
            for (v <- 0 until 8) {
                var sum = 0.0
                for (i <- 0 until 8) {
                    for (j <- 0 until 8) {
                        val pixelValue = matrix(i)(j) - 128
                        val cosVal = cos((2 * i + 1) * u * Pi / 16) * cos((2 * j + 1) * v * Pi / 16) * 100
                        
                        val roundedCval = if (cosVal >= 0) floor(cosVal) else ceil(cosVal)
                        sum = sum + pixelValue * roundedCval
                    }
                }
                val alphaU = if (u == 0) floor((1.0 / math.sqrt(2)) * 100) else 100
                val alphaV = if (v == 0) floor((1.0 / math.sqrt(2)) * 100) else 100
                val scaledSum = alphaU * alphaV * sum / 4
                dctMatrix(u)(v) = floor(scaledSum)
            }
        }
        dctMatrix.map(_.toSeq).toSeq
    }

    /**
      * Prints given matrix to terminal
      *
      * @param matrix Matrix to print
      */
    def printMatrix(matrix: Seq[Seq[Double]]): Unit = {
        matrix.foreach(row => println(row.mkString(" ")))
    }
    
    /**
      * Takes a matrix and rounds its elements to two decimal places 
      *
      * @param matrix Matrix to be rounded
      */
    def roundToTwoDecimalPlaces(matrix: Seq[Seq[Double]]): Seq[Seq[Double]] = {
        matrix.map { row =>
            row.map { element =>
              BigDecimal(element).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
            }
        }
    }

    /**
      * Takes a matrix and rounds its elements to Int
      *
      * @param matrix Matrix to be rounded
      */
    def roundToInt(matrix: Seq[Seq[Double]]): Seq[Seq[Double]] = {
        matrix.map { row =>
            row.map { element =>
                Math.round(element).toDouble
            }
        }
    }    

    /**
      * Performs RLE Encoding
      *
      * @param data
      */
    def RLE(data: Seq[Int]): Seq[Int] = {
        var result = Seq[Int]()
        var current = data.head
        var count = 1
        
        for (i <- 1 until data.length) {
            if (data(i) == current) {
                count += 1
            } 
            else {
                result :+= count
                result :+= current
                current = data(i)
                count = 1
            }
        }
        
        result :+= count
        result :+= current
        
        result
    }

    /**
      * Performs RLE Decoding
      *
      * @param data
      */
    def decodeRLE(data: Seq[Int]): Seq[Int] = {
        var result = Seq[Int]()
        
        for (i <- 0 until data.length by 2) {
            val count = data(i)
            val value = data(i + 1)
            result ++= Seq.fill(count)(value)
        }
        
        result
    }

    /**
      * Performs Delta Encoding
      *
      * @param data Data to be encoded
      */
    def delta(data: Seq[Int]): Seq[Int] = {
        if (data.isEmpty) {
            Seq.empty[Int] 
        } 
        else {
            var result = Seq(data.head)
            var prev = data.head 

            for (i <- 1 until data.length) {
                val diff = data(i) - prev
                result :+= diff
                prev = data(i)
            }

            result
        }
    }

    /**
      * Performs Delta Decoding
      *
      * @param data Data to be decoded
      */
    def decodeDelta(data: Seq[Int]): Seq[Int] = {
        if (data.isEmpty) {
            Seq.empty[Int]
        } else {
            var result = Seq(data.head)
            var prev = data.head

            for (i <- 1 until data.length) {
                val original = data(i) + prev
                result :+= original
                prev = original
            }

            result
        }
    }

    /**
      * Performs Quantization after DCT
      *
      * @param data Data that is Quantified
      * @param quantTable Quantization table used for Quantization
      */
    def quantization(data: Seq[Seq[Int]], quantTable: Seq[Seq[Int]]): Seq[Seq[Int]] = {
        data.zip(quantTable).map { case (dataRow, quantRow) =>
                dataRow.zip(quantRow).map { case (d, q) =>
                val result = d.toDouble / q.toDouble
                if (result < 0) (round(-result) * -1).toInt else round(result).toInt
            }
        }
    }
    
    /**
      * Performs Scaled Quantization for DCT output
      *
      * @param data Data that is Quantified
      * @param quantTable Quantization table used for Quantization
      */
    def scaledQuantization(data: Seq[Seq[Int]], quantTable: Seq[Seq[Int]]): Seq[Seq[Int]] = {
        data.zip(quantTable).map { case (dataRow, quantRow) =>
                dataRow.zip(quantRow).map { case (d, q) =>
                val result = d.toDouble / 1000000.0 / q.toDouble
                if (result < 0) (round(-result) * -1).toInt else round(result).toInt
            }
        }
    }

    /**
      * Undos Quantization
      *
      * @param data Data that is Quantified
      * @param quantTable Quantization table used to unQuantify
      */
    def inverseQuantization(data: Seq[Seq[Int]], quantTable: Seq[Seq[Int]]): Seq[Seq[Int]] = {
        data.zip(quantTable).map { case (dataRow, quantRow) =>
            dataRow.zip(quantRow).map { case (d, q) =>
                (d.toDouble * q.toDouble).toInt
            }
        }
    }

}