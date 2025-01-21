import numpy as np
import subprocess
import time
from test import *
class JPEGDCT:
    def __init__(self, scaling_factor=100):
        self.scaling_factor = scaling_factor
        
    def process_block(self, block):
        N = 8
        shifted_block = np.array(block, dtype=np.int32) - 128
        dct_block = np.zeros((N, N), dtype=np.int64)

        for u in range(N):
            for v in range(N):
                sum_val = 0
                for i in range(N):
                    for j in range(N):
                        pixel_value = shifted_block[i][j]
                        cos_i = int(np.cos((2 * i + 1) * u * np.pi / 16) * 100)
                        cos_j = int(np.cos((2 * j + 1) * v * np.pi / 16) * 100)
                        cos_val = (pixel_value * cos_i) // 100
                        sum_val += cos_val * cos_j

                alpha_u = int((1.0 / np.sqrt(2)) * 100) if u == 0 else 100
                alpha_v = int((1.0 / np.sqrt(2)) * 100) if v == 0 else 100
                intermediate = (sum_val * alpha_u) // 100
                final = (intermediate * alpha_v) // 100
                dct_block[u][v] = final // 4

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
    
def compare_results(hw_file, sw_result, stage_name, threshold=1000):
    try:
        hw_values = np.loadtxt(hw_file, dtype=np.int64)
        
        if isinstance(sw_result, list):
            sw_result = np.array(sw_result, dtype=np.int64)
        else:
            sw_result = sw_result.astype(np.int64)
            
        if len(hw_values.shape) == 2:
            hw_values = hw_values.flatten()
        if len(sw_result.shape) == 2:
            sw_result = sw_result.flatten()
            
        # For DCT stage
        if 'DCT' in stage_name:
            hw_values = hw_values // 10000
            
        # For both Quantization and ZigZag stages
        if 'Quantization' in stage_name or 'Zigzag' in stage_name:
            sw_result = sw_result // 10000
            
        diff = np.abs(hw_values - sw_result)
        max_diff = np.max(diff)
        
        if max_diff < threshold:
            print(f"Test {stage_name}: PASS")
            return True
        else:
            print(f"Test {stage_name}: FAIL (max diff: {max_diff:.2f})")
            print(f"Hardware range: [{np.min(hw_values)}, {np.max(hw_values)}]")
            print(f"Software range: [{np.min(sw_result)}, {np.max(sw_result)}]")
            return False
            
    except Exception as e:
        print(f"Test {stage_name}: ERROR - {str(e)}")
        return False
def test_full_pipeline():
    try:
        print("\nRunning hardware test...")
        hw_start_time = time.time()
        result = subprocess.run(["python3", "test.py"], capture_output=True, text=True)
        hw_time = time.time() - hw_start_time
        print(f"Take {hw_time:.2f}s")
        if result.returncode != 0:
            print("\nHardware test Failed")
            print(result.stderr)
            return
        else:
            print("\nHardware test completed")
        print("\n=== Hardware Test Output ===")
        if result.stdout:
            print(result.stdout)
        if result.stderr:
            print("Errors:", result.stderr)
        print("===========================\n")
        components = [
            ("Y", 1),
            ("Cb", 2),
            ("Cr", 2)
        ]
        for comp, qt_choice in components:
            print(f"\nTesting {comp} component:")
            test = JPEGTest()
            input_block = test.read_block(f"hw_output/{comp.lower()}_block_0.txt")
            
            # DCT
            dct = JPEGDCT()
            sw_dct = dct.process_block(input_block)
            
            # Quantization
            quant = JPEGQuantization(qt_choice=qt_choice)
            sw_quant = quant.quantize(sw_dct)
            
            # Zigzag
            zigzag = JPEGZigzag()
            sw_zigzag = zigzag.scan(sw_quant)
            
            compare_results(f"hw_output/chisel_dct_{comp}.txt", sw_dct, f"{comp}_DCT")
            compare_results(f"hw_output/chisel_quant_{comp}.txt", sw_quant, f"{comp}_Quantization")
            compare_results(f"hw_output/chisel_zigzag_{comp}.txt", sw_zigzag, f"{comp}_Zigzag")
            
    except Exception as e:
        print(f"Error during testing: {e}")
        raise
def main():
    test_full_pipeline()
    
def generate_comparison_report(input_name):
    print(f"\n=== Comparison Report for {input_name} ===")
    print("\nSoftware Output:")
    
    print("\nHardware Output:")
    
    print("\nDifference Analysis:")
if __name__ == "__main__":
    main()