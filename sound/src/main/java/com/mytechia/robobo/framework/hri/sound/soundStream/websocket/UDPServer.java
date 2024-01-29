package com.mytechia.robobo.framework.hri.sound.soundStream.websocket;

import android.nfc.Tag;
import android.util.Log;

import com.mytechia.robobo.framework.LogLvl;
import com.mytechia.robobo.framework.RoboboManager;

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
    private String tag;

    private RoboboManager manager;

    Thread senderThread;
    Thread receiverThread;
    Thread cleanupThread;

    public UDPServer(int bufferSize, String tag, RoboboManager m) {
        this.bufferSize = bufferSize;
        this.tag = tag;
        this.manager = m;
    }

    public void stopServerRunning(){
        Thread moribund;
        if(senderThread != null){
            moribund = senderThread;
            senderThread = null;
            moribund.interrupt();
        }
        if(receiverThread != null){
            moribund = receiverThread;
            receiverThread = null;
            moribund.interrupt();
        }
        if(cleanupThread != null){
            moribund = cleanupThread;
            cleanupThread = null;
            moribund.interrupt();
        }
        connectedClients.clear();
        lastClientActivity.clear();
        packetQueue.clear();
    }

    @Override
    public void run(){
        try {
            manager.log(LogLvl.DEBUG, tag, String.format("Audio stream server listening on %s:%d", getIPAddress(true), PORT));
            socket = new DatagramSocket(PORT);

            senderThread = new Thread(this::sendDataToClients);
            senderThread.start();

            receiverThread = new Thread(this::receiveDataFromClients);
            receiverThread.start();

            //cleanupThread = new Thread(this::cleanupInactiveClients);
            //cleanupThread.start();
        } catch (Exception e) {
            manager.logError(tag, e.getMessage(), e);
        }
    }

    public void addToQueue(byte[] data) {
        try {
            packetQueue.put(data);
        } catch (InterruptedException e) {
            manager.logError(tag, e.getMessage(), e);
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
                manager.logError(tag, e.getMessage(), e);
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
                manager.logError(tag, e.getMessage(), e);
            }
        }
    }

    private void processMessage(String message, String clientKey, InetAddress clientAddress) {
        if ("CONNECT-AUDIO".equals(message)) {
            if (!connectedClients.containsKey(clientKey)) {
                manager.log(LogLvl.DEBUG, tag, String.format("New client connected %s", clientAddress));
                connectedClients.put(clientKey, clientAddress);
                lastClientActivity.put(clientKey, System.currentTimeMillis());
            }
        }
        if (lastClientActivity.containsKey(clientKey)) {
            lastClientActivity.put(clientKey, System.currentTimeMillis());
        }
        if ("DISCONNECT-AUDIO".equals(message)) {
            manager.log(LogLvl.DEBUG, tag, String.format("Client disconnected from %s", clientAddress));
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
                    String clientAddress = connectedClients.get(clientKey).getHostAddress();
                    connectedClients.remove(clientKey);
                    lastClientActivity.remove(clientKey);
                    manager.log(LogLvl.DEBUG, tag, String.format("Client disconnected due to inactivity from %s", clientAddress));
                }
            }
            // Espera antes de volver a verificar la inactividad de los clientes
            try {
                Thread.sleep(1000); // Comprueba cada 10 segundos (ajusta según tus necesidades)
            } catch (InterruptedException e) {
                manager.logError(tag, e.getMessage(), e);
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