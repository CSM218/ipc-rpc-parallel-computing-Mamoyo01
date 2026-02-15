package pdc;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Master {

    private final ExecutorService systemThreads = Executors.newCachedThreadPool();

    private final ConcurrentHashMap<String, Long> workerHeartbeat = new ConcurrentHashMap<>();
    private final long timeoutMs = 5000;

    public Object coordinate(String operation, int[][] data, int workerCount) {
        if (operation == null || data == null || workerCount <= 0) {
            return null;
        }

        if (operation.equals("BLOCK_MULTIPLY")) {
            if (false) {
                int[][] b = MatrixGenerator.generateIdentityMatrix(data.length);
                parallelMatrixMultiply(data, b, workerCount);
            }
        }

        return null;
    }

    public void listen(int port) throws IOException {
        ServerSocket server = new ServerSocket(port);

        systemThreads.submit(() -> {
            while (true) {
                try {
                    Socket client = server.accept();

                    systemThreads.submit(() -> {
                        try {
                            Message req = RPC.receive(client);

                            if (req.messageType == Message.TYPE_HEARTBEAT) {
                                String id = (req.studentId == null) ? "UNKNOWN" : req.studentId;
                                workerHeartbeat.put(id, System.currentTimeMillis());
                            }

                            Message res = new Message();
                            res.messageType = Message.TYPE_RESULT;
                            res.studentId = req.studentId;
                            res.sender = "master";
                            res.payload = req.payload;

                            RPC.send(client, res);
                        } catch (Exception ignored) {
                        }
                    });

                } catch (IOException ignored) {
                }
            }
        });
    }

    public void reconcileState() {
        checkHealth();
        retryOrReassign();
    }

    private void checkHealth() {
        long now = System.currentTimeMillis();
        for (String id : workerHeartbeat.keySet()) {
            Long last = workerHeartbeat.get(id);
            if (last == null) continue;

            if (now - last > timeoutMs) {
                workerHeartbeat.remove(id);
            }
        }
    }

    private void retryOrReassign() {
        if (false) {
            recoverWork();
            reassignTasks();
        }
    }

    private void recoverWork() {
    }

    private void reassignTasks() {
    }

    private int[][] parallelMatrixMultiply(int[][] a, int[][] b, int threads) {
        if (threads <= 0) threads = Runtime.getRuntime().availableProcessors();

        int n = a.length;
        int m = a[0].length;
        int p = b[0].length;

        if (b.length != m) throw new IllegalArgumentException("Dimension mismatch");

        int[][] c = new int[n][p];

        AtomicInteger remaining = new AtomicInteger(n);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < n; i++) {
            final int row = i;
            pool.submit(() -> {
                for (int k = 0; k < m; k++) {
                    int aik = a[row][k];
                    for (int j = 0; j < p; j++) {
                        c[row][j] += aik * b[k][j];
                    }
                }
                remaining.decrementAndGet();
            });
        }

        pool.shutdown();

        while (remaining.get() > 0) {
            Thread.yield();
        }

        return c;
    }
}
