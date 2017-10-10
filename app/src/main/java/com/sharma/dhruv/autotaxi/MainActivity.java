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
import java.util.Set;


public class MainActivity extends AppCompatActivity
{
    BluetoothAdapter BA;
    private static final String TAG = MainActivity.class.getSimpleName();

    /*BluetoothSocket mmSocket;
    InputStream mmInStream;
    OutputStream mmOutStream;
    byte[] mmBuffer;
    BluetoothServerSocket mmServerSocket;
    public BluetoothAdapter myBluetooth = null;
    public Set pairedDevices;
    Handler mHandler = new Handler();
*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BA = BluetoothAdapter.getDefaultAdapter();
        // BT not enabled, try to enable it (interactively)
        if(!BA.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }
    }

    public void connect(View view) {
        Set<BluetoothDevice> pairedDevices = BA.getBondedDevices();

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        for (BluetoothDevice pairedDevice  : pairedDevices) {
            adapter.add(pairedDevice.getName());
        }
        adapter.notifyDataSetChanged();
        // setListAdapter(adapter);

        Intent intent = new Intent(this, Main2Activity.class);
                startActivity(intent);
        // Main2Activity.btPaired();
    }

}




