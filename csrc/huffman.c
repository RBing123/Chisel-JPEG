#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define MAX_TREE_HT 100
#define MAX_PAIRS 1000
#define MAX_CODE_LEN 32
#define BITS_PER_BYTE 8

// RLE pair structure
typedef struct {
    int run_length;
    int value;
} RLEPair;

// Structure to store Huffman codes
typedef struct {
    RLEPair pair;
    char code[MAX_CODE_LEN];
    int code_len;
} HuffmanCode;

// Huffman tree node structure
typedef struct Node {
    RLEPair data;
    int frequency;
    struct Node *left, *right;
} Node;

// Min heap node structure
typedef struct MinHeap {
    int size;
    int capacity;
    Node** array;
} MinHeap;

// Bit writer for compression
typedef struct {
    unsigned char buffer;
    int bits_count;
    FILE* output_file;
    unsigned char* all_bytes;  // Store all byte
    int byte_count;           // current byte
    int total_capacity;       // Capacity
} BitWriter;

// Bit reader for decompression
typedef struct {
    unsigned char buffer;
    int bits_count;
    FILE* input_file;
} BitReader;

// Initialize bit writer
BitWriter* createBitWriter(const char* filename) {
    BitWriter* writer = (BitWriter*)malloc(sizeof(BitWriter));
    writer->buffer = 0;
    writer->bits_count = 0;
    writer->output_file = fopen(filename, "wb");
    writer->total_capacity = 1024;  // init
    writer->all_bytes = (unsigned char*)malloc(writer->total_capacity);
    writer->byte_count = 0;
    return writer;
}

// Write a single bit
void writeBit(BitWriter* writer, int bit) {
    writer->buffer = (writer->buffer << 1) | (bit & 1);
    writer->bits_count++;
    
    if (writer->bits_count == BITS_PER_BYTE) {
        fwrite(&writer->buffer, 1, 1, writer->output_file);
        if (writer->byte_count >= writer->total_capacity) {
            writer->total_capacity *= 2;
            writer->all_bytes = (unsigned char*)realloc(writer->all_bytes, writer->total_capacity);
        }
        writer->all_bytes[writer->byte_count++] = writer->buffer;
        writer->buffer = 0;
        writer->bits_count = 0;
    }
}

// Flush remaining bits
void flushBitWriter(BitWriter* writer) {
    if (writer->bits_count > 0) {
        writer->buffer <<= (BITS_PER_BYTE - writer->bits_count);
        writer->all_bytes[writer->byte_count++] = writer->buffer;
        fwrite(&writer->buffer, 1, 1, writer->output_file);
    }
    
    printf("\nBit stream:\n");
    for (int i = 0; i < writer->byte_count; i++) {
        for (int j = 7; j >= 0; j--) {
            printf("%d", (writer->all_bytes[i] >> j) & 1);
        }
        printf(" ");
    }
    printf("\n");
    
    fclose(writer->output_file);
    free(writer->all_bytes);
    free(writer);
}

// Initialize bit reader
BitReader* createBitReader(const char* filename) {
    BitReader* reader = (BitReader*)malloc(sizeof(BitReader));
    reader->buffer = 0;
    reader->bits_count = 0;
    reader->input_file = fopen(filename, "rb");
    return reader;
}

// Read a single bit
int readBit(BitReader* reader) {
    if (reader->bits_count == 0) {
        fread(&reader->buffer, 1, 1, reader->input_file);
        reader->bits_count = BITS_PER_BYTE;
    }
    
    int bit = (reader->buffer >> (reader->bits_count - 1)) & 1;
    reader->bits_count--;
    return bit;
}

// Function to read RLE data from file
int readRLEFromFile(const char* filename, RLEPair* data) {
    FILE* file = fopen(filename, "r");
    if (file == NULL) {
        printf("Error opening file: %s\n", filename);
        return -1;
    }

    int count = 0;
    while (!feof(file) && count < MAX_PAIRS) {
        if (fscanf(file, "%d %d", &data[count].run_length, &data[count].value) == 2) {
            count++;
        }
    }

    fclose(file);
    return count;
}

// Function to create a new node
Node* newNode(RLEPair data, int freq) {
    Node* temp = (Node*)malloc(sizeof(Node));
    temp->left = temp->right = NULL;
    temp->data = data;
    temp->frequency = freq;
    return temp;
}

// Function to create a min heap
MinHeap* createMinHeap(int capacity) {
    MinHeap* minHeap = (MinHeap*)malloc(sizeof(MinHeap));
    minHeap->size = 0;
    minHeap->capacity = capacity;
    minHeap->array = (Node**)malloc(minHeap->capacity * sizeof(Node*));
    return minHeap;
}

// Function to swap two nodes
void swapNode(Node** a, Node** b) {
    Node* t = *a;
    *a = *b;
    *b = t;
}

