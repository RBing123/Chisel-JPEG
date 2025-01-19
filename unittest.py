import numpy as np

class JPEGDCT:
    def __init__(self, scaling_factor=100):
        self.scaling_factor = scaling_factor
        
    def process_block(self, block):
        """
        Perform 2D DCT on an 8x8 block with fixed-point arithmetic
        """
        N = 8
        dct_block = np.zeros((N, N), dtype=np.int32)
        
        for u in range(N):
            for v in range(N):
                sum_val = 0
                for i in range(N):
                    for j in range(N):
                        pixel_value = block[i][j]
                        cos_val = int(np.cos((2 * i + 1) * u * np.pi / 16) * 
                                    np.cos((2 * j + 1) * v * np.pi / 16) * 
                                    self.scaling_factor)
                        sum_val += pixel_value * cos_val
                
                alpha_u = int((1.0 / np.sqrt(2)) * self.scaling_factor) if u == 0 else self.scaling_factor
                alpha_v = int((1.0 / np.sqrt(2)) * self.scaling_factor) if v == 0 else self.scaling_factor
                
                dct_block[u][v] = (alpha_u * alpha_v * sum_val) // (4 * self.scaling_factor)
        
        return dct_block
class JPEGQuantization:
    def __init__(self, qt_choice=1):
        self.qt_choice = qt_choice
        # Define quantization tables based on qt_choice
        if qt_choice == 1:  # Luminance (Y)
            self.quant_table = np.array([
                [16, 11, 10, 16,  24,  40,  51,  61],
                [12, 12, 14, 19,  26,  58,  60,  55],
                [14, 13, 16, 24,  40,  57,  69,  56],
                [14, 17, 22, 29,  51,  87,  80,  62],
                [18, 22, 37, 56,  68, 109, 103,  77],
                [24, 35, 55, 64,  81, 104, 113,  92],
                [49, 64, 78, 87, 103, 121, 120, 101],
                [72, 92, 95, 98, 112, 100, 103,  99]
            ])
        else:  # Chrominance (Cb, Cr)
            self.quant_table = np.array([
                [17, 18, 24, 47, 99, 99, 99, 99],
                [18, 21, 26, 66, 99, 99, 99, 99],
                [24, 26, 56, 99, 99, 99, 99, 99],
                [47, 66, 99, 99, 99, 99, 99, 99],
                [99, 99, 99, 99, 99, 99, 99, 99],
                [99, 99, 99, 99, 99, 99, 99, 99],
                [99, 99, 99, 99, 99, 99, 99, 99],
                [99, 99, 99, 99, 99, 99, 99, 99]
            ])

    def quantize(self, dct_block):
        """Quantize the DCT coefficients"""
        return np.round(dct_block / self.quant_table).astype(np.int32)
    
class JPEGZigzag:
    def __init__(self):
        # Define zigzag scanning order
        self.zigzag_order = [
            (0,0), (0,1), (1,0), (2,0), (1,1), (0,2), (0,3), (1,2),
            (2,1), (3,0), (4,0), (3,1), (2,2), (1,3), (0,4), (0,5),
            (1,4), (2,3), (3,2), (4,1), (5,0), (6,0), (5,1), (4,2),
            (3,3), (2,4), (1,5), (0,6), (0,7), (1,6), (2,5), (3,4),
            (4,3), (5,2), (6,1), (7,0), (7,1), (6,2), (5,3), (4,4),
            (3,5), (2,6), (1,7), (2,7), (3,6), (4,5), (5,4), (6,3),
            (7,2), (7,3), (6,4), (5,5), (4,6), (3,7), (4,7), (5,6),
            (6,5), (7,4), (7,5), (6,6), (5,7), (6,7), (7,6), (7,7)
        ]

    def scan(self, block):
        """Convert block to 1D array in zigzag order"""
        return [block[i][j] for i, j in self.zigzag_order]
    
class JPEGTest:
    @staticmethod
    def read_block(filename):
        """Read an 8x8 block from file"""
        with open(filename, 'r') as f:
            values = [int(line.strip()) for line in f]
            return np.array(values).reshape(8, 8)
    
    def test_dct(self, input_file):
        """Test DCT processing"""
        print(f"Testing DCT for {input_file}")
        block = self.read_block(input_file)
        dct = JPEGDCT()
        result = dct.process_block(block)
        
        print("\n=== DCT Output ===")
        for row in result:
            print(" ".join(f"{val:5d}" for val in row))
        
        return result
    def test_quantization(self, dct_result, qt_choice):
        """Test quantization processing"""
        print("\n=== Quantization Output ===")
        quant = JPEGQuantization(qt_choice)
        quant_result = quant.quantize(dct_result)
        
        for row in quant_result:
            print(" ".join(f"{val:5d}" for val in row))
        
        return quant_result
    def test_zigzag(self, quant_result):
        """Test zigzag scanning"""
        print("\n=== Zigzag Output ===")
        zigzag = JPEGZigzag()
        zigzag_result = zigzag.scan(quant_result)
        
        for i, value in enumerate(zigzag_result):
            print(f"Index {i}: {value}")
        
        return zigzag_result

def main():
    test = JPEGTest()
    
    # Test Y component
    print("Processing Y component")
    test.test_dct("hw_output/y_block_0.txt")
    
    # Test Cb component
    print("\nProcessing Cb component")
    test.test_dct("hw_output/cb_block_0.txt")
    
    # Test Cr component
    print("\nProcessing Cr component")
    test.test_dct("hw_output/cr_block_0.txt")
    print("Processing Y component")
    dct_result = test.test_dct("hw_output/y_block_0.txt")
    test.test_quantization(dct_result, qt_choice=1)  # Use luminance table
    
    # Test Cb component
    print("\nProcessing Cb component")
    dct_result = test.test_dct("hw_output/cb_block_0.txt")
    test.test_quantization(dct_result, qt_choice=2)  # Use chrominance table
    
    # Test Cr component
    print("\nProcessing Cr component")
    dct_result = test.test_dct("hw_output/cr_block_0.txt")
    test.test_quantization(dct_result, qt_choice=2)  # Use chrominance table

if __name__ == "__main__":
    main()