package com.example.recordingsmartphoneapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Set;
import java.util.UUID;

public class ESP32BluetoothEventListener {

    private static final String TAG = "ESP32BluetoothListener";
    private static final String EVENT_TRIGGER_REQUEST_STRING = "EVENT TRIGGERED";

    private static final int ENABLE_BLUETOOTH_REQUEST_INTENT_ID = 1;

    private BluetoothAdapter bluetoothAdapter;

    private BluetoothDevice deviceToListenTo;

    private BluetoothSocket socketForDevice;
    private PrintWriter socketForDeviceOut;
    private BufferedReader socketForDeviceIn;

    public void detectAndInitBluetoothAdapter(Activity callingActivity) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            //Log.d(TAG, "Device doesn't support bluetooth!");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            //Log.d(TAG, "Gotta get user to enable bluetooth!");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            callingActivity.startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_INTENT_ID);
        }
    }

    public boolean initializeDeviceToListenTo(String deviceName) {
        // Query all paired devices and find the one with the target device name
        // Assign that device to the deviceToListenTo reference
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                //String name = device.getName();
                //String hardwareAddress = device.getAddress(); // MAC address
                //Log.d(TAG, "Name: " + deviceName + "  Address: " + deviceHardwareAddress);

                if (device.getName().equals(deviceName)) {
                    deviceToListenTo = device;
                    return true; // True for success
                }
            }
        }
        return false; // False for failure
    }

    public boolean initializeSocketForDevice(UUID uuid) {
        try {
            socketForDevice = deviceToListenTo.createRfcommSocketToServiceRecord(uuid);
        } catch (IOException ioe) {
            //Log.e(TAG, "Socket's create() method failed", e);
            return false;
        }
        return true;
    }

    public boolean connectToDeviceAndOpenStreams() {
        bluetoothAdapter.cancelDiscovery();

        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            socketForDevice.connect();
        } catch (IOException connectException) {
            //Log.e(TAG, "Unable to connect");
            try {
                socketForDevice.close();
            } catch (IOException closeException) {
                //Log.e(TAG, "Could not close the client socket", closeException);
            }
            return false;
        }

        //Log.d(TAG, "Connected successfully");

        // Open streams
        try {
            socketForDeviceOut = new PrintWriter(socketForDevice.getOutputStream());
            socketForDeviceIn = new BufferedReader(new InputStreamReader(socketForDevice.getInputStream()));
        } catch (IOException ioe) {
            //Log.e(TAG, "Couldn't create the output/input streams.");
            return false;
        }
        // Log.d(TAG, "Created input/output streams!");

        return true;
    }

    public void sendHandshake() {
        socketForDeviceOut.println("ready");
        socketForDeviceOut.flush();
    }

    public void waitForInput() throws IOException {
        while (!socketForDeviceIn.ready()) {
        }
    }

    public void readAllAvailableInput() throws IOException {
        // read everything that is there
        while (socketForDeviceIn.ready()) {
            Log.d(TAG, socketForDeviceIn.readLine());
        }
    }

    public void readEventTriggerRequest() throws IOException {
        String receivedRequest = "";
        // read everything that is there
        while (socketForDeviceIn.ready() && !receivedRequest.equals(EVENT_TRIGGER_REQUEST_STRING)) {
            receivedRequest += (char) socketForDeviceIn.read();
            //Log.d(TAG, socketForDeviceIn.readLine());
        }
        Log.d(TAG, receivedRequest);
    }

    public void destruct() {
        try {
            socketForDeviceIn.close();
            socketForDeviceOut.close();
            socketForDevice.close();
        } catch (IOException ioe) {
        }
    }

}
