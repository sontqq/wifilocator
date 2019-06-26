package com.sontme.esp.getlocation.Servers;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.sontme.esp.getlocation.BackgroundService;

import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class ObjectSender extends AsyncTask<Object, Object, Object> {

    private int port;
    private Context ctx;
    private Object o;

    public ObjectSender(String ip, int port, Context ctx) {
        this.port = port;
        this.ctx = ctx;
    }

    public ObjectSender(Object o, String ip, int port, Context ctx) {
        this.port = port;
        this.ctx = ctx;
        this.o = o;
    }

    @Override
    protected Object doInBackground(Object... strings) {
        try {
            InetAddress IPAddress;
            if (BackgroundService.check_if_local(ctx) == true) {
                IPAddress = InetAddress.getByName("192.168.0.248");
            } else {
                IPAddress = InetAddress.getByName("89.134.254.96"); // localhost/127.0.0.1
            }
            /*
            String prefix = BackgroundService.DEVICE_ACCOUNT2 + " _ ";
            String tosend = prefix + strings[0];
            */
            //Object objectToSend = strings;
            Object objectToSend = o;

            Socket s = new Socket("127.0.0.1", 1234);
            ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
            out.writeObject(objectToSend);
            out.flush();
            //out.close();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("OBJECT_SCK_", "ERROR_class_" + e.getMessage());
        }
        return null;
    }

}