// Heapify function
void minHeapify(MinHeap* minHeap, int idx) {
    int smallest = idx;
    int left = 2 * idx + 1;
    int right = 2 * idx + 2;

    if (left < minHeap->size && 
        minHeap->array[left]->frequency < minHeap->array[smallest]->frequency)
        smallest = left;

    if (right < minHeap->size && 
        minHeap->array[right]->frequency < minHeap->array[smallest]->frequency)
        smallest = right;

    if (smallest != idx) {
        swapNode(&minHeap->array[smallest], &minHeap->array[idx]);
        minHeapify(minHeap, smallest);
    }
}

// Function to build min heap
void buildMinHeap(MinHeap* minHeap) {
    int n = minHeap->size - 1;
    for (int i = (n - 1) / 2; i >= 0; --i)
        minHeapify(minHeap, i);
}

// Function to extract minimum value node
Node* extractMin(MinHeap* minHeap) {
    if (minHeap->size <= 0) return NULL;
    
    Node* temp = minHeap->array[0];
    minHeap->array[0] = minHeap->array[minHeap->size - 1];
    --minHeap->size;
    minHeapify(minHeap, 0);
    
    return temp;
}

// Function to insert a new node
void insertMinHeap(MinHeap* minHeap, Node* minHeapNode) {
    ++minHeap->size;
    int i = minHeap->size - 1;
    
    while (i && minHeapNode->frequency < minHeap->array[(i - 1) / 2]->frequency) {
        minHeap->array[i] = minHeap->array[(i - 1) / 2];
        i = (i - 1) / 2;
    }
    
    minHeap->array[i] = minHeapNode;
}

// Build Huffman Tree
Node* buildHuffmanTree(RLEPair* data, int size) {
    // First count frequencies
    int freq[MAX_PAIRS] = {0};
    int unique_count = 0;
    RLEPair unique_pairs[MAX_PAIRS];
    
    for (int i = 0; i < size; i++) {
        int found = 0;
        for (int j = 0; j < unique_count; j++) {
            if (unique_pairs[j].run_length == data[i].run_length && 
                unique_pairs[j].value == data[i].value) {
                freq[j]++;
                found = 1;
                break;
            }
        }
        if (!found) {
            unique_pairs[unique_count] = data[i];
            freq[unique_count] = 1;
            unique_count++;
        }
    }
    
    // Create a min heap
    MinHeap* minHeap = createMinHeap(unique_count);
    
    // Add all nodes to min heap
    for (int i = 0; i < unique_count; ++i)
        minHeap->array[i] = newNode(unique_pairs[i], freq[i]);
    
    minHeap->size = unique_count;
    buildMinHeap(minHeap);
    
    // Build Huffman tree
    while (minHeap->size != 1) {
        Node* left = extractMin(minHeap);
        Node* right = extractMin(minHeap);
        
        RLEPair dummy = {0, 0};
        Node* top = newNode(dummy, left->frequency + right->frequency);
        
        top->left = left;
        top->right = right;
        
        insertMinHeap(minHeap, top);
    }
    
    return extractMin(minHeap);
}

// Store Huffman codes in array
void storeCode(HuffmanCode* codes, int* code_count, Node* root, int arr[], int top) {
    if (root->left) {
        arr[top] = 0;
        storeCode(codes, code_count, root->left, arr, top + 1);
    }
    
    if (root->right) {
        arr[top] = 1;
        storeCode(codes, code_count, root->right, arr, top + 1);
    }
    
    if (!root->left && !root->right) {
        codes[*code_count].pair = root->data;
        codes[*code_count].code_len = top;
        
        char code_str[MAX_CODE_LEN] = {0};
        for (int i = 0; i < top; i++) {
            code_str[i] = arr[i] + '0';
        }
        strcpy(codes[*code_count].code, code_str);
        
        (*code_count)++;
    }
}

// Find Huffman code for a given RLE pair
const char* findCode(HuffmanCode* codes, int code_count, RLEPair pair) {
    for (int i = 0; i < code_count; i++) {
        if (codes[i].pair.run_length == pair.run_length && 
            codes[i].pair.value == pair.value) {
            return codes[i].code;
        }
    }
    return NULL;
}

