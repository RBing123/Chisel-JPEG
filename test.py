import os
import subprocess
import numpy as np
import heapq
from PIL import Image
class HuffmanNode:
    def __init__(self, char, freq):
        self.char = char
        self.freq = freq
        self.left = None
        self.right = None
    
    def __lt__(self, other):
        return self.freq < other.freq
    
def convert_jpg2bmp(file_path, bmp_path):
    """
    Convert jpg to bmp
    """
    img = Image.open(file_path)
    if img.mode != 'RGB':
        img = img.convert('RGB')
    img.save(bmp_path, format="BMP")
    print(f"Converted {file_path} to {bmp_path}")
    
def read_bmp(filepath):
    """
    Read BMP and convert it to YCbCr
    """
    img = Image.open(filepath)
    if img.mode != 'RGB':
        raise ValueError("Image must be in RGB format!")
    
    ycbcr = img.convert('YCbCr')
    y, cb, cr = ycbcr.split()
    
    width, height = img.size
    if width % 8 != 0 or height % 8 != 0:
        raise ValueError(f"Image dimensions must be multiples of 8! Current size: {width}x{height}")
    
    return np.array(y), np.array(cb), np.array(cr)

def extract_blocks(channel_data):
    h, w = channel_data.shape
    blocks = []
    for i in range(0, h, 8):
        for j in range(0, w, 8):
            block = channel_data[i:i+8, j:j+8]
            block = block.astype(np.int16) - 128
            blocks.append(block)
    return blocks

def save_blocks_for_chisel(blocks, component_name, output_dir="hw_output"):
    os.makedirs(output_dir, exist_ok=True)
    for idx, block in enumerate(blocks):
        filename = f"{output_dir}/{component_name}_block_{idx}.txt"
        with open(filename, 'w') as f:
            for row in block:
                for val in row:
                    f.write(f"{val}\n")

def run_chisel_test(sbt_project_path, blocks_y, blocks_cb, blocks_cr):
    save_blocks_for_chisel(blocks_y, "y")
    save_blocks_for_chisel(blocks_cb, "cb")
    save_blocks_for_chisel(blocks_cr, "cr")
    
    print("Running Chisel tests...")
    process = subprocess.run(
        ["sbt", "testOnly jpeg.JPEGEncodeChiselTests"],
        cwd=sbt_project_path,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True
    )
    
    if process.returncode != 0:
        print("Chisel test failed!")
        print(process.stderr)
        return None
    
    return True

def read_encoded_output(encoding_type="rle"):
    """
    Read Chisel encoded output (RLE/DPCM)
    """
    output_dir = "hw_output"
    y_output = []
    cb_output = []
    cr_output = []
    
    for component in ['y', 'cb', 'cr']:
        output_file = f"{output_dir}/encoded_{encoding_type}_{component}.txt"
        if os.path.exists(output_file):
            with open(output_file, 'r') as f:
                values = [int(line.strip()) for line in f if line.strip()]
                if component == 'y':
                    y_output = values
                elif component == 'cb':
                    cb_output = values
                else:
                    cr_output = values
    
    return y_output, cb_output, cr_output
def read_encoded_blocks(output_dir="hw_output", encoding_type="RLE"):
    """
    Read encoded blocks from RLE or Delta output files
    
    Args:
        output_dir: Directory containing encoded files
        encoding_type: "RLE" or "Delta"
        
    Returns:
        Dictionary containing Y, Cb, Cr channel data for all blocks
    """
    encoded_data = {'Y': [], 'Cb': [], 'Cr': []}
    encoding_dir = f"{output_dir}/{encoding_type}"
    
    # Read all files in the directory
    for filename in os.listdir(encoding_dir):
        if not filename.endswith('.txt'):
            continue
            
        # Parse filename to get component and block number
        parts = filename.split('_')
        component = parts[0]  # Y, Cb, or Cr
        
        with open(os.path.join(encoding_dir, filename), 'r') as f:
            if encoding_type == "RLE":
                # Read pairs of values for RLE
                values = []
                for line in f:
                    run_length, value = map(int, line.strip().split())
                    values.extend([run_length, value])
            else:  # Delta
                # Read single DC difference value
                value = int(f.readline().strip())
                values = [value]
                
        encoded_data[component].append(values)
    
    return encoded_data
