package com.sharma.dhruv.autotaxi;

import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;

public class Main2Activity extends Activity {
    public int pickx = 0, picky = 0, dropx = 0, dropy = 0;
    public int mode = 0; //0= initialise pickup, 1 = initialise dropoff
    public BluetoothAdapter myBluetooth = null;
    BluetoothSocket mmSocket;
    BluetoothDevice device;
    public OutputStream mmOutStream=null;
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // MAC-address of Bluetooth module (you must edit this line)
    public static String address = "02:AA:06:0D:44:3B";


    // Two things are needed to make a connection:
    static final String TAG = "MY_APP_DEBUG_TAG";
    interface MessageConstants {
        int MESSAGE_READ = 0;
        int MESSAGE_WRITE = 1;
        int MESSAGE_TOAST = 2;

        // ... (Add other message types here as needed.)
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        AddTouchListener();
        //write();
       main();

    }

    public void AddTouchListener() {
        ImageView image = (ImageView) findViewById(R.id.imageView2);
        image.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                float x = event.getX();
                float y = event.getY();

                return true;
            }
        });
    }

    public void touchPos() {

    }

    public void main() {
       int[][] track = new int[30][28];
        File file = new File ("main/res/track.txt");
        Scanner scanner = null;
        try {
            scanner = new Scanner(file);
            Log.d("after ","scanner");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        for(int i = 0; i < 27; i++){
            for(int j = 0; j < 30; j++) track[j][i] = scanner.nextInt();
        }
        AddTouchListener();

        //ImageView image = (ImageView) findViewById(R.id.imageView4);
        //int width = image.getWidth();
        //int height = image.getHeight();*/
        //write(mmBuffer);

        try {
            myBluetooth = BluetoothAdapter.getDefaultAdapter();

            device = myBluetooth.getRemoteDevice(address);

            mmOutStream = mmSocket.getOutputStream();

        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and output stream creation failed:" + e.getMessage() + ".");
        }

    }
    public void errorExit(String title, String message){
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }

    // Call this from the main activity to send data to the remote device.
    public void write(byte[] bytes) {
        Log.d("tag1","inside write");
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        CharSequence text = "S";
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
       try {
             mmOutStream.write(bytes);
            Context context1 = getApplicationContext();
            int duration1 = Toast.LENGTH_LONG;
            CharSequence text1 = "Success";
            Toast toast1 = Toast.makeText(context1, text1, duration1);
            toast1.show();

        } catch (IOException e) {
         Log.e(TAG, "Error occurred when sending data", e);
            Context context2 = getApplicationContext();
            int duration2 = Toast.LENGTH_LONG;
            CharSequence text2 = "failure";
            Toast toast2 = Toast.makeText(context2, text2, duration2);
            toast2.show();}
    }


    // Call this method from the main activity to shut down the connection.

    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the connect socket", e);
        }
    }
}
