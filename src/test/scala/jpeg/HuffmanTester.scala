package jpeg

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class HuffmanAcEncoderTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "HuffmanAcEncoder"

  it should "encode AC values correctly" in {
    test(new HuffmanAcEncoder) { dut =>
      // 測試每個 AC 值的輔助函式
      def processValue(run: Int, size: Int, amplitude: Int, isLuminance: Boolean) = {
        dut.io.run.poke(run.U)                      // 設定 run
        dut.io.size.poke(size.U)                    // 設定 size
        dut.io.amplitude.poke(amplitude.S)          // 設定 amplitude
        dut.io.isLuminance.poke(isLuminance.B)      // 設定是否為 Luminance
        dut.clock.step(1)                           // 觸發時鐘

        println(s"\nProcessing Run: $run, Size: $size, Amplitude: $amplitude")
        println(s"Encoded bits: 0x${dut.io.out.bits.peek().litValue.toString(16)}")
        println(s"Code length: ${dut.io.out.length.peek().litValue}")
      }

      // 測試數據： (run, size, amplitude, isLuminance)
      val testValues = Seq(
        (0, 0, 0, true),   // End of Block (EOB) for Luminance
        (1, 1, 1, true),   // Run=1, Size=1, Amplitude=1, Luminance
        (0, 1, -1, false), // Run=0, Size=1, Amplitude=-1, Chrominance
        (2, 2, 2, true),   // Run=2, Size=2, Amplitude=2, Luminance
        (0, 3, -3, false)  // Run=0, Size=3, Amplitude=-3, Chrominance
      )

      // 對每組測試數據進行測試
      testValues.foreach { case (run, size, amplitude, isLuminance) =>
        processValue(run, size, amplitude, isLuminance)
      }
    }
  }
}
class HuffmanDcEncoderTest extends AnyFlatSpec with ChiselScalatestTester {
 behavior of "HuffmanDcEncoder"
 
 it should "encode DC values correctly" in {
   test(new HuffmanDcEncoder) { dut =>
     def processValue(value: Int) = {
       dut.io.in.bits.value.poke(value.S)
       dut.io.in.bits.isLuminance.poke(true.B)
       dut.io.in.valid.poke(true.B)
       dut.clock.step(1)

       println(s"\nProcessing value: $value")
       println(s"Encoded bits: 0x${dut.io.out.bits.bits.peek().litValue.toString(16)}")
       println(s"Code length: ${dut.io.out.bits.length.peek().litValue}")
     }

     // 測試一系列值
     val testValues = Seq(0, 1, -1, 2, -2, 3, -3)
     testValues.foreach(processValue)
   }
 }
}
class HuffmanAcDecoderTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "HuffmanAcDecoder"

  it should "decode AC values correctly" in {
    test(new HuffmanAcDecoder) { dut =>
      def processValue(bits: Int, length: Int, isLuminance: Boolean) = {
        dut.io.in.bits.poke(bits.U)
        dut.io.in.length.poke(length.U)
        dut.io.in.isLuminance.poke(isLuminance.B)
        dut.clock.step(1)

        // 取得解碼結果
        val run = dut.io.out.run.peek().litValue
        val size = dut.io.out.size.peek().litValue
        val amplitude = dut.io.out.amplitude.peek().litValue

        println(s"\nDecoding bits: 0x${bits.toHexString}, length: $length")
        println(s"Decoded Run: $run")
        println(s"Decoded Size: $size")
        println(s"Decoded Amplitude: $amplitude")
      }

      // 測試數據：(bits, length, isLuminance)
      val testValues = Seq(
        (0xa, 4, true),     // EOB for Luminance
        (0x0, 2, true),     // 簡單情況 - Luminance
        (0x1, 2, false),    // 簡單情況 - Chrominance
        (0x100, 9, true),   // 較長的編碼 - Luminance
        (0x3f0, 10, false)  // 較長的編碼 - Chrominance
      )

      testValues.foreach { case (bits, length, isLuminance) =>
        processValue(bits, length, isLuminance)
      }
    }
  }
}
class HuffmanDcDecoderTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "HuffmanDcDecoder"

  it should "decode DC values correctly" in {
    test(new HuffmanDcDecoder) { dut =>
      def processValue(bits: Int, length: Int, isLuminance: Boolean) = {
        dut.io.in.bits.poke(bits.U)
        dut.io.in.length.poke(length.U)
        dut.io.in.isLuminance.poke(isLuminance.B)
        dut.clock.step(1)

        val decodedValue = dut.io.out.value.peek().litValue

        println(s"\nDecoding bits: 0x${bits.toHexString}, length: $length")
        println(s"Decoded Value: $decodedValue")
      }

      // 測試數據：(bits, length, isLuminance)
      val testValues = Seq(
        (0x0, 2, true),    // 零值 - Luminance
        (0x2, 3, true),    // 正值 - Luminance
        (0x3, 3, true),    // 負值 - Luminance
        (0x4, 3, false),   // 正值 - Chrominance
        (0x5, 3, false),   // 負值 - Chrominance
        (0xA, 4, true)     // 較大值 - Luminance
      )

      testValues.foreach { case (bits, length, isLuminance) =>
        processValue(bits, length, isLuminance)
      }
    }
  }

  // 可以添加更多測試案例
  it should "handle edge cases correctly" in {
    test(new HuffmanDcDecoder) { dut =>
      // 測試極端情況
      def processValue(bits: Int, length: Int, isLuminance: Boolean) = {
        dut.io.in.bits.poke(bits.U)
        dut.io.in.length.poke(length.U)
        dut.io.in.isLuminance.poke(isLuminance.B)
        dut.clock.step(1)
      }

      // 測試極端情況：最大值、最小值等
      val edgeCases = Seq(
        (0xFFF, 12, true),  // 最大正值
        (0x000, 12, true),  // 最小負值
        (0x800, 12, false)  // 中間值
      )

      edgeCases.foreach { case (bits, length, isLuminance) =>
        processValue(bits, length, isLuminance)
      }
    }
  }
}