def generate_huffman_codes(frequencies):
    """
    Generate Huffman codes for given frequencies
    
    Args:
        frequencies: Dictionary of value:frequency pairs
        
    Returns:
        Dictionary of value:huffman_code pairs
    """
    if not frequencies:
        return {}
        
    # Create heap from frequencies
    heap = []
    for char, freq in frequencies.items():
        heapq.heappush(heap, HuffmanNode(char, freq))
    
    # Build Huffman tree
    while len(heap) > 1:
        left = heapq.heappop(heap)
        right = heapq.heappop(heap)
        
        internal = HuffmanNode(None, left.freq + right.freq)
        internal.left = left
        internal.right = right
        
        heapq.heappush(heap, internal)
    
    # Generate codes by traversing tree
    codes = {}
    def generate_codes_recursive(node, code=""):
        if node is None:
            return
            
        if node.char is not None:
            codes[node.char] = code
            return
            
        generate_codes_recursive(node.left, code + "0")
        generate_codes_recursive(node.right, code + "1")
    
    if heap:
        generate_codes_recursive(heap[0])
    
    return codes
def perform_huffman_coding(encoded_data):
    """
    Perform Huffman coding on RLE and Delta encoded data
    
    Args:
        encoded_data: Dictionary containing encoded data for each component
    """
    for encoding_type in ['RLE', 'Delta']:
        data = read_encoded_blocks(encoding_type=encoding_type)
        
        for component in ['Y', 'Cb', 'Cr']:
            # Collect all values for frequency calculation
            all_values = []
            for block in data[component]:
                all_values.extend(block)
                
            # Create Huffman tree
            frequencies = {}
            for value in all_values:
                frequencies[value] = frequencies.get(value, 0) + 1
                
            # Generate Huffman codes
            huffman_codes = generate_huffman_codes(frequencies)
            
            # Save Huffman codes and encoded data
            save_huffman_output(
                component, 
                encoding_type, 
                huffman_codes, 
                data[component],
                output_dir="hw_output/huffman"
            )
            
def save_huffman_output(component, encoding_type, huffman_codes, data, output_dir):
    """Save Huffman coding results"""
    os.makedirs(output_dir, exist_ok=True)
    
    # Save Huffman codes
    codes_file = f"{output_dir}/{component}_{encoding_type.lower()}_codes.txt"
    with open(codes_file, 'w') as f:
        for value, code in huffman_codes.items():
            f.write(f"{value} {code}\n")
            
    # Save encoded data
    data_file = f"{output_dir}/{component}_{encoding_type.lower()}_data.txt"
    with open(data_file, 'w') as f:
        for block in data:
            encoded = ' '.join(huffman_codes[val] for val in block)
            f.write(f"{encoded}\n")
def create_bitstream(rle_data, delta_data, output_dir="hw_output/bitstream"):
    """
    Create bitstream from RLE and Delta encoded data
    """
    os.makedirs(output_dir, exist_ok=True)
    
    for component in ['Y', 'Cb', 'Cr']:
        # Create bitstream for each component
        bitstream = bytearray()
        
        # Add component type marker
        component_marker = ord(component[0])
        bitstream.append(component_marker)
        
        # Add Delta (DC) data
        if component in delta_data:
            # Get DC value from first block
            dc_value = delta_data[component][0][0]  # [block][value]
            bitstream.extend(int(dc_value).to_bytes(2, byteorder='big', signed=True))
        
        # Add RLE (AC) data
        if component in rle_data:
            for block in rle_data[component]:
                for run_length, value in zip(block[::2], block[1::2]):
                    bitstream.append(int(run_length))
                    bitstream.extend(int(value).to_bytes(2, byteorder='big', signed=True))
        
        # Write to file
        output_file = f"{output_dir}/{component.lower()}_encoded.bin"
        with open(output_file, 'wb') as f:
            f.write(bitstream)
        
        print(f"Created bitstream for {component}: {output_file}")

