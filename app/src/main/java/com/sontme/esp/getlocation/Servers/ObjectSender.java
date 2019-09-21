package com.sontme.esp.getlocation.Servers;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.sontme.esp.getlocation.BackgroundService;

import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SealedObject;

public class ObjectSender extends AsyncTask<Object, Object, Object> {
    public class objectWrapper implements Serializable {
        Object o;

        objectWrapper(Object obj) {
            this.o = obj;
        }

        public Object getObj() {
            return o;
        }

        public void setObj(Object obj) {
            this.o = obj;
        }
    }
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

            // Create cipher
            KeyGenerator gen = KeyGenerator.getInstance("AES");
            gen.init(256);
            Key sKey = gen.generateKey();

            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.ENCRYPT_MODE, sKey);

            Object objectToSend = o;

            SealedObject so = new SealedObject((Serializable) objectToSend, c);

            Socket s = new Socket("127.0.0.1", 1234);

            ObjectOutputStream oOut = new ObjectOutputStream(s.getOutputStream());

            oOut.writeObject(sKey);
            oOut.writeObject(so);
            oOut.flush();
            oOut.close();

        } catch (Exception e) {
            e.printStackTrace();
            Log.d("OBJECT_SCK_", "ERROR_class_" + e.getStackTrace() + "_" + e.getCause() + "_" + e.getMessage());
        }
        return null;
    }

}