package jpeg

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import java.beans.beancontext.BeanContextChildSupport

object ZigZagParseData {
    val in2x2 = Seq(Seq(1, 2),
                    Seq(3, 4))

    val in3x3 = Seq(Seq(1, 2, 6),
                    Seq(3, 5, 7),
                    Seq(4, 8, 9))
    
    val in4x4 = Seq(Seq(10, 11, 12, 13),
                    Seq(14, 15, 16, 17),
                    Seq(18, 19, 20, 21),
                    Seq(22, 23, 24, 25))

    val in8x8 = Seq(Seq(10, 11, 12, 13, 14, 15, 16, 17),
                    Seq(18, 19, 20, 21, 22, 23, 24, 25),
                    Seq(26, 27, 28, 29, 30, 31, 32, 33),
                    Seq(34, 35, 36, 37, 38, 39, 40, 41),
                    Seq(42, 43, 44, 45, 46, 47, 48, 49),
                    Seq(50, 51, 52, 53, 54, 55, 56, 57),
                    Seq(58, 59, 60, 61, 62, 63, 64, 65),
                    Seq(66, 67, 68, 69, 70, 71, 72, 73))

    val out2x2 = Seq(1, 2, 3, 4)
    val out3x3 = Seq(1, 2, 3, 4, 5, 6, 7, 8, 9)
    val out4x4 = Seq(10, 11, 14, 18, 15, 12, 13, 16, 19, 22, 23, 20, 17, 21, 24, 25)
    val out8x8 = Seq(10, 11, 18, 26, 19, 12, 13, 20, 27, 34, 42, 35, 28, 21, 14, 15, 22, 29, 36, 43, 50, 58, 51, 44, 37, 30, 23, 16, 17, 24, 31, 38, 45, 52, 59, 66, 67, 60, 53, 46, 39, 32, 25, 33, 40, 47, 54, 61, 68, 69, 62, 55, 48, 41, 49, 56, 63, 70, 71, 64, 57, 65, 72, 73)
}

object RLEData {
    val in1 = Seq(1, 1, 1, 2, 2, 3, 3, 3, 3)
    val in2 = Seq(5, 5, 5, 5, 3, 3, 1, 1, 1)
    val in3 = Seq(4, 4, 4, 4, 4)
    val in4 = Seq(1, 2, 3, 4 ,5)
    
    val out1 = Seq(3, 1, 2, 2, 4, 3)
    val out2 = Seq(4, 5, 2, 3, 3, 1)
    val out3 = Seq(5, 4)
    val out4 = Seq(1, 1, 1, 2, 1, 3, 1, 4, 1, 5)
}

object deltaData {
    val in1 = Seq(1, 3, 6, 10)
    val in2 = Seq(10, 7, 4, 2)
    val in3 = Seq(5, 5, 5, 5)
    val in4 = Seq.empty[Int]
    val in5 = Seq(100)

    val out1 = Seq(1, 2, 3, 4)
    val out2 = Seq(10, -3, -3, -2)
    val out3 = Seq(5, 0, 0, 0)
    val out4 = Seq.empty[Int]
    val out5 = Seq(100)

}


object QuantizationData {
    val in1 = Seq(Seq(-415, -33, -58, 35, 58, -51, -15, -12),
                  Seq(5, -34, 49, 18, 27, 1, -5, 3),
                  Seq(-46, 14, 80, -35, -50, 19, 7, -18),
                  Seq(-53, 21, 34, -20, 2, 34, 36, 12),
                  Seq(9, -2, 9, -5, -32, -15, 45, 37),
                  Seq(-8, 15, -16, 7, -8, 11, 4, 7),
                  Seq(19, -28, -2, -26, -2, 7, -44, -21),
                  Seq(18, 25, -12, -44, 35, 48, -37, -3))

    val in2 = Seq(Seq(100, -33, -58, 35, 58, -51, -15, -12),
                  Seq(5, -34, 49, 18, 27, 1, -5, 3),
                  Seq(-46, 14, 80, -35, -50, 19, 7, -18),
                  Seq(-53, 21, 34, -20, 2, 34, 36, 12),
                  Seq(9, -2, 9, -5, -32, -15, 45, 37),
                  Seq(-8, 15, -16, 7, -8, 11, 4, 7),
                  Seq(19, -28, -2, -26, -2, 7, -44, -21),
                  Seq(18, 25, -12, -44, 35, 48, -37, -3))

    val in3 = Seq(Seq(120, -40, 80, 60, 90, -50, -30, 45),
                  Seq(10, -30, 45, 25, 35, 15, -20, 30),
                  Seq(-25, 35, 55, -15, 20, 40, 50, -10),
                  Seq(5, -20, 30, 40, 26, 10, -15, 20),
                  Seq(-10, 15, 20, 10, -5, 25, 30, -10),
                  Seq(60, -30, -35, 20, 10, 15, -25, 35),
                  Seq(30, -25, 40, 15, -10, 20, 25, -20),
                  Seq(25, 15, -10, -20, 30, 45, -35, -15))
                  
