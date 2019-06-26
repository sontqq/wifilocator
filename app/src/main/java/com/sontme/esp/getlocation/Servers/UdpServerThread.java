package com.sontme.esp.getlocation.Servers;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Date;
import java.util.Enumeration;

public class UdpServerThread extends Thread {

    //UdpServerThread udpServerThread = new UdpServerThread(1234);
    //udpServerThread.start();

    int serverPort;
    DatagramSocket socket;
    boolean running;

    public UdpServerThread(int serverPort) {
        super();
        this.serverPort = serverPort;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    @Override
    public void run() {
        running = true;
        try {
            socket = new DatagramSocket(serverPort);
            Log.e("udp_server", "UDP Server is running");
            while (running) {
                byte[] buf = new byte[256];

                // receive request
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);     //this code block the program flow

                // send the response to the client at "address" and "port"
                InetAddress address = packet.getAddress();
                int port = packet.getPort();


                String dString = new Date().toString() + "\n"
                        + "Your address " + address.toString() + ":" + port;
                buf = dString.getBytes();
                packet = new DatagramPacket(buf, buf.length, address, port);
                socket.send(packet);

            }

            Log.e("udp_server", "UDP Server ended");

        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                socket.close();
                Log.e("udp_server", "socket.close()");
            }
        }
    }


    private String getIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip += "SiteLocalAddress: "
                                + inetAddress.getHostAddress() + "\n";
                    }

                }

            }

        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip += "Something Wrong! " + e.toString() + "\n";
        }

        return ip;
    }
}
