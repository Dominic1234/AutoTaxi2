package com.sharma.dhruv.autotaxi;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.app.ListActivity;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.security.NoSuchProviderException;
import java.util.Set;


public class MainActivity extends ListActivity
{
    BluetoothAdapter BA;
    //private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BA = BluetoothAdapter.getDefaultAdapter();
        // Does the device have BT capabilities?
        if (BA == null) {
            Toast.makeText(this, "Sorry, your device seems not to have bluetooth capabilities.", Toast.LENGTH_LONG).show();
            //finish();
        }
    }

    public void connect(View view) {
        if (!BA.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }
        Set<BluetoothDevice> pairedDevices = BA.getBondedDevices();

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        for (BluetoothDevice pairedDevice  : pairedDevices) {
            adapter.add(pairedDevice.getName());
        }
        adapter.notifyDataSetChanged();
        setListAdapter(adapter);

    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        String btName = (String) getListAdapter().getItem(position);
        ArduinoBT arduino = ArduinoBT.getInstance();

        try {

            // Disconnect previous device
            arduino.removeAllReceiveListeners();
            if (arduino.isConnected()) {
                arduino.disconnect();
            }

            // Connect new device
            arduino.setup(this, btName);


            // Close chooser activity
            finish();
            Intent intent = new Intent(this, Main2Activity.class);
            startActivity(intent);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Problem occured: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (NoSuchProviderException e) {
            Toast.makeText(this, btName + " was not found.", Toast.LENGTH_LONG).show();
        }
    }

}