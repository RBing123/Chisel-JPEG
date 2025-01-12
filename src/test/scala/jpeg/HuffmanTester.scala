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
