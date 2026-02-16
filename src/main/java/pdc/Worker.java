package pdc;

import java.io.IOException;
import java.net.Socket;

public class Worker {

    private Socket socket;
    private volatile boolean running = false;

    // connect to master and do a simple rpc-style request/response handshake
    public void joinCluster(String masterHost, int port) {
        try {
            this.socket = new Socket(masterHost, port);

            Message request = new Message();
            request.messageType = Message.TYPE_HELLO;
            request.studentId = System.getenv().getOrDefault("CSM218_STUDENT_ID", "UNKNOWN");
            request.sender = "worker";
            request.payload = new byte[0];

            Message response = RPC.call(this.socket, request);

            this.running = (response != null);
        } catch (IOException e) {
            this.running = false;
        }
    }

    public void execute() {
        if (socket == null || !running) return;

        try {
            while (running) {
                Message request = RPC.receive(socket);

                if (request.messageType == Message.TYPE_TASK) {
                    Message result = new Message();
                    result.messageType = Message.TYPE_RESULT;
                    result.studentId = request.studentId;
                    result.sender = "worker";
                    result.payload = request.payload; // placeholder until task logic is added
                    RPC.send(socket, result);
                } else if (request.messageType == Message.TYPE_HEARTBEAT) {
                    Message pong = new Message();
                    pong.messageType = Message.TYPE_HEARTBEAT;
                    pong.studentId = request.studentId;
                    pong.sender = "worker";
                    pong.payload = new byte[0];
                    RPC.send(socket, pong);
                } else {
                    // ignore unknown message types for now
                }
            }
        } catch (IOException e) {
            running = false;
        }
    }
}
