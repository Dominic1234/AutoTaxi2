/*
 * Arduino Bluetooth Controller
 * (c) 2015 Martin Bachmann, m.bachmann@insign.ch
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * @license GPL-3.0+ <http://opensource.org/licenses/GPL-3.0>
 */

package com.sharma.dhruv.autotaxi;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;



/**
 * This class handles the connection and communication to and from the Arduino.
 * It uses a streaming RFCOMM socket on the general SPP serial device
 * service uuid 00001101-0000-1000-8000-00805f9b34fb
 *
 * Note: Currently, Bluetooth 4 BLE is not supported. BLE does not support SPP,
 * so a different implementation is required. BLE support would be required to enable
 * BLE devices like the RFDuino (http://www.rfduino.com/)
 */
public class ArduinoBT {

	private BluetoothDevice btDevice;
	private BluetoothAdapter adapter;
	private BluetoothSocket socket;
	private OutputStream outputStream;
	private InputStream inputStream;
	private boolean connected = false;

	private static ArduinoBT instance;

	public static final String DELIM_VALUE = "~";
	public static final char DELIM_PACKET = "|".charAt(0);
	private static final String TAG = "ArduinoBT";

	@SuppressLint("UseSparseArrays")
	private Map<Integer, ArrayList<ReceiveListener>> receiveListeners = new HashMap<Integer, ArrayList<ReceiveListener>>();


	/**
	 * Used to receive data received from Arduino.
	 * Can use different command channels / keys.
	 * 
	 * @author bachi
	 *
	 */
	public static abstract class ReceiveListener {

		private Integer[] channels;

		/**
		 * Receive the commands for the passed channel keys.
		 * Commands are filtered by the used channel keys.
		 * If you set no channel key or channel 0, all commands will be received.
		 * @param channel
		 */
		public ReceiveListener(Integer... channel) {

			// No channel = channel 0 = receive all packets
			if (channel.length == 0) {
				this.channels = new Integer[] {0};
				return;
			}
			
			this.channels = channel;
		}


		/**
		 * Receives any new commands (matching the selected keys) from Arduino
		 * @param key
		 * @param command
		 */
		public abstract void receive(int key, String command);
	}

	/**
	 * Private constructor - singleton.
	 */
	private ArduinoBT() {}

	/**
	 * Get the Arduino Bluetooth singleton instance
	 * @return
	 */
	public static ArduinoBT getInstance() {
		if (instance == null) instance = new ArduinoBT();
		return instance;
	}

	/**
	 * Add a new Arduino receive listener.
	 * @param listener
	 */
	public void addReceiveListener(ReceiveListener listener) {

		for (Integer channel : listener.channels) {
			if (!receiveListeners.containsKey(channel)) {
				receiveListeners.put(channel, new ArrayList<ReceiveListener>());
			}

			receiveListeners.get(channel).add(listener);
		}
	}

	/**
	 * Remove all receive listeners
	 */
	public void removeAllReceiveListeners() {
		receiveListeners.clear();
	}

	/**
	 * Called when a new packet has arrived - call the listeners
	 * listeners with key=null receive all events, those with specific keys only their events
	 *
	 * @param packet
	 */
	protected void notifyListeners(String packet) {
		// Command format: "1234<DELIM_VALUE>somePayLoad"
		if (packet.indexOf(DELIM_VALUE) == -1) {
			return;
		}
		String channelStr = packet.substring(0, packet.indexOf(DELIM_VALUE));
		String command = packet.substring(packet.indexOf(DELIM_VALUE) + 1);

		Integer channel = Integer.parseInt(channelStr);
		ArrayList<ReceiveListener> listeners = new ArrayList<ReceiveListener>();
		if (receiveListeners.containsKey(channel))
			listeners.addAll(receiveListeners.get(channel));
		
		if  (receiveListeners.containsKey(0))
			listeners.addAll(receiveListeners.get(0));
		
		for (ReceiveListener listener : listeners) {
			listener.receive(channel, command);
		}
	}
	
