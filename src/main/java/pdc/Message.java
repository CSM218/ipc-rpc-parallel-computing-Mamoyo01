package pdc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Message {

    public String magic;
    public int version;

    public byte messageType;
    public String studentId;

    public String sender;
    public long timestamp;
    public byte[] payload;

    public static final String MAGIC_STR = "CSM218";
    public static final int MAGIC_INT = 0x43534D32; // "CSM2" marker
    public static final short VERSION = 1;

    // message types (simple codes)
    public static final byte TYPE_HELLO = 1;
    public static final byte TYPE_TASK = 2;
    public static final byte TYPE_RESULT = 3;
    public static final byte TYPE_HEARTBEAT = 4;
    public static final byte TYPE_ERROR = 5;

    public Message() {
        this.magic = MAGIC_STR;
        this.version = VERSION;
        this.timestamp = System.currentTimeMillis();
        this.payload = new byte[0];
        this.sender = "";
        this.studentId = "";
        this.messageType = 0;
    }

    public byte[] pack() {
        byte[] sid = encode(studentId);
        byte[] snd = encode(sender);
        byte[] pay = (payload == null) ? new byte[0] : payload;

        int frameLen =
                4 +
                2 +
                1 +
                8 +
                2 + sid.length +
                2 + snd.length +
                4 + pay.length;

        ByteBuffer buf = ByteBuffer.allocate(4 + frameLen);

        buf.putInt(frameLen);
        buf.putInt(MAGIC_INT);
        buf.putShort((short) (version & 0xFFFF));
        buf.put(messageType);
        buf.putLong(timestamp);

        putWithLength(buf, sid);
        putWithLength(buf, snd);

        buf.putInt(pay.length);
        buf.put(pay);

        return buf.array();
    }

    public static Message unpack(byte[] data) {
        if (data == null || data.length < 4) return null;

        try {
            ByteBuffer buf = ByteBuffer.wrap(data);

            int possibleLen = buf.getInt(0);
            boolean hasPrefix = (data.length >= 8) && (buf.getInt(4) == MAGIC_INT);

            if (hasPrefix) {
                int required = 4 + possibleLen;
                if (data.length < required) return null;
                buf.position(4);
            } else {
                buf.position(0);
            }

            int magicInt = buf.getInt();
            if (magicInt != MAGIC_INT) return null;

            Message m = new Message();
            m.magic = MAGIC_STR;

            m.version = buf.getShort() & 0xFFFF;
            m.messageType = buf.get();
            m.timestamp = buf.getLong();

            m.studentId = readString(buf);
            m.sender = readString(buf);

            int payloadLen = buf.getInt();
            if (payloadLen < 0 || payloadLen > buf.remaining()) return null;

            m.payload = new byte[payloadLen];
            buf.get(m.payload);

            return m;

        } catch (Exception e) {
            return null;
        }
    }

    public byte[] serialize() {
        return pack();
    }

    public static Message deserialize(byte[] data) {
        return unpack(data);
    }

    private static byte[] encode(String s) {
        if (s == null) return new byte[0];
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static void putWithLength(ByteBuffer buf, byte[] b) {
        if (b == null) b = new byte[0];
        buf.putShort((short) (b.length & 0xFFFF));
        buf.put(b);
    }

    private static String readString(ByteBuffer buf) throws IOException {
        if (buf.remaining() < 2) throw new IOException();
        int len = buf.getShort() & 0xFFFF;
        if (len > buf.remaining()) throw new IOException();
        byte[] b = new byte[len];
        buf.get(b);
        return new String(b, StandardCharsets.UTF_8);
    }
}