    val out1qt1 = Seq(Seq(-26, -3, -6, 2, 2, -1, 0, 0),
                      Seq(0, -3, 4, 1, 1, 0, 0, 0),
                      Seq(-3, 1, 5, -1, -1, 0, 0, 0),
                      Seq(-4, 1, 2, -1, 0, 0, 0, 0),
                      Seq(1, 0, 0, 0, 0, 0, 0, 0),
                      Seq(0, 0, 0, 0, 0, 0, 0, 0),
                      Seq(0, 0, 0, 0, 0, 0, 0, 0),
                      Seq(0, 0, 0, 0, 0, 0, 0, 0))

    val out1qt2 = Seq(Seq(-24, -2, -2, 1, 1, -1, 0, 0),
                      Seq(0, -2, 2, 0, 0, 0, 0, 0),
                      Seq(-2, 1, 1, 0, -1, 0, 0, 0),
                      Seq(-1, 0, 0, 0, 0, 0, 0, 0),
                      Seq(0, 0, 0, 0, 0, 0, 0, 0),
                      Seq(0, 0, 0, 0, 0, 0, 0, 0),
                      Seq(0, 0, 0, 0, 0, 0, 0, 0),
                      Seq(0, 0, 0, 0, 0, 0, 0, 0))

    val out2qt1 = Seq(Seq(6, -3, -6, 2, 2, -1, 0, 0),
                     Seq(0, -3, 4, 1, 1, 0, 0, 0),
                     Seq(-3, 1, 5, -1, -1, 0, 0, 0),
                     Seq(-4, 1, 2, -1, 0, 0, 0, 0),
                     Seq(1, 0, 0, 0, 0, 0, 0, 0),
                     Seq(0, 0, 0, 0, 0, 0, 0, 0),
                     Seq(0, 0, 0, 0, 0, 0, 0, 0),
                     Seq(0, 0, 0, 0, 0, 0, 0, 0))

    val out2qt2 = Seq(Seq(6, -2, -2, 1, 1, -1, 0, 0),
                      Seq(0, -2, 2, 0, 0, 0, 0, 0),
                      Seq(-2, 1, 1, 0, -1, 0, 0, 0),
                      Seq(-1, 0, 0, 0, 0, 0, 0, 0),
                      Seq(0, 0, 0, 0, 0, 0, 0, 0),
                      Seq(0, 0, 0, 0, 0, 0, 0, 0),
                      Seq(0, 0, 0, 0, 0, 0, 0, 0),
                      Seq(0, 0, 0, 0, 0, 0, 0, 0))

    val out3qt1 = Seq(Seq(8, -4, 8, 4, 4, -1, -1, 1),
                      Seq(1, -3, 3, 1, 1, 0, 0, 1),
                      Seq(-2, 3, 3, -1, 1, 1, 1, 0),
                      Seq(0, -1, 1, 1, 1, 0, 0, 0),
                      Seq(-1, 1, 1, 0, 0, 0, 0, 0),
                      Seq(3, -1, -1, 0, 0, 0, 0, 0),
                      Seq(1, 0, 1, 0, 0, 0, 0, 0),
                      Seq(0, 0, 0, 0, 0, 0, 0, 0))

    val out3qt2 = Seq(Seq(7, -2, 3, 1, 1, -1, 0, 0),
                      Seq(1, -1, 2, 0, 0, 0, 0, 0),
                      Seq(-1, 1, 1, 0, 0, 0, 1, 0),
                      Seq(0, 0, 0, 0, 0, 0, 0, 0),
                      Seq(0, 0, 0, 0, 0, 0, 0, 0),
                      Seq(1, 0, 0, 0, 0, 0, 0, 0),
                      Seq(0, 0, 0, 0, 0, 0, 0, 0),
                      Seq(0, 0, 0, 0, 0, 0, 0, 0))
}

object QuantizationDecodeData {
    val in1 = Seq(Seq(-26, -3, -6, 2, 2, -1, 0, 0), 
                  Seq(0, -3, 4, 1, 1, 0, 0, 0), 
                  Seq(-3, 1, 5, -1, -1, 0, 0, 0), 
                  Seq(-4, 1, 2, -1, 0, 0, 0, 0), 
                  Seq(1, 0, 0, 0, 0, 0, 0, 0), 
                  Seq(0, 0, 0, 0, 0, 0, 0, 0), 
                  Seq(0, 0, 0, 0, 0, 0, 0, 0), 
                  Seq(0, 0, 0, 0, 0, 0, 0, 0))
                  
