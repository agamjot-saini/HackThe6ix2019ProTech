package com.example.recordingsmartphoneapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.FrameLayout;

import java.io.IOException;
import java.util.UUID;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;


public class MainActivity extends AppCompatActivity {

    private CameraRecorder frontFacingCameraRecorder;

    ESP32BluetoothEventListener esp32BluetoothEventListener;

    public static final int RECORDING_DURATION_MILLISECONDS = 10000;

    static final int REQUEST_VIDEO_CAPTURE = 1;

    // HTTP
    final String url = "http://d778fce8.ngrok.io";
    ApiService apiService;
    private final static int IMAGE_RESULT = 200;
    // Video URI
    Uri videoURI;

    public void initUI() {
        OkHttpClient client = new OkHttpClient.Builder().build();

        apiService = new Retrofit.Builder().baseUrl(url).client(client).build().create(ApiService.class);
//        recordButton = findViewById(R.id.recordButton);
//        recordButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                dispatchTakeVideoIntent();
//            }
//        });

        final Activity tempAct = this;
//
//        deleteButton = findViewById(R.id.deleteButton);
//        deleteButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (ContextCompat.checkSelfPermission(tempAct, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//                    ActivityCompat.requestPermissions(tempAct, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
//                } else {
//                    deleteFileFromURI(videoURI);
//                }
//            }
//        });

        if (ContextCompat.checkSelfPermission(tempAct, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(tempAct, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, 2);
        } else {
//            bluetoothAdapterInit();
        }


    }


    /**
     * Call back once result of permissions has come back
     *
     * @param requestCode  request code
     * @param permissions  permissions
     * @param grantResults grant results
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Video
        if (requestCode == 1) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
//                        deleteFileFromURI(videoURI);
                    } else {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                    }
                }
            }
        }
        // Bluetooth
        else if (requestCode == 2) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (permission.equals(Manifest.permission.BLUETOOTH_ADMIN)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
//                        bluetoothAdapterInit();
                    }
                }
            }
        }
    }

    /**
     * Deletes file from a URI
     *
     * @param path Path of the URI where video is stored
     */
    private void deleteFileFromURI(Uri path) {
        try {
            getContentResolver().delete(path, null, null);

        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
    }

    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    private static final String TAG = "tagg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize bluetooth
        esp32BluetoothEventListener = new ESP32BluetoothEventListener();
        esp32BluetoothEventListener.detectAndInitBluetoothAdapter(this);
        esp32BluetoothEventListener.initializeDeviceToListenTo("ESP32");
        esp32BluetoothEventListener.initializeSocketForDevice(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));

        // Initialize cameras
        frontFacingCameraRecorder = new CameraRecorder();
        frontFacingCameraRecorder.initCamera();
        frontFacingCameraRecorder.initCameraPreview(this);

        // Create a preview for the camera object, but don't make it visible to the
        // user (there is no need, as the camera will automatically start recording
        // when it receives a request to do so via bluetooth).
        FrameLayout preview = findViewById(R.id.camera_preview);
        preview.addView(frontFacingCameraRecorder.getCameraPreview());
        preview.setVisibility(View.INVISIBLE);
        //Log.d(TAG, "Created preview.");

        final TextView statusTextView = findViewById(R.id.statusTextView);

        frontFacingCameraRecorder.initializeRecordingStorageDirectory(this, "SurveillanceCamera");
        initUI();

        // Set up the bluetooth event listening loop to run on a separate thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                esp32BluetoothEventListener.connectToDeviceAndOpenStreams();

                esp32BluetoothEventListener.sendHandshake();

                //Log.d(TAG, "gonna read all the verbose now");
                try {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            statusTextView.setText("Waiting for event trigger request");
                        }
                    });

                    long lastProcessedTriggerEventTimeMillis = System.currentTimeMillis();
                    while (true) {
                        // Bluetooth event listening
                        esp32BluetoothEventListener.waitForInput();
                        esp32BluetoothEventListener.readEventTriggerRequest();

                        if (System.currentTimeMillis() - lastProcessedTriggerEventTimeMillis >= RECORDING_DURATION_MILLISECONDS) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    statusTextView.setText("Received Event Trigger Request... Recording video now");
                                }
                            });

                            // Upon reading a full event trigger request, start a new thread
                            // for video recording
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        // The fact that there was an event means that the camera should start recording
                                        String filePath = frontFacingCameraRecorder.recordVideoForDuration(RECORDING_DURATION_MILLISECONDS / 1000);
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                statusTextView.setText("Recorded video. Uploading it to database now");
                                            }
                                        });

                                        File videoFile = new File(filePath);
                                        RequestBody reqFile = RequestBody.create(MediaType.parse("video/mp4"), videoFile);
                                        MultipartBody.Part body = MultipartBody.Part.createFormData("upload", videoFile.getName(), reqFile);
                                        RequestBody name = RequestBody.create(MediaType.parse("text/plain"), "upload");
                                        Call<ResponseBody> req = apiService.postVideo(body, name);
                                        req.enqueue(new Callback<ResponseBody>() {
                                            @Override
                                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                                if (response.code() == 200) {
                                                    Log.d(TAG, "Uploaded Successfully");

                                                }
                                            }

                                            @Override
                                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                                Log.d(TAG, "Uploaded Failed: " + t.getMessage());
                                            }
                                        });
                                    } catch (Exception e) {
                                        Log.d(TAG, e.getMessage());
                                    }
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            statusTextView.setText("Sent video. Waiting for next event trigger request");
                                        }
                                    });

                                }
                            }).start();

                            lastProcessedTriggerEventTimeMillis = System.currentTimeMillis();
                        }
                    }
                } catch (IOException ioe) {
                    //Log.e(TAG, "Couldn't read from the input stream.");
                }
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        frontFacingCameraRecorder.destruct();
    }
}
