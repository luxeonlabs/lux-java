package com.remy.iso.networking;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import com.remy.iso.networking.handlers.AuthHandler;
import com.remy.iso.networking.handlers.RoomHandler;
import com.remy.iso.networking.outgoing.auth.AuthToken;

public class GameClient {
    private static GameClient instance;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private Thread pollingThread;
    private volatile boolean isRunning = false;
    private final ConcurrentLinkedQueue<Runnable> callbackQueue = new ConcurrentLinkedQueue<>();

    private byte[] buffer = new byte[0];
    private ByteArrayOutputStream readBuffer = new ByteArrayOutputStream();

    // Map packet ID → factory + callback
    private final Map<Integer, PacketHandler<? extends Packet>> handlers = new HashMap<>();

    // Inner class to store factory + callback
    private static class PacketHandler<T extends Packet> {
        PacketFactory<T> factory;
        Consumer<T> callback;

        PacketHandler(PacketFactory<T> factory, Consumer<T> callback) {
            this.factory = factory;
            this.callback = callback;
        }
    }

    // Factory interface to create packets from bytes
    public interface PacketFactory<T extends Packet> {
        T create();
    }

    // Register a packet handler
    public <T extends Packet> void register(Incoming id, PacketFactory<T> factory, Consumer<T> callback) {
        handlers.put(id.getId(), new PacketHandler<>(factory, callback));
    }

    public GameClient() {
        instance = this;
        new AuthHandler();
        new RoomHandler();
    }

    public static GameClient getInstance() {
        if (instance == null)
            instance = new GameClient();
        return instance;
    }

    // Connect to server
    public void connect(String host, int port, String sso) {
        try {
            socket = new Socket(host, port);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            System.out.println("Connected to " + host + ":" + port);
            this.send(new AuthToken(sso));

            startPolling();
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    // Send a packet
    public void send(Packet packet) {
        try {
            byte[] data = packet.toArray();
            out.writeInt(data.length); // length prefix
            out.write(data);
            out.flush();
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    private void startPolling() {
        isRunning = true;
        pollingThread = new Thread(() -> {
            while (isRunning) {
                try {
                    poll();
                    Thread.sleep(10); // Small delay to avoid spinning
                } catch (IOException e) {
                    System.err.println("Polling error: " + e.getMessage());
                    e.printStackTrace();
                    isRunning = false;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        pollingThread.setName("NetworkPollingThread");
        pollingThread.setDaemon(true);
        pollingThread.start();
    }

    // Poll incoming packets
    public void poll() throws IOException {
        byte[] temp = new byte[1024];
        int read = in.read(temp);
        if (read <= 0)
            return;

        readBuffer.write(temp, 0, read);

        byte[] buf = readBuffer.toByteArray();
        int offset = 0;

        while (buf.length - offset >= 4) {
            int length = ByteBuffer.wrap(buf, offset, 4).getInt();
            if (buf.length - offset - 4 < length)
                break;

            byte[] packetData = Arrays.copyOfRange(buf, offset + 4, offset + 4 + length);
            int packetId = ByteBuffer.wrap(packetData, 0, 2).getShort();

            PacketHandler<?> handler = handlers.get(packetId);

            if (handler != null) {
                byte[] payloadData = Arrays.copyOfRange(packetData, 2, packetData.length);
                Packet typedPacket = handler.factory.create();
                typedPacket.wrap(payloadData);
                typedPacket.reset();
                typedPacket.parse();

                // Queue the callback to run on render thread instead of running it now
                callbackQueue.add(() -> {
                    @SuppressWarnings("unchecked")
                    Consumer<Packet> callback = (Consumer<Packet>) handler.callback;
                    callback.accept(typedPacket);
                });
            } else {
                System.out.println("Unhandled packet ID: " + packetId);
            }

            offset += 4 + length;
        }

        readBuffer.reset();
        if (offset < buf.length) {
            readBuffer.write(buf, offset, buf.length - offset);
        }
    }

    // Call this from your render method
    public void processCallbacks() {
        while (!callbackQueue.isEmpty()) {
            Runnable callback = callbackQueue.poll();
            if (callback != null) {
                callback.run();
            }
        }
    }

    public void disconnect() throws IOException {
        isRunning = false;
        if (pollingThread != null) {
            try {
                pollingThread.join(5000); // Wait up to 5 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (socket != null)
            socket.close();
        System.out.println("Disconnected");
    }
}