    val in2 = Seq(Seq(6, -3, -6, 2, 2, -1, 0, 0), 
                  Seq(0, -3, 4, 1, 1, 0, 0, 0), 
                  Seq(-3, 1, 5, -1, -1, 0, 0, 0), 
                  Seq(-4, 1, 2, -1, 0, 0, 0, 0), 
                  Seq(1, 0, 0, 0, 0, 0, 0, 0), 
                  Seq(0, 0, 0, 0, 0, 0, 0, 0), 
                  Seq(0, 0, 0, 0, 0, 0, 0, 0), 
                  Seq(0, 0, 0, 0, 0, 0, 0, 0))

    val in3 = Seq(Seq(8, -4, 8, 4, 4, -1, -1, 1), 
                  Seq(1, -3, 3, 1, 1, 0, 0, 1), 
                  Seq(-2, 3, 3, -1, 1, 1, 1, 0), 
                  Seq(0, -1, 1, 1, 1, 0, 0, 0), 
                  Seq(-1, 1, 1, 0, 0, 0, 0, 0), 
                  Seq(3, -1, -1, 0, 0, 0, 0, 0), 
                  Seq(1, 0, 1, 0, 0, 0, 0, 0), 
                  Seq(0, 0, 0, 0, 0, 0, 0, 0))

    val out1qt1 = Seq(Seq(-416, -33, -60, 32, 48, -40, 0, 0), 
                      Seq(0, -36, 56, 19, 26, 0, 0, 0), 
                      Seq(-42, 13, 80, -24, -40, 0, 0, 0), 
                      Seq(-56, 17, 44, -29, 0, 0, 0, 0), 
                      Seq(18, 0, 0, 0, 0, 0, 0, 0), 
                      Seq(0, 0, 0, 0, 0, 0, 0, 0), 
                      Seq(0, 0, 0, 0, 0, 0, 0, 0), 
                      Seq(0, 0, 0, 0, 0, 0, 0, 0))

    val out1qt2 = Seq(Seq(-408, -36, -48, 47, 99, -99, 0, 0), 
                      Seq(0, -42, 52, 0, 0, 0, 0, 0), 
                      Seq(-48, 26, 56, 0, -99, 0, 0, 0), 
                      Seq(-47, 0, 0, 0, 0, 0, 0, 0), 
                      Seq(0, 0, 0, 0, 0, 0, 0, 0), 
                      Seq(0, 0, 0, 0, 0, 0, 0, 0), 
                      Seq(0, 0, 0, 0, 0, 0, 0, 0), 
                      Seq(0, 0, 0, 0, 0, 0, 0, 0))

    val out2qt1 = Seq(Seq(96, -33, -60, 32, 48, -40, 0, 0), 
                      Seq(0, -36, 56, 19, 26, 0, 0, 0), 
                      Seq(-42, 13, 80, -24, -40, 0, 0, 0), 
                      Seq(-56, 17, 44, -29, 0, 0, 0, 0),
                      Seq(18, 0, 0, 0, 0, 0, 0, 0), 
                      Seq(0, 0, 0, 0, 0, 0, 0, 0), 
                      Seq(0, 0, 0, 0, 0, 0, 0, 0), 
                      Seq(0, 0, 0, 0, 0, 0, 0, 0))

    val out2qt2 = Seq(Seq(102, -36, -48, 47, 99, -99, 0, 0), 
                      Seq(0, -42, 52, 0, 0, 0, 0, 0), 
                      Seq(-48, 26, 56, 0, -99, 0, 0, 0), 
                      Seq(-47, 0, 0, 0, 0, 0, 0, 0), 
                      Seq(0, 0, 0, 0, 0, 0, 0, 0), 
                      Seq(0, 0, 0, 0, 0, 0, 0, 0), 
                      Seq(0, 0, 0, 0, 0, 0, 0, 0), 
                      Seq(0, 0, 0, 0, 0, 0, 0, 0))

    val out3qt1 = Seq(Seq(128, -44, 80, 64, 96, -40, -51, 61), 
                      Seq(12, -36, 42, 19, 26, 0, 0, 55), 
                      Seq(-28, 39, 48, -24, 40, 57, 69, 0), 
                      Seq(0, -17, 22, 29, 51, 0, 0, 0), 
                      Seq(-18, 22, 37, 0, 0, 0, 0, 0), 
                      Seq(72, -35, -55, 0, 0, 0, 0, 0), 
                      Seq(49, 0, 78, 0, 0, 0, 0, 0), 
                      Seq(0, 0, 0, 0, 0, 0, 0, 0))

    val out3qt2 = Seq(Seq(119, -36, 72, 47, 99, -99, 0, 0), 
                      Seq(18, -21, 52, 0, 0, 0, 0, 0), 
                      Seq(-24, 26, 56, 0, 0, 0, 99, 0), 
                      Seq(0, 0, 0, 0, 0, 0, 0, 0), 
                      Seq(0, 0, 0, 0, 0, 0, 0, 0), 
                      Seq(99, 0, 0, 0, 0, 0, 0, 0), 
                      Seq(0, 0, 0, 0, 0, 0, 0, 0), 
                      Seq(0, 0, 0, 0, 0, 0, 0, 0))
}

