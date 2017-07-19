package com.example.pengguanghong.mycamera;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.ndk.CrashlyticsNdk;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.fabric.sdk.android.Fabric;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private CameraPreview mPreview;
    private Camera mCamera;
    private int mCameraAngle = 0;
    private boolean mIsBackCamera = true;
    private SurfaceHolder mSurfaceHolder;

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics(), new CrashlyticsNdk());
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mCamera = CameraController.getCameraInstance(mIsBackCamera ? findBackCamera() : findFrontCamera());
        mPreview = new CameraPreview(this, mCamera);
        mSurfaceHolder = mPreview.getHolder();
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        mCamera.setDisplayOrientation(getCameraAngle(this));

        Button capture = (Button) findViewById(R.id.button_capture);
        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.takePicture(null, null, mPicture);
            }
        });
        Button change = (Button) findViewById(R.id.button_change);
        change.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeCamera();
            }
        });

    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        private float mRotateAngle;

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null) {
                Log.d(TAG, "Error creating media file, check storage permissions: ");
                return;
            }
            Log.d(TAG, "File :" + pictureFile.getPath());
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
            String path = pictureFile.getPath();
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            Matrix matrix = new Matrix();
            matrix.reset();
            getRotateAngle();
            matrix.postRotate(mCameraAngle);
            Bitmap rotateBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            File file = new File(path);
            try {
                Log.d(TAG, "写入文件");
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file));
                rotateBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bufferedOutputStream);
                bufferedOutputStream.flush();
                bufferedOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };



    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d(TAG, "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    public static int readPictureDegree(String path) {
        int degree  = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            String orientationAngle = exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION);
            String device = exifInterface.getAttribute(ExifInterface.TAG_MAKE);
            Log.d(TAG, "readPictureDegree:" + orientation +" device:" + device + " degreeString:" + orientationAngle);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            Log.d(TAG, "IOException:" + e);
        }
        return degree;
    }

    /**
     * 获取照相机旋转角度
     */
    public int getCameraAngle(Activity activity) {
        int rotateAngle = 90;
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mIsBackCamera ? findBackCamera() : findFrontCamera(), info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Log.w(TAG, "rotation ==" + rotation);
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            Log.w("ceshi", "front.facing:" + info.orientation);
            rotateAngle = (info.orientation + degrees) % 360;
            rotateAngle = (360 - rotateAngle) % 360; // compensate the mirror
//            rotateAngle -= 180;//强制修复nexus6问题
        } else { // back-facing
            rotateAngle = (info.orientation - degrees + 360) % 360;
            Log.w("ceshi", "back.facing:" + info.orientation + " rotateAngle:" + rotateAngle);
        }
        mCameraAngle =  rotateAngle;
        Log.d(TAG, "angle :" + mCameraAngle);
        return mCameraAngle;
    }

    public void getRotateAngle() {
        if (!mIsBackCamera) {
            mCameraAngle = (360 - mCameraAngle) % 360;
        }
    }


    private void changeCamera() {
        int cameraCount = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Log.d(TAG, "cameraCount:" + cameraCount);
        if (cameraCount > 0) {
            releaseCamera();
            mIsBackCamera = !mIsBackCamera;
            try{
                Log.d(TAG, "1111");
                mCamera = CameraController.getCameraInstance(mIsBackCamera ? findBackCamera() : findFrontCamera());
                Log.d(TAG, "2222");
                mCamera.setPreviewDisplay(mSurfaceHolder);
                mCamera.setDisplayOrientation(getCameraAngle(this));
                Log.d(TAG, "3333");

                mCamera.startPreview();
            } catch (Exception e) {
                Log.d(TAG, "Exception:" + e);
            }
        }
    }

    private int findFrontCamera(){
        int cameraCount = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

        for ( int camIdx = 0; camIdx < cameraCount;camIdx++ ) {
            Camera.getCameraInfo(camIdx, cameraInfo); // get camerainfo
            if ( cameraInfo.facing ==Camera.CameraInfo.CAMERA_FACING_FRONT ) {
                // 代表摄像头的方位，目前有定义值两个分别为CAMERA_FACING_FRONT前置和CAMERA_FACING_BACK后置
                Log.d(TAG, "findFrontCamera id:" + camIdx);
                return camIdx;
            }
        }
        return -1;
    }

    private int findBackCamera(){
        int cameraCount = Camera.getNumberOfCameras(); // get cameras number
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

        for ( int camIdx = 0; camIdx < cameraCount;camIdx++ ) {
            Camera.getCameraInfo(camIdx, cameraInfo); // get camerainfo
            if ( cameraInfo.facing ==Camera.CameraInfo.CAMERA_FACING_BACK ) {
                // 代表摄像头的方位，目前有定义值两个分别为CAMERA_FACING_FRONT前置和CAMERA_FACING_BACK后置
                Log.d(TAG, "findBackCamera id:" + camIdx);
                return camIdx;
            }
        }

        return -1;
    }

    /**
     * 释放mCamera
     */
    private void releaseCamera() {
        if (mCamera != null) {
//            mCamera.setPreviewCallback(null);
            Log.d(TAG, "releasing camera");
            mCamera.stopPreview();// 停掉原来摄像头的预览
            mCamera.release();
            mCamera = null;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
