package com.example.surveillancenotificationapp;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "tagg";

    SharedPreferences preferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final LinearLayout listOfRecordingScrollViewLinearLayout = findViewById(R.id.listOfRecordingsLinLayout);

        preferences = getApplicationContext().getSharedPreferences("pref", 0);
        editor = preferences.edit();
        updateSavedID(-1);

        final Runnable populateRecordingListRunnable = new Runnable() {
            @Override
            public void run() {
                URL headUrl;
                HttpURLConnection headURLConnection;
                try {
                    headUrl = new URL("http://d778fce8.ngrok.io/getRecordings/");
                } catch (MalformedURLException murle) {
                    Log.e(TAG, murle.getMessage());
                    return;
                }

                try {
                    headURLConnection = (HttpURLConnection) headUrl.openConnection();
                } catch (IOException ioe) {
                    Log.e(TAG, ioe.getMessage());
                    return;
                }

                final List<VideoLink> videoLinkList;

                try {
                    final InputStream in = new BufferedInputStream(headURLConnection.getInputStream());

                    videoLinkList = readJsonStream(in);

                    in.close();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listOfRecordingScrollViewLinearLayout.removeAllViews();
                            for (int i = 0; i < videoLinkList.size(); i++) {
                                final TextView videoLinkTextView = new TextView(getApplicationContext());
                                videoLinkTextView.setText(videoLinkList.get(i).getVideoName());
                                videoLinkTextView.setTextSize(30);

                                videoLinkTextView.setClickable(true);
                                videoLinkTextView.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        for (int i = 0; i < videoLinkList.size(); i++) {
                                            if (videoLinkList.get(i).getVideoName().equals(videoLinkTextView.getText().toString())) {
                                                final VideoLink selectedVideo = videoLinkList.get(i);
                                                // Start a new thread to download the object and get a video player to play it
                                                new Thread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Intent i = new Intent(Intent.ACTION_VIEW);
                                                        i.setData(Uri.parse(selectedVideo.getVideoURLPath()));
                                                        startActivity(i);
                                                    }
                                                }).start();
                                            }
                                        }
                                    }
                                });

                                listOfRecordingScrollViewLinearLayout.addView(videoLinkTextView);
                            }
                        }
                    });
                } catch (IOException ioe) {
                    Log.e(TAG, ioe.getMessage());
                } finally {
                    headURLConnection.disconnect();
                }
            }
        };

        Runnable checkIfRecordingListUpToDateRunnable = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    URL headUrl;
                    HttpURLConnection headURLConnection;
                    try {
                        headUrl = new URL("http://d778fce8.ngrok.io/getLatestRecord/");
                    } catch (MalformedURLException murle) {
                        Log.e(TAG, murle.getMessage());
                        return;
                    }

                    try {
                        headURLConnection = (HttpURLConnection) headUrl.openConnection();
                    } catch (IOException ioe) {
                        Log.e(TAG, ioe.getMessage());
                        return;
                    }

                    try {
                        final InputStream in = new BufferedInputStream(headURLConnection.getInputStream());
                        try {
                            List<VideoLink> latestLinkList = readJsonStream(in);
                            if (!latestLinkList.isEmpty()) {
                                VideoLink latestLink = latestLinkList.get(0);
                                if (latestLink.getID() > getSavedID()) {
                                    //new Thread(populateRecordingListRunnable).start();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
//                                            sendNotification();
                                        }
                                    });
                                    populateRecordingListRunnable.run();
                                    updateSavedID(latestLink.getID());
                                }
                            }
                            in.close();
                        } catch (IOException ioe) {
                            Log.d(TAG, ioe.getMessage());
                        }

                    } catch (IOException ioe) {
                        Log.d(TAG, ioe.getMessage());
                    }
                }
            }
        };

        new Thread(checkIfRecordingListUpToDateRunnable).start();
        //new Thread(populateRecordingListRunnable).start();
    }
    public void updateSavedID(long newId) {
        editor.clear();
        editor.putInt("key", (int) newId);
        editor.commit();

    }
    public long getSavedID() {
        return preferences.getInt("key", -1);
    }
    // JSON STUFF:
    //************************************************************************************
    public List<VideoLink> readJsonStream(InputStream in) throws IOException {
        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        try {
            return readVideoLinksArray(reader);
        } finally {
            reader.close();
        }
    }

    public List<VideoLink> readVideoLinksArray(JsonReader reader) throws IOException {
        List<VideoLink> messages = new ArrayList<VideoLink>();

        reader.beginArray();
        while (reader.hasNext()) {
            messages.add(readVideoLink(reader));
        }
        reader.endArray();
        return messages;
    }

    public VideoLink readVideoLink(JsonReader reader) throws IOException {
        long id = -1;
        String videoName = null;
        String videoURLPath = null;

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("id")) {
                id = reader.nextLong();
            } else if (name.equals("name")) {
                videoName = reader.nextString();
            } else if (name.equals("filepath")) {
                videoURLPath = "http://" + reader.nextString();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return new VideoLink(id, videoName, videoURLPath);
    }
    //************************************************************************************

    // SANDS notification. we dont need to return anything
    private void sendNotification() {
        // build the notification rs
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("YEYE NOTIFICATION RS")
                .setContentText("YEYE ANDROID RS")
                .setAutoCancel(true);

        // we want to show the notification rs
        Intent notificationIntent = new Intent(this, com.example.surveillancenotificationapp.MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);

        // add the notificition
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(0, builder.build());
    }
}