object DCTData {
    // Baseline comparison 
    val in1 = Seq(
        Seq(62, 55, 55, 54, 49, 48, 47, 55),
        Seq(62, 57, 54, 52, 48, 47, 48, 53),
        Seq(61, 60, 52, 49, 48, 47, 49, 54),
        Seq(63, 61, 60, 60, 63, 65, 68, 65),
        Seq(67, 67, 70, 74, 79, 85, 91, 92),
        Seq(82, 95, 101, 106, 114, 115, 112, 117),
        Seq(96, 111, 115, 119, 128, 128, 130, 127),
        Seq(109, 121, 127, 133, 139, 141, 140, 133))
    
    val scaledOut1 = Seq(
        Seq(-362232500, -29286250, -2604000, -2500750, -1102500, -3547250, -1452500, -42000),
        Seq(-228012750, 44592500, 24272500, -137500, 9225000, 3705000, 4165000, -1390000),
        Seq(61932500, 8460000, -7540000, -2665000, 312500, -417500, 527500, -842500),
        Seq(11880750, -14430000, -3492500, -3485000, 2417500, -1090000, 2705000, -335000),
        Seq(-4777500, -3807500, 867500, 3492500, 405000, 5115000, 1122500, 472500),
        Seq(465500, 3030000, -1430000, 390000, -1072500, -1622500, -1152500, 862500),
        Seq(4284000, 2217500, -1700000, -1535000, 1082500, -2735000, 1105000, -1415000),
        Seq(-9962750, -1812500, 5812500, -397500, 237500, 442500, -1052500, 2500))


    val in2: Seq[Seq[Int]] = Seq(
        Seq(231, 32, 233, 161, 24, 71, 140, 245),
        Seq(247, 40, 248, 245, 124, 204, 36, 107),
        Seq(234, 202, 245, 167, 9, 217, 239, 173),
        Seq(193, 190, 100, 167, 43, 180, 8, 70),
        Seq(11, 24, 220, 177, 81, 243, 8, 112),
        Seq(97, 195, 203, 47, 125, 114, 165, 181),
        Seq(193, 70, 174, 167, 41, 30, 127, 245),
        Seq(87, 149, 57, 192, 65, 129, 178, 228))

    val scaledOut2 = Seq(
        Seq(94080000, 43554000, 74336500, -138047000, 2205000, 120790250, 193728500, -99030750),
        Seq(75596500, 113510000, -20830000, 42072500, 8960000, 98052500, 136407500, 9805000),
        Seq(42647500, -63535000, 112412500, -73517500, 125610000, 94120000, -42045000, 55930000),
        Seq(-68017250, -39585000, -23902500, -78095000, 25310000, -35997500, 66275000, 126597500),
        Seq(-105595000, -42512500, -55965000, 6905000, 28420000, -27747500, 4000000, -91425000),
        Seq(-7150500, 54177500, 171905000, -32795000, 33692500, 32157500, -59995000, 16305000),
        Seq(76982500, -64235000, 117345000, -13910000, -135885000, -30945000, -105977500, 38602500),
        Seq(21376250, -77292500, 525000, -74592500, -23317500, 81142500, 65160000, 7637500))


    // Inputs 3-6 are Random values 
    val in3 = Seq(
        Seq(101,  63,  79,   2,  14, 241,  22,  84),
        Seq( 96,  69, 110, 238,  90, 187, 227,  79),
        Seq(239,  36, 168, 129, 120, 189,  69, 150),
        Seq( 35,  85,  45,  71, 246,  49,  48, 227),
        Seq(202, 215,  57, 170, 166,  85, 130,  32),
        Seq(140, 159,  25, 225, 145, 182, 253, 137),
        Seq(219,  11,  89, 201, 155, 163, 119,  83),
        Seq(206, 127, 191, 122,  43,  47,  42, 146))

    val scaledOut3 = Seq(
        Seq( -36382500,    -138250,   -9149000,   96166000,   89547500,   67140500,    39945500,  -61817000),
        Seq( -70726250,  -86657500,  -23877500,   34002500,  -84062500,  -34592500,   83725000,  -25662500),
        Seq( -68908000,   33655000,   36352500,  -15715000,  -87177500,   82242500,  158672500,  -46200000),
        Seq(   4576250,  -39080000,  -21482500,  112347500,  -85392500,   24172500, -110432500,  -88315000),
        Seq(-135852500,   74795000,   65627500,  -58777500,   -5482500, -176145000,    6682500,   38540000),
        Seq( -82188750,  -78437500,   -1220000,   32560000,   76335000,  -96242500,  116047500,   42670000),
        Seq(  14885500,   18885000,   86402500,   -4570000,  -32782500,  -36700000,   71805000,  -21380000),
        Seq(   2971500,  149955000,   16002500,   80740000,     537500,   48787500,   91420000,    3357500))

