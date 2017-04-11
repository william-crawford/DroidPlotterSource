package com.rjl.droidplotter;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.rjl.droidplotter.MainPlot.btHandler;

public class BlueToothThread extends Thread{
    private InputStream btInputStream;
    private OutputStream btOutputStream ;

    private byte[] buffData = new byte[8192];  // buffer store for the stream

    BlueToothThread(BluetoothSocket btSocket) {

        try {
            btInputStream = btSocket.getInputStream();
            btOutputStream = btSocket.getOutputStream();
        } catch (IOException iOe) {
            btInputStream = null;
        }
    }

    public void run() {

        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream
                int bytes = btInputStream.read(buffData);
                btHandler.obtainMessage(1, bytes, -1, buffData).sendToTarget();		// Send to message queue Handler
            } catch (IOException e) {
                break;
            }
        }
    }

    /* Call this from the main activity to send data to the remote device */
    void SendData(String message) {
        byte[] msgBuffer = message.getBytes();
        try {
            btOutputStream.write(msgBuffer);
        } catch (IOException e) {
            Log.d(" ", "...Error data send: " + e.getMessage() + "...");
        }
    }
}