void writeCompressedData(const char* output_file, RLEPair* data, int data_size, 
                        HuffmanCode* codes, int code_count) {
    BitWriter* writer = createBitWriter(output_file);

    fprintf(writer->output_file, "%d\n", code_count);
    
    // Write to coding table
    for (int i = 0; i < code_count; i++) {
        fprintf(writer->output_file, "%d %d %s\n", 
                codes[i].pair.run_length, 
                codes[i].pair.value, 
                codes[i].code);
    }
    
    fprintf(writer->output_file, "---\n");

    // Build binary.txt
    char binary_file[256];
    sprintf(binary_file, "%s.binary.txt", output_file);
    FILE* binary_out = fopen(binary_file, "w");
    
    fprintf(binary_out, "%d\n", code_count);
    for (int i = 0; i < code_count; i++) {
        fprintf(binary_out, "%d %d %s\n", 
                codes[i].pair.run_length, 
                codes[i].pair.value, 
                codes[i].code);
    }
    fprintf(binary_out, "---\n");

    // Compressed
    for (int i = 0; i < data_size; i++) {
        const char* code = findCode(codes, code_count, data[i]);
        if (code) {
            for (int j = 0; code[j]; j++) {
                writeBit(writer, code[j] - '0');
            }
        }
    }
    
    if (writer->bits_count > 0) {
        writer->buffer <<= (BITS_PER_BYTE - writer->bits_count);
        fwrite(&writer->buffer, 1, 1, writer->output_file);
        writer->all_bytes[writer->byte_count++] = writer->buffer;
    }

    for (int i = 0; i < writer->byte_count; i++) {
        for (int j = 7; j >= 0; j--) {
            fprintf(binary_out, "%d", (writer->all_bytes[i] >> j) & 1);
        }
        fprintf(binary_out, " ");
    }
    fprintf(binary_out, "\n");

    fclose(binary_out);
    fclose(writer->output_file);
    free(writer->all_bytes);
    free(writer);
}

// Function to decompress data
void decompressData(const char* output_file) {
    FILE* in = fopen("hw_output/compressed.bin", "rb");
    FILE* out = fopen(output_file, "w");
    
    if (!in || !out) {
        printf("Error opening files\n");
        return;
    }
    
    // Read code table size
    int code_count;
    fscanf(in, "%d\n", &code_count);
    
    // Read code table
    HuffmanCode codes[MAX_PAIRS];
    for (int i = 0; i < code_count; i++) {
        fscanf(in, "%d %d %s\n",
               &codes[i].pair.run_length,
               &codes[i].pair.value,
               codes[i].code);
    }
    
    // Skip marker line
    char line[256];
    fgets(line, sizeof(line), in);
    
    // Read and decode bits
    char current_code[MAX_CODE_LEN] = {0};
    int code_pos = 0;
    unsigned char byte;
    
    // Keep reading bytes until EOF
    while (fread(&byte, 1, 1, in) == 1) {
        // Process each bit in the byte
        for (int bit = 7; bit >= 0; bit--) {
            // Get current bit
            char current_bit = ((byte >> bit) & 1) + '0';
            current_code[code_pos++] = current_bit;
            current_code[code_pos] = '\0';
            
            // Try to match current code
            for (int i = 0; i < code_count; i++) {
                if (strcmp(current_code, codes[i].code) == 0) {
                    fprintf(out, "%d %d\n",
                            codes[i].pair.run_length,
                            codes[i].pair.value);
                    code_pos = 0;
                    current_code[0] = '\0';
                    break;
                }
            }
            
            if (code_pos >= MAX_CODE_LEN) {
                printf("Error: Invalid code encountered\n");
                goto cleanup;
            }
        }
    }

cleanup:
    fclose(in);
    fclose(out);
}


int main() {
    RLEPair data[MAX_PAIRS];
    HuffmanCode codes[MAX_PAIRS];
    int code_count = 0;
    
    // Read RLE data
    int size = readRLEFromFile("hw_output/rle_output.txt", data);
    if (size < 0) {
        return 1;
    }
    
    // Print read data
    printf("Read RLE data:\n");
    for (int i = 0; i < size; i++) {
        printf("%d %d\n", data[i].run_length, data[i].value);
    }
    
    // Build Huffman tree and get codes
    Node* root = buildHuffmanTree(data, size);
    int arr[MAX_TREE_HT], top = 0;
    storeCode(codes, &code_count, root, arr, top);
    
    // Write compressed data
    writeCompressedData("hw_output/compressed.bin", data, size, codes, code_count);
    
    // Write human-readable code table
    FILE* code_table = fopen("hw_output/code_table.txt", "w");
    fprintf(code_table, "Huffman Codes:\n");
    for (int i = 0; i < code_count; i++) {
        fprintf(code_table, "(%d,%d): %s\n", 
                codes[i].pair.run_length, 
                codes[i].pair.value, 
                codes[i].code);
    }
    fclose(code_table);
    
    // Test decompression
    decompressData("hw_output/decompressed.txt");
    
    printf("\nCompression completed:\n");
    printf("1. Compressed data written to 'compressed.bin'\n");
    printf("2. Code table written to 'code_table.txt'\n");
    printf("3. Decompressed data written to 'decompressed.txt'\n");
    printf("4. Binary representation written to 'compressed.bin.binary.txt'\n");
    return 0;
}