    val in4 = Seq(
        Seq( 80,  64, 240,  45, 193, 121, 196, 231),
        Seq(223,  64, 205, 150, 107,  40, 217, 142),
        Seq( 76, 124, 252,  37, 228,  94, 244, 185),
        Seq( 17, 250, 191,  12,  75,  89, 105, 128),
        Seq(160,  36, 210, 122,  19, 109, 160,  50),
        Seq( 95, 201,   2,  43, 100,  73, 100, 154),
        Seq(144, 225,   3, 112,  60,  35,  53, 191),
        Seq(220, 189, 191, 101, 182,  38,  58, 195))

    val scaledOut4 = Seq(
        Seq( -16660000,   13665750,  139639500, -124850250,  -36995000,  -55118000,  -16390500,  148998500),
        Seq(  91983500, -130415000,  -72307500,  -37565000,  -92147500,  102502500,   66947500,  173932500),
        Seq(  95910500,   -5565000,   29280000,  -11260000,  147495000,  -24515000,   17727500,   44192500),
        Seq( -92431500,  -50357500,   20240000,   52647500,   18950000,   42267500,     -82500,  -89302500),
        Seq(  12005000,   27800000,  -50875000,  -40112500,  -65305000,     300000,  103670000,   41462500),
        Seq( -40283250,  -60675000,   22210000,  -37207500,  -53562500, -191942500,  -59955000,  -56860000),
        Seq(  46588500,  -88322500,  -32762500,   15850000,  -26190000, -108612500,   -3045000,   93387500),
        Seq(  36972250,  -35642500,  -39565000,    5910000,   11355000,  100802500,   57002500,   52455000))

    val in5 = Seq(
        Seq(107,  61, 118,  83, 222,   0,  48,  82),
        Seq(123,  15,  29,  91, 227,  40, 101, 146),
        Seq(145,  96,  49, 121,  51,  49, 193, 232),
        Seq( 30, 161,   5, 151, 217,  34,  50,  67),
        Seq(197, 101, 117,  69, 133, 115,  58,  49),
        Seq(  6,   0, 121, 121,  91,  91, 111, 115),
        Seq( 25,  97, 212, 204, 219, 166, 154, 234),
        Seq(190,  50,  53, 221, 193, 137, 181, 114))

    val scaledOut5 = Seq(
        Seq(-135117500, -85576750, -79495500, -5143250, 179217500, -6786500, -59409000, 83672750),
        Seq(-127149750, 53997500, 72542500, 12442500, 65385000, -66115000, -51680000, 70985000),
        Seq(81917500, -47720000, -40212500, 2980000, 66552500, 39012500, 17755000, 61107500),
        Seq(-78877750, 39285000, -79545000, -36597500, -57317500, -38737500, 100947500, 74015000),
        Seq(-31972500, 154685000, -49357500, 108752500, 26047500, -33792500, -43557500, 7432500),
        Seq(43505000, -32897500, -67792500, -120192500, -35277500, -77650000, -44350000, -19995000),
        Seq(-58702000, -12062500, 82105000, -23767500, -26277500, 93855000, -1615000, -30442500),
        Seq(99916250, 32590000, 102140000, -9732500, -40217500, 32762500, 70015000, 5185000))

    val in6 = Seq(
        Seq(255, 237, 164,   6, 139,  51, 140, 234),
        Seq(167, 234, 134,  52,  65, 175, 134, 109),
        Seq(  9, 239,   5, 179,  62, 152, 122, 232),
        Seq(223, 101, 203,  19, 219,  39, 207, 202),
        Seq(113,  63, 228,  88, 145, 237,  93, 171),
        Seq( 79,  35, 158, 137,  39,  55, 213, 249),
        Seq(114,  38, 142, 117,  67, 232,  84,  71),
        Seq(102,  34,  50, 198, 116, 176, 139,  79))

    val scaledOut6 = Seq(
        Seq(  21927500,  -75848500,  116669000,  -25326000,  -31482500,  -10766000,  64774500,    9868250),
        Seq(  92576750,  106275000,  183510000,  -26322500,  -32730000, -164385000, -95120000,   39092500),
        Seq( -39186000,   77320000,   10642500,   74830000,   -7252500,   13087500, -39082500,  -78615000),
        Seq(  28185500,   20525000,   76280000,  -61895000,  -10257500,   21967500, 100105000,  110692500),
        Seq(  69947500,   44420000,   10112500,   47725000,   67717500,  -18112500,  38497500,  165640000),
        Seq(   8855000,   18960000,   58807500,   -9120000,   50715000,  -12450000, -17985000,   87290000),
        Seq( -10531500, -101700000,   46057500, -122192500,   76632500,    6062500, -124245000,  -68465000),
        Seq( -16471000,  -16535000,  -92315000,   26347500,  -35335000,  -72567500,  57935000, -155240000))

