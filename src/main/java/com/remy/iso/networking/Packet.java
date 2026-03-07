package com.remy.iso.networking;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Packet {

    private ByteBuffer buffer;

    public int readOffset = 0;

    /** Create a packet with an initial size (for writing) */
    public Packet(Outgoing id) {
        buffer = ByteBuffer.allocate(1024);
        buffer.putShort((short) id.getId()); // write packet ID
    }

    public Packet() {
    }

    public void wrap(byte[] data) {
        buffer = ByteBuffer.wrap(data);
    }

    /* ---------- WRITE METHODS ---------- */

    public void writeByte(int value) {
        buffer.put((byte) value);
    }

    public void writeShort(int value) {
        buffer.putShort((short) value);
    }

    public void writeInt(int value) {
        buffer.putInt(value);
    }

    public void writeFloat(float value) {
        buffer.putFloat(value);
    }

    public void writeDouble(double value) {
        buffer.putDouble(value);
    }

    public void writeString(String value) {
        byte[] strBytes = value.getBytes(StandardCharsets.UTF_8);
        writeShort(strBytes.length);
        buffer.put(strBytes);
    }

    /* ---------- READ METHODS ---------- */

    public int readByte() {
        int val = buffer.get(readOffset) & 0xFF;
        readOffset += 1;
        return val;
    }

    public int readShort() {
        int val = buffer.getShort(readOffset);
        readOffset += 2;
        return val;
    }

    public int readInt() {
        int val = buffer.getInt(readOffset);
        readOffset += 4;
        return val;
    }

    public float readFloat() {
        float val = buffer.getFloat(readOffset);
        readOffset += 4;
        return val;
    }

    public double readDouble() {
        double val = buffer.getDouble(readOffset);
        readOffset += 8;
        return val;
    }

    public String readString() {
        int length = readShort();
        byte[] strBytes = new byte[length];
        buffer.get(readOffset, strBytes);
        readOffset += length;
        return new String(strBytes, StandardCharsets.UTF_8);
    }

    /* ---------- UTILITY ---------- */

    /** Get the raw byte array to send over TCP (only written portion) */
    public byte[] toArray() {
        int len = buffer.position();
        byte[] data = new byte[len];
        buffer.rewind();
        buffer.get(data, 0, len);
        return data;
    }

    /** Reset offset (for reading from beginning again) */
    public void reset() {
        readOffset = 0;
    }

    public <T> T[] readArray(Class<T> type, PacketArrayReader<T> reader) {
        int length = readInt();
        @SuppressWarnings("unchecked")
        T[] array = (T[]) java.lang.reflect.Array.newInstance(type, length);
        for (int i = 0; i < length; i++) {
            array[i] = reader.read();
        }
        return array;
    }

    @FunctionalInterface
    public interface PacketArrayReader<T> {
        T read();
    }

    public void parse() {
    }
}