def encode_huffman_table(huffman_codes, is_dc=True):
    """
    Encode Huffman table in JPEG format
    """
    table_data = bytearray()
    # Add DHT marker
    table_data.extend(bytes([0xFF, 0xC4]))
    
    # Add table type (0 for DC, 1 for AC)
    table_type = 0 if is_dc else 1
    table_data.append(table_type)
    
    # Add codes...
    # (具體實現依照 JPEG 規範)
    
    return table_data
def analyze_huffman_table_statistics():
    """Analyze Huffman table statistics for each component and encoding type"""
    huffman_dir = "hw_output/huffman"
    
    for component in ['Y', 'Cb', 'Cr']:
        for encoding in ['rle', 'delta']:
            codes_file = f"{huffman_dir}/{component}_{encoding}_codes.txt"
            if os.path.exists(codes_file):
                code_lengths = []
                with open(codes_file, 'r') as f:
                    for line in f:
                        # Skip empty lines and handle malformed lines
                        line = line.strip()
                        if not line:
                            continue
                        try:
                            parts = line.split()
                            if len(parts) >= 2:
                                value, code = parts[0], parts[1]
                                code_lengths.append(len(code))
                        except Exception as e:
                            print(f"Warning: Skipping malformed line in {codes_file}: {line}")
                            continue
                
                if code_lengths:
                    print(f"\n{component} {encoding.upper()} Statistics:")
                    print(f"Total symbols: {len(code_lengths)}")
                    print(f"Min code length: {min(code_lengths)}")
                    print(f"Max code length: {max(code_lengths)}")
                    print(f"Average code length: {sum(code_lengths)/len(code_lengths):.2f}")
def calculate_compression_ratio(original_size, compressed_size):
    """Calculate compression ratio"""
    ratio = original_size / compressed_size
    print(f"\nCompression Results:")
    print(f"Original size: {original_size} bytes")
    print(f"Compressed size: {compressed_size} bytes")
    print(f"Compression ratio: {ratio:.2f}:1")
if __name__ == "__main__":
    jpg_path = "8.jpg"
    bmp_path = "output.bmp"
    sbt_project_path = "."  # 假設在項目根目錄運行
    # 1. Convert jpg to bmp
    print("Converting JPG to BMP...")
    convert_jpg2bmp(jpg_path, bmp_path)
    # 2. Read .bmp
    print("Reading and processing BMP file...")
    y, cb, cr = read_bmp(bmp_path)
    y_blocks = extract_blocks(y)
    cb_blocks = extract_blocks(cb)
    cr_blocks = extract_blocks(cr)
    
    # 3. Run Chisel Test
    print("Running Chisel implementation...")
    success = run_chisel_test(sbt_project_path, y_blocks, cb_blocks, cr_blocks)
    
    if success:
        print("Reading encoded data...")
        rle_data = read_encoded_blocks(encoding_type="RLE")
        delta_data = read_encoded_blocks(encoding_type="Delta")
        
        print("Performing Huffman coding...")
        perform_huffman_coding({"RLE": rle_data, "Delta": delta_data})
        
        print("\nHuffman Coding Results:")
        huffman_dir = "hw_output/huffman"
        for component in ['Y', 'Cb', 'Cr']:
            for encoding in ['rle', 'delta']:
                codes_file = f"{huffman_dir}/{component}_{encoding}_codes.txt"
                data_file = f"{huffman_dir}/{component}_{encoding}_data.txt"
                
                if os.path.exists(codes_file):
                    with open(codes_file, 'r') as f:
                        num_codes = len(f.readlines())
                    print(f"{component} {encoding.upper()}: {num_codes} unique codes")
        print("Creating bitstreams...")
        create_bitstream(rle_data, delta_data)
        print("\nAnalyzing Huffman table statistics...")
        analyze_huffman_table_statistics()
        
        # Calculate compression ratio
        original_size = y.size + cb.size + cr.size
        # Get compressed size from bitstream files
        compressed_size = sum(os.path.getsize(f"hw_output/bitstream/{c.lower()}_encoded.bin") 
                            for c in ['Y', 'Cb', 'Cr'])
        calculate_compression_ratio(original_size, compressed_size)