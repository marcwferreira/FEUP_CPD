import java.util.Scanner;

public class Main {

    public static boolean human_prints = true;

    public static void OnMult(int m_ar, int m_br) {
        double temp;
        double[] pha = new double[m_ar*m_br];
        double[] phb = new double[m_ar*m_br];
        double[] phc = new double[m_ar*m_br];
        
        for (int i = 0; i < m_ar; i++) {
            for (int j = 0; j < m_ar; j++) {
                pha[i*m_ar+j] = 1.0;
            }
        }
        
        for (int i = 0; i < m_br; i++) {
            for (int j = 0; j < m_br; j++) {
                phb[i*m_br+j] = (double) (i + 1);
            }
        }
        
        long startTime = System.nanoTime();
        for (int i = 0; i < m_ar; i++) {
            for (int j = 0; j < m_br; j++) {
                temp = 0;
                for (int k = 0; k < m_ar; k++) {
                    temp += pha[i*m_ar+k] * phb[k*m_ar+j];
                }
                phc[i*m_ar+j] = temp;
            }
        }

        long endTime = System.nanoTime();
        long duration = (endTime - startTime);
        if(human_prints) { System.out.println("Time: " + duration / 1000000000.0 + " seconds"); }
        else { System.out.println(duration / 1000000000.0);}

        // display 10 elements of the result matrix tto verify correctness
        if(human_prints) { System.out.println("Result matrix: "); }
        for (int i = 0; i < 1; i++) {
            for (int j = 0; j < Math.min(10, m_br); j++) {
                if(human_prints) {System.out.print(phc[i*m_ar+j] + " ");}
            }
        }
        if(human_prints) { System.out.println(); }
    }

    // multiplication using line
    public static void OnMultLine(int m_ar, int m_br) {
        long startTime = System.nanoTime();
        double temp;
        double[] pha = new double[m_ar*m_ar];
        double[] phb = new double[m_ar*m_ar];
        double[] phc = new double[m_ar*m_ar];

        for (int i = 0; i < m_ar; i++) {
            for (int j = 0; j < m_ar; j++) {
                pha[i*m_ar+j] = 1.0;
            }
        }

        for (int i = 0; i < m_br; i++) {
            for (int j = 0; j < m_br; j++) {
                phb[i*m_br+j] = (double) (i + 1);
            }
        }

        for (int i = 0; i < m_ar; i++) {
            for (int k = 0; k < m_ar; k++) {
                for (int j = 0; j < m_br; j++) {
       
                    phc[i*m_ar+j] += pha[i*m_ar+k] * phb[k*m_ar+j];
                }
            }
        }

        long endTime = System.nanoTime();
        long duration = (endTime - startTime);
        if(human_prints) { System.out.println("Time: " + duration / 1000000000.0 + " seconds"); }
        else { System.out.println(duration / 1000000000.0);}

        // display 10 elements of the result matrix tto verify correctness
        if(human_prints) { System.out.println("Result matrix: "); }
        for (int i = 0; i < 1; i++) {
            for (int j = 0; j < Math.min(10, m_br); j++) {
                if(human_prints) {System.out.print(phc[i*m_ar+j] + " "); }
            }
        }
        if(human_prints) { System.out.println(); }
    }

    // multiplication using blocks
    public static void OnMultBlock(int m_ar, int m_br, int bkSize) {

        long startTime = System.nanoTime();
        double temp;
        double[] pha = new double[m_ar*m_ar];
        double[] phb = new double[m_ar*m_ar];
        double[] phc = new double[m_ar*m_ar];

        for (int i = 0; i < m_ar; i++) {
            for (int j = 0; j < m_ar; j++) {
                pha[i*m_ar+j] = 1.0;
            }
        }

        for (int i = 0; i < m_br; i++) {
            for (int j = 0; j < m_br; j++) {
                phb[i*m_br+j] = (double) (i + 1);
            }
        }

        int a_line_block, k_block, b_col_block, a_line, k, b_col;
        int n_blocks = m_ar / bkSize;

        for(a_line_block = 0; a_line_block < n_blocks; a_line_block++) {
            for( k_block=0; k_block < n_blocks; k_block++) {	
                for( b_col_block=0; b_col_block < n_blocks; b_col_block++) {
                    int next_line_block = (a_line_block+1) * bkSize;
                    for (a_line = a_line_block*bkSize; a_line < next_line_block; a_line++) {
                        int k_next_block = (k_block + 1) * bkSize;
                        for ( k = k_block * bkSize; k < k_next_block; k++) {
                            int b_next_block = (b_col_block+1)*bkSize;
                            for (b_col = b_col_block * bkSize; b_col < b_next_block; b_col++) {
                                phc[a_line * m_ar + b_col] += pha[a_line * m_ar + k] * phb[k * m_ar + b_col];
                            }
                        }
                    }
                }
            }
        }

        long endTime = System.nanoTime();
        long duration = (endTime - startTime);
        if(human_prints) { System.out.println("Time: " + duration / 1000000000.0 + " seconds"); }
        else { System.out.println(duration / 1000000000.0);}

        // display 10 elements of the result matrix tto verify correctness
        if(human_prints) { System.out.println("Result matrix: "); }
        for (int i = 0; i < 1; i++) {
            for (int j = 0; j < Math.min(10, m_br); j++) {
                if(human_prints) { System.out.print(phc[i*m_ar+j] + " "); }
            }
        }
        if(human_prints) { System.out.println(); }
    }
    
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int op = 1; //change to receive args
        
        do {
            if(human_prints){
                System.out.println("1. Multiplication");
                System.out.println("2. Line Multiplication");
                System.out.println("3. Block Multiplication");
                System.out.print("Selection?: ");
            }
            op = sc.nextInt();
            if (op == 0) {
                break;
            }
            if(human_prints) { System.out.print("Dimensions: lins=cols ? "); }
            int lin = sc.nextInt();
            int col = lin;
            System.out.print(lin+",");

            switch (op) {
                case 1:
                    OnMult(lin, col);
                    break;
                case 2:
                    OnMultLine(lin, col);
                    break;
                case 3:
                    if(human_prints) { System.out.print("Block Size? "); }
                    int blockSize = sc.nextInt();
                    if(!human_prints) { System.out.print(blockSize+","); }
                    OnMultBlock(lin, col, blockSize);
                    break;
            }

        } while (op != 0 && human_prints);                         

        sc.close();
    }

}
