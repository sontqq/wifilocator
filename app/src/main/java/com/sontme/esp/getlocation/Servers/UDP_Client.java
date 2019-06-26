package com.sontme.esp.getlocation.Servers;

import android.content.Context;
import android.os.AsyncTask;

import com.sontme.esp.getlocation.BackgroundService;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDP_Client extends AsyncTask<String, String, String> {

    private int port;
    private Context ctx;

    public UDP_Client(String ip, int port, Context ctx) {
        this.port = port;
        this.ctx = ctx;
    }

    @Override
    protected String doInBackground(String... strings) {
        try {
            InetAddress IPAddress;
            if (BackgroundService.check_if_local(ctx) == true) {
                IPAddress = InetAddress.getByName("192.168.0.43");
            } else {
                IPAddress = InetAddress.getByName("89.134.254.96");
            }
            String prefix = BackgroundService.DEVICE_ACCOUNT2 + " _ ";
            String tosend = prefix + strings[0];
            byte[] tosenddata = tosend.getBytes();
            DatagramSocket serverSocket = new DatagramSocket();

            DatagramPacket sendPacket = new DatagramPacket(tosenddata, tosenddata.length, IPAddress, port);
            serverSocket.send(sendPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

