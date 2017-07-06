package com.example.pengguanghong.mycamera;

import android.hardware.Camera;

/**
 * 相机管理类
 *
 * Created by pengguanghong on 2017/07/06.
 */

public class CameraController {

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }



}
