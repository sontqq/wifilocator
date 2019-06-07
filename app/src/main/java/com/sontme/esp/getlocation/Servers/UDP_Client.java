package com.sontme.esp.getlocation.Servers;

import android.os.AsyncTask;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDP_Client extends AsyncTask<String, String, String> {

    private String ip;
    private int port;

    public UDP_Client(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    @Override
    protected String doInBackground(String... strings) {
        try {
            DatagramSocket serverSocket = new DatagramSocket();
            InetAddress IPAddress = InetAddress.getByName(ip);
            byte[] sendData = strings[0].getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
            serverSocket.send(sendPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
