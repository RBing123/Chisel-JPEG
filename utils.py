import numpy as np
from PIL import Image
import argparse

if __name__ == "__main__":
    # 設定命令行參數解析
    parser = argparse.ArgumentParser(description='Convert image to BMP format')
    parser.add_argument('input_path', type=str, help='Input image path')
    parser.add_argument('--output', '-o', type=str, default='output.bmp', 
                        help='Output BMP file path (default: output.bmp)')
    parser.add_argument('--quality', '-q', type=int, default=100,
                        help='Output quality 0-100 (default: 100)')
    
    args = parser.parse_args()
    
    # 開啟圖片
    img = Image.open(args.input_path)
    
    # 確保圖片是 RGB 模式
    if img.mode != 'RGB':
        img = img.convert('RGB')
    
    # 儲存為 BMP
    img.save(args.output, "BMP", quality=args.quality)