	/**
	 * Can be used to inject packets as if they were received from Arduino (e.g. for testing)
	 * Command format: "<channel><DELIM_VALUE>somePayLoad<DELIM_PACKET>", e.g.: "1~myCommandHere|"
	 * @param packet
	 * @return
	 */
	public void injectPacket(String packet) {
		notifyListeners(packet);
	}



	public boolean setup(Activity activity, String deviceName) throws IOException, NoSuchProviderException {
		BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

		// Does the device have BT capabilities?
		if (btAdapter == null) {
			throw new IOException("Your device has no bluetooth capabilities.");
		}

		// BT not enabled, try to enable it (interactively)
		if(!btAdapter.isEnabled())
		{
			Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			activity.startActivityForResult(enableBluetooth, 0);
		}


		Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
		if(pairedDevices.size() > 0)
		{
			for(BluetoothDevice pairedDevice : pairedDevices)
			{
				if(pairedDevice.getName().equals(deviceName))
				{
					btDevice = pairedDevice;
					break;
				}
			}

		} else {
			throw new NoSuchProviderException("No paired bt device found. Please pair your device first.");
		}

		if (btDevice == null) {
			throw new NoSuchProviderException(String.format("Cannot find device '%1s'", deviceName));
		}

		UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID

		//UUID uuid = UUID.fromString("00002220-0000-1000-8000-00805f9b34fb"); // RFduino


		socket = btDevice.createRfcommSocketToServiceRecord(uuid);
		btAdapter.cancelDiscovery();
		socket.connect(); // if it throws no exception, we're connected now
		outputStream = socket.getOutputStream();
		inputStream = socket.getInputStream();
		connected = true;
		
		beginListenForData();
		return true;
	}
	
	public void disconnect() throws IOException {
		inputStream.close();
		outputStream.close();
		socket.close();
		connected = false;
	}

	/**
	 * Returns whether we're connected to the Arduino
	 *
	 * Note: For API level < 14, this will not detect connection interruptions!
	 */
	public boolean isConnected() {
		if(android.os.Build.VERSION.SDK_INT >= 14) {
			return socket != null && socket.isConnected();
		}
		else {
			return connected;
		}
	}

	protected void beginListenForData()
	{
		Thread workerThread = new Thread(new Runnable()
		{
			Boolean stopWorker = false;
			int readBufferPosition = 0;
			byte[] readBuffer = new byte[4196];

			public void run()
			{           
				while(!Thread.currentThread().isInterrupted() && !stopWorker && connected)
				{
					try 
					{
						int bytesAvailable = inputStream.available();                        
						if(bytesAvailable > 0)
						{
							byte[] packetBytes = new byte[bytesAvailable];
							inputStream.read(packetBytes);
							for(int i=0;i<bytesAvailable;i++)
							{
								byte b = packetBytes[i];
								if(b == "|".charAt(0))
								{
									byte[] encodedBytes = new byte[readBufferPosition];
									System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
									final String packet = new String(encodedBytes, "ISO-8859-1");
									readBufferPosition = 0;

									notifyListeners(packet);								
								}
								else
								{
									readBuffer[readBufferPosition++] = b;
								}
								
							}
						}
					} 
					catch (Exception e)
					{
						stopWorker = true;
					}
				}
			}
		});

		workerThread.start();
	}


	public boolean send(int channel, String msg) {

		if (outputStream == null) return false;

		String packet = String.valueOf(channel) + DELIM_VALUE + msg + DELIM_PACKET;

		try {
			outputStream.write(packet.getBytes());
			return true;

		} catch (IOException e) {
			try {
				disconnect();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			return false;
		}
	}

	/**
	 * @return the bluetooth device
	 */
	public BluetoothDevice getDevice() {
		return btDevice;
	}

	/**
	 * @return the bluetooth adapter
	 */
	public BluetoothAdapter getAdapter() {
		return adapter;
	}

	/**
	 * @return the bluetooth socket
	 */
	public BluetoothSocket getSocket() {
		return socket;
	}

	/**
	 * @return the OutputStream
	 */
	public OutputStream getOutputStream() {
		return outputStream;
	}

	/**
	 * @return the InputStream
	 */
	public InputStream getInputStream() {
		return inputStream;
	}



}