    // Sparse Zeros and Random values
    val in7 = Seq(
        Seq(194, 176, 144, 165, 193, 137,   6, 0),
        Seq(167,   9, 131, 200,   8,  78, 240,  93),
        Seq(144, 246, 171, 232,  68, 142,  74,  16),
        Seq(0, 246, 181, 248,  26,  40, 163, 249),
        Seq(213, 230, 241, 0, 171,  65, 212, 235),
        Seq( 41, 249,  95, 139,  98,  69, 197, 231),
        Seq(174, 225,  12, 129, 111,  67, 117, 0),
        Seq(0, 161,  64, 252,  59, 178, 240, 0))

    val scaledOut7 = Seq(
        Seq(33075000, 107546250, 9842000, -99748250, -91875000, 60639250, -179879000, -94592750),
        Seq(14061250, 97392500, -35082500, -11107500, 68782500, 47472500, 141412500, 30817500),
        Seq(-118702500, 52672500, -169135000, 204772500, 17560000, 111880000, -5552500, -65247500),
        Seq(30891000, 10720000, 107115000, 46465000, 70767500, -140467500, 25110000, 138607500),
        Seq(63210000, -23332500, -30940000, -48772500, -25775000, -65092500, 26780000, 39672500),
        Seq(-41683250, 126020000, -66762500, -1380000, 38965000, -115362500, -44450000, -99712500),
        Seq(35969500, 25195000, -93415000, -17255000, -45485000, -89232500, -7332500, -80980000),
        Seq(26194000, 178312500, 7340000, 165337500, -2210000, -73952500, 57877500, 94207500))

    // Null Matrix
    val in8: Seq[Seq[Int]] = Seq.fill(8)(Seq.fill(8)(0))

    val scaledOut8 = Seq(
        Seq(-1003520000, 0, 0, 0, 0, 0, 0, 0),
        Seq(0, 0, 0, 0, 0, 0, 0, 0),
        Seq(0, 0, 0, 0, 0, 0, 0, 0),
        Seq(0, 0, 0, 0, 0, 0, 0, 0),
        Seq(0, 0, 0, 0, 320000, 0, 0, 0),
        Seq(0, 0, 0, 0, 0, 0, 0, 0),
        Seq(0, 0, 0, 0, 0, 0, 0, 0),
        Seq(0, 0, 0, 0, 0, 0, 0, 0))

}

class ZigZagDecodeModelTester extends AnyFlatSpec with ChiselScalatestTester {
    it should "Produce out 2x2" in {
        val jpegEncoder = new jpegEncode(false, List.empty, 0)
        assert(jpegEncoder.zigzagDecode(ZigZagParseData.out2x2) == ZigZagParseData.in2x2)
    }

    it should "Produce out 3x3" in {
        val jpegEncoder = new jpegEncode(false, List.empty, 0)
        assert(jpegEncoder.zigzagDecode(ZigZagParseData.out3x3) == ZigZagParseData.in3x3)
    }

    it should "Produce out 4x4" in {
        val jpegEncoder = new jpegEncode(false, List.empty, 0)
        assert(jpegEncoder.zigzagDecode(ZigZagParseData.out4x4) == ZigZagParseData.in4x4)
    }

    it should "Produce out 8x8" in {
        val jpegEncoder = new jpegEncode(false, List.empty, 0)
        assert(jpegEncoder.zigzagDecode(ZigZagParseData.out8x8) == ZigZagParseData.in8x8)
    }
}

class ZigZagParseModelTester extends AnyFlatSpec with ChiselScalatestTester {
    it should "Zig Zag 2x2" in {
        val jpegEncoder = new jpegEncode(false, List.empty, 0)
        assert(jpegEncoder.zigzagParse(ZigZagParseData.in2x2) == ZigZagParseData.out2x2)
    }

    it should "Zig Zag 3x3" in {
        val jpegEncoder = new jpegEncode(false, List.empty, 0)
        assert(jpegEncoder.zigzagParse(ZigZagParseData.in3x3) == ZigZagParseData.out3x3)
    }

    it should "Zig Zag 4x4" in {
        val jpegEncoder = new jpegEncode(false, List.empty, 0)
        assert(jpegEncoder.zigzagParse(ZigZagParseData.in4x4) == ZigZagParseData.out4x4)
    }

    it should "Zig Zag 8x8" in {
        val jpegEncoder = new jpegEncode(false, List.empty, 0)
        assert(jpegEncoder.zigzagParse(ZigZagParseData.in8x8) == ZigZagParseData.out8x8)
    }
}


