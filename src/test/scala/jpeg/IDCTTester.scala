package jpeg

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

/**
  * 測試數據，用於驗證 Chisel IDCT 的實現
  */
class IDCTChiselTest extends AnyFlatSpec with ChiselScalatestTester {
  def doIDCTChiselTest(data: Seq[Seq[Int]]): Unit = {
    test(new IDCTChisel) { dut =>
      // Compute IDCT in SCALA for decomperssion
      val jpegDecoder = new jpegEncode(false, List.empty, 0)
      val expectedIDCT = jpegDecoder.IDCT(data)
      val convertedMatrix: Seq[Seq[SInt]] = expectedIDCT.map(row => row.map(value => value.toInt.S))
      
      // Set input valiid/ready bits
      dut.io.in.valid.poke(true.B)
      for (i <- 0 until 8) {
        for (j <- 0 until 8) {
          dut.io.in.bits.matrixIn(i)(j).poke(data(i)(j).toInt.S)
        }
      }

      dut.clock.step()  // 載入矩陣
      dut.clock.step()  // 進入計算狀態
      dut.clock.step()  // 計算狀態進行中
      dut.clock.step()  // 計算結束
      dut.clock.step()  // 確保 validOut 為 true
      dut.clock.step(15) // 更多的步驟以確保狀態轉換完全

      // Check IDCT output
      dut.io.idctOut.valid.expect(true.B)
      for (i <- 0 until 8) {
        for (j <- 0 until 8) {
          val actual = dut.io.idctOut.bits(i)(j).peek()
          //println(s"Expected: ${convertedMatrix(i)(j)} Actual: $actual")
          dut.io.idctOut.bits(i)(j).expect(convertedMatrix(i)(j))
        }
      }
    }
  }

  behavior of "IDCTChisel"

  it should "compute IDCT correctly in1" in {
    val inputMatrix = DCTData.scaledOut1
    doIDCTChiselTest(inputMatrix)
  }

  it should "compute IDCT correctly in2" in {
    val inputMatrix = DCTData.scaledOut2
    doIDCTChiselTest(inputMatrix)
  }

  it should "compute IDCT correctly in3" in {
    val inputMatrix = DCTData.scaledOut3
    doIDCTChiselTest(inputMatrix)
  }

  it should "compute IDCT correctly in4" in {
    val inputMatrix = DCTData.scaledOut4
    doIDCTChiselTest(inputMatrix)
  }

  it should "compute IDCT correctly in5" in {
    val inputMatrix = DCTData.scaledOut5
    doIDCTChiselTest(inputMatrix)
  }

  it should "compute IDCT correctly in6" in {
    val inputMatrix = DCTData.scaledOut6
    doIDCTChiselTest(inputMatrix)
  }

  it should "compute IDCT correctly in7" in {
    val inputMatrix = DCTData.scaledOut7
    doIDCTChiselTest(inputMatrix)
  }

  it should "compute IDCT correctly in8" in {
    val inputMatrix = DCTData.scaledOut8
    doIDCTChiselTest(inputMatrix)
  }
  
}
