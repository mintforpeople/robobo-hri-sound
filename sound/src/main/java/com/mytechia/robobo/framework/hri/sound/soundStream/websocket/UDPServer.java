package com.mytechia.robobo.framework.hri.sound.soundStream.websocket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class UDPServer extends Thread {
    private static final long CLIENT_TIMEOUT = 3000; // 30 segundos (ajusta según tus necesidades)
    private static final int QUEUE_SIZE = 30;
    private DatagramSocket socket;
    private BlockingQueue<byte[]> packetQueue = new ArrayBlockingQueue<>(QUEUE_SIZE);
    private Map<String, InetAddress> connectedClients = new HashMap<>();
    private Map<String, Long> lastClientActivity = new HashMap<>();

    private static final int PORT = 40406;
    private int bufferSize;

    public UDPServer(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    @Override
    public void run(){
        try {
            socket = new DatagramSocket(PORT);
            Thread senderThread = new Thread(this::sendDataToClients);
            senderThread.start();

            Thread receiverThread = new Thread(this::receiveDataFromClients);
            receiverThread.start();

            Thread cleanupThread = new Thread(this::cleanupInactiveClients);
            cleanupThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addToQueue(byte[] data) {
        try {
            packetQueue.put(data);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sendDataToClients() {
        while (true) {
            try {
                byte[] data = packetQueue.take();
                for (InetAddress clientAddress : connectedClients.values()) {
                    DatagramPacket packet = new DatagramPacket(data, data.length, clientAddress, PORT);
                    socket.send(packet);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void receiveDataFromClients() {
        byte[] receiveData = new byte[bufferSize];
        while (true) {
            try {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);

                InetAddress clientAddress = receivePacket.getAddress();
                String clientKey = getClientKey(clientAddress);

                byte[] receivedData = receivePacket.getData();
                String message = new String(receivedData, 0, receivePacket.getLength());

                // Procesa el mensaje recibido
                processMessage(message, clientKey, clientAddress);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void processMessage(String message, String clientKey, InetAddress clientAddress) {
        if ("CONNECT-AUDIO".equals(message)) {
            if (!connectedClients.containsKey(clientKey)) {
                connectedClients.put(clientKey, clientAddress);
                lastClientActivity.put(clientKey, System.currentTimeMillis());
            }
        }
        if (lastClientActivity.containsKey(clientKey)) {
            lastClientActivity.put(clientKey, System.currentTimeMillis());
        }
        if ("DISCONNECT-AUDIO".equals(message)) {
            if (connectedClients.containsKey(clientKey)) {
                connectedClients.remove(clientKey);
                lastClientActivity.remove(clientKey);
            }
        }
    }

    private void cleanupInactiveClients() {
        while (true) {
            long currentTime = System.currentTimeMillis();
            for (Map.Entry<String, Long> entry : lastClientActivity.entrySet()) {
                if (currentTime - entry.getValue() > CLIENT_TIMEOUT) {
                    String clientKey = entry.getKey();
                    connectedClients.remove(clientKey);
                    lastClientActivity.remove(clientKey);
                }
            }
            // Espera antes de volver a verificar la inactividad de los clientes
            try {
                Thread.sleep(1000); // Comprueba cada 10 segundos (ajusta según tus necesidades)
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private String getClientKey(InetAddress address) {
        return address.getHostAddress();
    }

    private static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) { } // for now eat exceptions
        return "";
    }
}