class RLEModelTester extends AnyFlatSpec with ChiselScalatestTester {
    it should "RLE test 1" in {
        val jpegEncoder = new jpegEncode(false, List.empty, 0)
        assert(jpegEncoder.RLE(RLEData.in1) == RLEData.out1)
    }

    it should "RLE test 2" in {
        val jpegEncoder = new jpegEncode(false, List.empty, 0)
        assert(jpegEncoder.RLE(RLEData.in2) == RLEData.out2)
    }

    it should "RLE test 3" in {
        val jpegEncoder = new jpegEncode(false, List.empty, 0)
        assert(jpegEncoder.RLE(RLEData.in3) == RLEData.out3)
    }

    it should "RLE test no dupes" in {
        val jpegEncoder = new jpegEncode(false, List.empty, 0)
        assert(jpegEncoder.RLE(RLEData.in4) == RLEData.out4)
    }

}

class deltaModelTester extends AnyFlatSpec with ChiselScalatestTester {
    it should "delta test 1" in {
        val jpegEncoder = new jpegEncode(false, List.empty, 0)
        assert(jpegEncoder.delta(deltaData.in1) == deltaData.out1)
    }

    it should "delta test 2" in {
        val jpegEncoder = new jpegEncode(false, List.empty, 0)
        assert(jpegEncoder.delta(deltaData.in2) == deltaData.out2)
    }

    it should "delta test 3" in {
        val jpegEncoder = new jpegEncode(false, List.empty, 0)
        assert(jpegEncoder.delta(deltaData.in3) == deltaData.out3)
    }

    it should "delta test empty" in {
        val jpegEncoder = new jpegEncode(false, List.empty, 0)
        assert(jpegEncoder.delta(deltaData.in4) == deltaData.out4)
    }

    it should "delta test single elem" in {
        val jpegEncoder = new jpegEncode(false, List.empty, 0)
        assert(jpegEncoder.delta(deltaData.in5) == deltaData.out5)
    }

}
class deltaDecodeModelTester extends AnyFlatSpec with ChiselScalatestTester {
    it should "delta decode test 1" in {
        val jpegEncoder = new jpegEncode(false, List.empty, 0)
        assert(jpegEncoder.decodeDelta(deltaData.out1) == deltaData.in1)
    }

    it should "delta decode test 2" in {
        val jpegEncoder = new jpegEncode(false, List.empty, 0)
        assert(jpegEncoder.decodeDelta(deltaData.out2) == deltaData.in2)
    }

    it should "delta decode test 3" in {
        val jpegEncoder = new jpegEncode(false, List.empty, 0)
        assert(jpegEncoder.decodeDelta(deltaData.out3) == deltaData.in3)
    }

    it should "delta decode test empty" in {
        val jpegEncoder = new jpegEncode(false, List.empty, 0)
        assert(jpegEncoder.decodeDelta(deltaData.out4) == deltaData.in4)
    }

    it should "delta decode test single elem" in {
        val jpegEncoder = new jpegEncode(false, List.empty, 0)
        assert(jpegEncoder.decodeDelta(deltaData.out5) == deltaData.in5)
    }

}

class quantizationModelTester extends AnyFlatSpec with ChiselScalatestTester {
    it should "in1 / quant table 1" in {
        val jpegEncoder = new jpegEncode(false, List.empty, 0)
        assert(jpegEncoder.quantization(QuantizationData.in1, QuantizationTables.qt1) == QuantizationData.out1qt1)
    }
  
    it should "in1 / quant table 2" in {
        val jpegEncoder = new jpegEncode(false, List.empty, 0)
        assert(jpegEncoder.quantization(QuantizationData.in1, QuantizationTables.qt2) == QuantizationData.out1qt2)
    }

    it should "in2 / quant table 1" in {
        val jpegEncoder = new jpegEncode(false, List.empty, 0)
        assert(jpegEncoder.quantization(QuantizationData.in2, QuantizationTables.qt1) == QuantizationData.out2qt1)
    }

    it should "in2 / quant table 2" in {
        val jpegEncoder = new jpegEncode(false, List.empty, 0)
        assert(jpegEncoder.quantization(QuantizationData.in2, QuantizationTables.qt2) == QuantizationData.out2qt2)
    }

    it should "in3 / quant table 1" in {
        val jpegEncoder = new jpegEncode(false, List.empty, 0)
        assert(jpegEncoder.quantization(QuantizationData.in3, QuantizationTables.qt1) == QuantizationData.out3qt1)
    }

    it should "in3 / quant table 2" in {
        val jpegEncoder = new jpegEncode(false, List.empty, 0)
        assert(jpegEncoder.quantization(QuantizationData.in3, QuantizationTables.qt2) == QuantizationData.out3qt2)
    }
}

