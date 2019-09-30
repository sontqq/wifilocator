package com.sontme.esp.getlocation.Servers;

import android.os.AsyncTask;
import android.util.Log;

import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;

import javax.crypto.Cipher;
import javax.crypto.SealedObject;
import javax.crypto.spec.SecretKeySpec;

public class ObjectSender extends AsyncTask<Object, Object, Object> {

    final byte[] key = "1234567890000000".getBytes();
    private static final String transformation = "Blowfish";

    private Object o;

    public SealedObject encryptObject(Serializable obj) {
        try {
            SecretKeySpec sks = new SecretKeySpec(key, transformation);
            Cipher cipher = Cipher.getInstance(transformation);
            cipher.init(Cipher.ENCRYPT_MODE, sks);

            return new SealedObject(obj, cipher);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public ObjectSender(Object o) {
        this.o = o;
    }

    @Override
    protected Object doInBackground(Object... strings) {
        try {
            //Socket s = new Socket("127.0.0.1", 1234);
            Socket s = new Socket("192.168.0.157", 1234);
            ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());

            SealedObject send = encryptObject((Serializable) o);

            oos.writeObject(send);
            oos.flush();
            oos.close();

        } catch (Exception e) {
            e.printStackTrace();
            Log.d("OBJECT_SCK_", "ERROR_class_" + e.getStackTrace() + "_" + e.getCause() + "_" + e.getMessage());
        }
        return null;
    }

}