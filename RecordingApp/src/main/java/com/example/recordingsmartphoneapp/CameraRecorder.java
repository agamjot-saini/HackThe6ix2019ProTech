package com.example.recordingsmartphoneapp;

import android.content.Context;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CameraRecorder {
    private Camera camera;
    private MediaRecorder mediaRecorder;
    private CameraPreview cameraPreview;

    private File recordingStorageDirectory;

    public void initCamera(int cameraID) {
        try {
            camera = Camera.open(cameraID);
        } catch (Exception e) {
            //Log.d(TAG, "Unable to access camera with ID: " + cameraID);
            //Log.d(TAG, e.getMessage());
        }
    }

    public void initCamera() {
        try {
            camera = Camera.open();
        } catch (Exception e) {
            //Log.d(TAG, "Unable to access front-facing camera.");
            //Log.d(TAG, e.getMessage());
        }
        camera.setDisplayOrientation(90);
    }

    public void initCameraPreview(Context context) {
        cameraPreview = new CameraPreview(context, camera);
    }

    public CameraPreview getCameraPreview() {
        return cameraPreview;
    }

    public void initializeRecordingStorageDirectory(Context context, String directoryName) {
        recordingStorageDirectory = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), directoryName);

        if (!recordingStorageDirectory.exists()) {
            if (!recordingStorageDirectory.mkdirs()) {
                //Log.d(TAG, "Couldn't create directory");
            }
        }
    }

    public String getStorageDirectoryPath() {
        return recordingStorageDirectory.getAbsolutePath();
    }

    public String recordVideoForDuration(int durationSeconds) {
        camera.startPreview();

        mediaRecorder = new MediaRecorder();

        camera.unlock();
        mediaRecorder.setCamera(camera);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

//        File mediaStorageDir = new File(getBaseContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "SurveillanceCamera");
//        if (!recordingStorageDirectory.exists()) {
//            if (!recordingStorageDirectory.mkdirs()) {
//                return;
//            }
//        }

        String fileName = recordingStorageDirectory.getPath()
                + File.separator
                + "VID_"
                + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())
                + ".mp4";

        mediaRecorder.setOutputFile(fileName);
        //Log.d(TAG, "File created.");

        mediaRecorder.setPreviewDisplay(cameraPreview.getHolder().getSurface());
        //Log.d(TAG, "Previous preview assigned to MediaRecorder");

        try {
            mediaRecorder.prepare();
            //Log.d(TAG, "MediaRecorder prepared!");
        } catch (IllegalStateException ise) {
            //Log.d(TAG, "Error in preparing MediaRecorder (IllegalStateException): " + ise.getMessage());
            mediaRecorder.release();
            return "";
        } catch (IOException ioe) {
            //Log.d(TAG, "Error in preparing MediaRecorder (IOException): " + ioe.getMessage());
            mediaRecorder.release();
            return "";
        }

        //Log.d(TAG, "Starting to record video now.");
        mediaRecorder.start();

        try {
            Thread.sleep(durationSeconds * 1000);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }

        mediaRecorder.stop();
        //Log.d(TAG, "Stopped recording video.");
        return fileName;
    }

    public void destruct() {
        mediaRecorder.release();
        camera.lock();
        //Log.d(TAG, "Released media recorder and locked camera");

        camera.stopPreview();
        camera.release();
        //Log.d(TAG, "Stopped camera preview, and released camera instance");
    }
}