class quantizationDecodeModelTester extends AnyFlatSpec with ChiselScalatestTester {
    it should "in1 / qt1 * qt1" in {
        val jpegEncoder = new jpegEncode(false, List.empty, 0)
        val data = jpegEncoder.quantization(QuantizationData.in1, QuantizationTables.qt1)
        assert(jpegEncoder.inverseQuantization(data, QuantizationTables.qt1) == QuantizationDecodeData.out1qt1)
    }
  
    it should "in1 / qt2 * qt2" in {
        val jpegEncoder = new jpegEncode(false, List.empty, 0)
        val data = jpegEncoder.quantization(QuantizationData.in1, QuantizationTables.qt2)
        assert(jpegEncoder.inverseQuantization(data, QuantizationTables.qt2) == QuantizationDecodeData.out1qt2)
    }

    it should "in2 / qt1 * qt1" in {
        val jpegEncoder = new jpegEncode(false, List.empty, 0)
        val data = jpegEncoder.quantization(QuantizationData.in2, QuantizationTables.qt1)
        assert(jpegEncoder.inverseQuantization(data, QuantizationTables.qt1) == QuantizationDecodeData.out2qt1)
    }

    it should "in2 / qt2 * qt2" in {
        val jpegEncoder = new jpegEncode(false, List.empty, 0)
        val data = jpegEncoder.quantization(QuantizationData.in2, QuantizationTables.qt2)
        assert(jpegEncoder.inverseQuantization(data, QuantizationTables.qt2) == QuantizationDecodeData.out2qt2)
    }

    it should "in3 / qt1 * qt1" in {
        val jpegEncoder = new jpegEncode(false, List.empty, 0)
        val data = jpegEncoder.quantization(QuantizationData.in3, QuantizationTables.qt1)
        assert(jpegEncoder.inverseQuantization(data, QuantizationTables.qt1) == QuantizationDecodeData.out3qt1)
    }

    it should "in3 / qt2 * qt2" in {
        val jpegEncoder = new jpegEncode(false, List.empty, 0)
        val data = jpegEncoder.quantization(QuantizationData.in3, QuantizationTables.qt2)
        assert(jpegEncoder.inverseQuantization(data, QuantizationTables.qt2) == QuantizationDecodeData.out3qt2)
    }
}

class dctModelTester extends AnyFlatSpec with ChiselScalatestTester {

    it should "dct test 1" in {
        val jpegEncode = new jpegEncode(false, List.empty, 0)
        val dctResult = jpegEncode.DCT(DCTData.in1)
        val rounded = jpegEncode.roundToInt(dctResult)

        assert(rounded == DCTData.scaledOut1)
    }


    it should "dct test 2" in {
        val jpegEncode = new jpegEncode(false, List.empty, 0)
        val dctResult = jpegEncode.DCT(DCTData.in2)
        val rounded = jpegEncode.roundToInt(dctResult)
        
        assert(rounded == DCTData.scaledOut2)
    }

    it should "dct test 3" in {
        val jpegEncode = new jpegEncode(false, List.empty, 0)
        val dctResult = jpegEncode.DCT(DCTData.in3)
        val rounded = jpegEncode.roundToInt(dctResult)
        
        assert(rounded == DCTData.scaledOut3)
    }

    it should "dct test 4" in {
        val jpegEncode = new jpegEncode(false, List.empty, 0)
        val dctResult = jpegEncode.DCT(DCTData.in4)
        val rounded = jpegEncode.roundToInt(dctResult)
        
        assert(rounded == DCTData.scaledOut4)
    }

    it should "dct test 5" in {
        val jpegEncode = new jpegEncode(false, List.empty, 0)
        val dctResult = jpegEncode.DCT(DCTData.in5)
        val rounded = jpegEncode.roundToInt(dctResult)
        
        assert(rounded == DCTData.scaledOut5)
    }

    it should "dct test 6" in {
        val jpegEncode = new jpegEncode(false, List.empty, 0)
        val dctResult = jpegEncode.DCT(DCTData.in6)
        val rounded = jpegEncode.roundToInt(dctResult)
    
        assert(rounded == DCTData.scaledOut6)
    }

    it should "dct test 7" in {
        val jpegEncode = new jpegEncode(false, List.empty, 0)
        val dctResult = jpegEncode.DCT(DCTData.in7)
        val rounded = jpegEncode.roundToInt(dctResult)
        
        jpegEncode.printMatrix(rounded)
        assert(rounded == DCTData.scaledOut7)
    }

    it should "dct test 8" in {
        val jpegEncode = new jpegEncode(false, List.empty, 0)
        val dctResult = jpegEncode.DCT(DCTData.in8)
        val rounded = jpegEncode.roundToInt(dctResult)

        assert(rounded == DCTData.scaledOut8)
    }
}

