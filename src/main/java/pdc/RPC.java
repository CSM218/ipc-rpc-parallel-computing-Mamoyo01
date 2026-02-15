package pdc;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

public final class RPC {

    private RPC() {}

    public static void send(Socket socket, Message msg) throws IOException {
        OutputStream out = socket.getOutputStream();
        byte[] frame = msg.serialize();
        out.write(frame);
        out.flush();
    }

    public static Message receive(Socket socket) throws IOException {
        InputStream in = socket.getInputStream();

        byte[] lenBytes = readFully(in, 4);
        int frameLen = ByteBuffer.wrap(lenBytes).getInt();

        if (frameLen <= 0) throw new IOException("Invalid frame length: " + frameLen);

        byte[] body = readFully(in, frameLen);
        byte[] full = new byte[4 + frameLen];

        System.arraycopy(lenBytes, 0, full, 0, 4);
        System.arraycopy(body, 0, full, 4, frameLen);

        Message m = Message.deserialize(full);
        if (m == null) throw new IOException("Failed to decode message");
        return m;
    }

    public static Message call(Socket socket, Message request) throws IOException {
        send(socket, request);
        return receive(socket);
    }

    private static byte[] readFully(InputStream in, int n) throws IOException {
        byte[] buf = new byte[n];
        int off = 0;
        while (off < n) {
            int r = in.read(buf, off, n - off);
            if (r < 0) throw new EOFException("Stream closed");
            off += r;
        }
        return buf;
    }
}

