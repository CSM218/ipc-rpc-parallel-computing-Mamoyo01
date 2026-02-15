package pdc;

import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MatrixGenerator {

    private static final Random random = new Random();

    public static int[][] generateRandomMatrix(int rows, int cols, int maxValue) {
        int[][] matrix = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix[i][j] = random.nextInt(maxValue);
            }
        }
        return matrix;
    }

    public static int[][] generateIdentityMatrix(int size) {
        int[][] matrix = new int[size][size];
        for (int i = 0; i < size; i++) {
            matrix[i][i] = 1;
        }
        return matrix;
    }

    public static int[][] generateFilledMatrix(int rows, int cols, int value) {
        int[][] matrix = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix[i][j] = value;
            }
        }
        return matrix;
    }

    public static void printMatrix(int[][] matrix, String label) {
        if (label != null && !label.isEmpty()) {
            System.out.println(label);
        }
        for (int[] row : matrix) {
            for (int val : row) {
                System.out.printf("%6d ", val);
            }
            System.out.println();
        }
    }

    public static void printMatrix(int[][] matrix) {
        printMatrix(matrix, "");
    }

    public static int[][] multiply(int[][] a, int[][] b) {
        int n = a.length;
        int m = a[0].length;
        int p = b[0].length;

        if (b.length != m) throw new IllegalArgumentException("Dimension mismatch");

        int[][] c = new int[n][p];

        for (int i = 0; i < n; i++) {
            for (int k = 0; k < m; k++) {
                int aik = a[i][k];
                for (int j = 0; j < p; j++) {
                    c[i][j] += aik * b[k][j];
                }
            }
        }
        return c;
    }

    public static int[][] parallelMultiply(int[][] a, int[][] b, int threads) {
        if (threads <= 0) threads = Runtime.getRuntime().availableProcessors();

        int n = a.length;
        int m = a[0].length;
        int p = b[0].length;

        if (b.length != m) throw new IllegalArgumentException("Dimension mismatch");

        int[][] c = new int[n][p];

        int block = Math.max(1, n / (threads * 4));
        ConcurrentLinkedQueue<int[]> tasks = new ConcurrentLinkedQueue<>();
        for (int start = 0; start < n; start += block) {
            int end = Math.min(n, start + block);
            tasks.add(new int[] { start, end });
        }

        AtomicInteger remaining = new AtomicInteger(tasks.size());
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int t = 0; t < threads; t++) {
            pool.execute(() -> {
                int[] task;
                while ((task = tasks.poll()) != null) {
                    int startRow = task[0];
                    int endRow = task[1];

                    for (int i = startRow; i < endRow; i++) {
                        for (int k = 0; k < m; k++) {
                            int aik = a[i][k];
                            for (int j = 0; j < p; j++) {
                                c[i][j] += aik * b[k][j];
                            }
                        }
                    }

                    remaining.decrementAndGet();
                }
            });
        }

        pool.shutdown();

        while (remaining.get() > 0) {
            Thread.yield();
        }

        return c;
    }
}
