package com.example.gl552vx.gamemimic.Model;

import android.Manifest;
import android.Manifest.permission;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AliasActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.example.gl552vx.gamemimic.View.MainActivity;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.Face;

import org.w3c.dom.Text;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CameraModel {


    private Activity activity;
    private CameraDevice mDevice;
    private CameraCaptureSession mCaptureSess;
    private CaptureRequest.Builder captureRequestBuilder;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private String cameraId;
    private Size imageDimension;
    private ImageReader imageReader;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    // Add your Face endpoint to your environment variables.
    //southeastasia.api.cognitive.microsoft.com
    //pascalfaceapisandbox.cognitiveservices.azure.com
    private final String apiEndpoint = "https://westcentralus.api.cognitive.microsoft.com/face/v1.0/";
    // Add your Face subscription key to your environment variables.
    private final String subscriptionKey = "48c19bfc4ddb4e29abb2b565fdd3cc6f";
    private Face[] emotionRes;
    private ProgressDialog detecProgressDialog;
    private final FaceServiceClient faceServiceClient = new FaceServiceRestClient(apiEndpoint,subscriptionKey);
    private File file;

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mDevice = cameraDevice;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            cameraDevice.close();
        }
    };
    private TextureView textureView;
    //init ends here

    //Constructor class
    public CameraModel(Activity activity, TextureView textureView){
        this.activity = activity;
        this.textureView = textureView;
        this.detecProgressDialog = new ProgressDialog(activity);
    }

    public CameraDevice getmDevice() {
        return mDevice;
    }

    public Size getImageDimension() {
        return imageDimension;
    }

    public static int getRequestCameraPermission() {
        return REQUEST_CAMERA_PERMISSION;
    }

    public void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    public void stopBackgroundThread(){
        mBackgroundThread.quitSafely();

        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void takePicture(){
        if(mDevice == null){
            return;
        }
        else{
            CameraManager manager = (CameraManager) activity.getSystemService(activity.CAMERA_SERVICE);

            try {
                CameraCharacteristics characteristics= manager.getCameraCharacteristics(mDevice.getId());
                Size[] jpegSize = null;
                if(characteristics != null){
                    jpegSize = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
                }
                int width = 480;
                int height = 640;

                if(jpegSize != null && 0 < jpegSize.length){
                    width = jpegSize[0].getWidth();
                    height = jpegSize[0].getHeight();
                }

                final ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
                //ignoring surface output
                List<Surface> outputSurfaces = new ArrayList<Surface>(2);
                outputSurfaces.add(reader.getSurface());
                outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
                final CaptureRequest.Builder captureBuilder = mDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                captureBuilder.addTarget(reader.getSurface());
                captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
//                captureBuilder.set(
//                        CaptureRequest.JPEG_ORIENTATION,
//                       270);
               // activity.getWindowManager().getDefaultDisplay().getRotation();
               // sensorOrientation =  characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                file = new File(Environment.getExternalStorageDirectory()+"/pic.jpg");

                ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReader imageReader) {
                        Image image = null;
                        try {
                            image = reader.acquireLatestImage();
                            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                            byte[] bytes = new byte[buffer.capacity()];
                            Log.d("byte", bytes.length+"");
                            buffer.get(bytes);
                            save(bytes);
                            File root = Environment.getExternalStorageDirectory();
                            Bitmap bmap = BitmapFactory.decodeFile(root+"/pic.jpg");
                            //GameManager gm = new GameManager();
                            //gm.detectEmotion(bytes);
                            detectEmotion(bmap);
//                            while(emotionRes == null){
//
//                                Log.d("ABC","null");
//                            }

                            Log.d("RESULT",getResult("happiness")+" asfasfas");



                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            if (image != null) {
                                image.close();
                            }
                        }
                    }

                    private void save(byte[] bytes) throws IOException {
                        OutputStream output = null;
                        try {
                            output = new FileOutputStream(file);
                            output.write(bytes);
                        } finally {
                            if (null != output) {
                                output.close();
                            }
                        }
                    }
                };

                reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
                final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                        super.onCaptureCompleted(session, request, result);
                        Toast.makeText(activity, "Saved:" + file, Toast.LENGTH_SHORT).show();
                        createCameraPreview();
                    }
                };
                mDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(CameraCaptureSession session) {
                        try {
                            session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }
                    @Override
                    public void onConfigureFailed(CameraCaptureSession session) {
                    }
                }, mBackgroundHandler);

            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Method to publish camera on the texture view
     */
    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = mDevice.createCaptureRequest(mDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            mDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == mDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    mCaptureSess = cameraCaptureSession;
                    updatePreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(activity, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void updatePreview() {
        if(null == mDevice) {
            Log.e("d", "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            mCaptureSess.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void openCamera() {
        CameraManager manager = (CameraManager) this.activity.getSystemService(this.activity.CAMERA_SERVICE);
        Log.e("d", "is camera open");
        try {
            cameraId = manager.getCameraIdList()[1];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this.activity, new String[]{permission.CAMERA, permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e("d", "openCamera X");
    }

    public void closeCamera() {
        if (null != mDevice) {
            mDevice.close();
            mDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }

    private int getJpegOrientation(CameraCharacteristics c, int deviceOrientation) {
        if (deviceOrientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN) return 0;
        int sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION);

        // Round device orientation to a multiple of 90
        deviceOrientation = (deviceOrientation + 45) / 90 * 90;

        // Reverse device orientation for front-facing cameras
        boolean facingFront = c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT;
        if (facingFront) deviceOrientation = -deviceOrientation;

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        int jpegOrientation = (sensorOrientation + deviceOrientation + 360) % 360;

        return jpegOrientation;
    }


    public void detectEmotion(final Bitmap bmap) {
        //byte encodedArr = Base64.encode;
        //Bitmap imageBitmap = BitmapFactory.decodeByteArray(arr,0,arr.length);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        ByteArrayInputStream inputStreams =
                new ByteArrayInputStream(outputStream.toByteArray());

        AsyncTask<InputStream,String, Face[]> faceThread = new AsyncTask<InputStream, String, Face[]>() {
            String exception ="Something went wrong!";

            @Override
            protected Face[] doInBackground(InputStream... inputStream) {
                try {
                    publishProgress("Detecting...");
                    Log.d("byte", inputStream[0].read()+"");
                    Face[] result = faceServiceClient.detect(
                            inputStream[0],
                            true,         // returnFaceId
                            false,        // returnFaceLandmarks
                            new FaceServiceClient.FaceAttributeType[]{FaceServiceRestClient.FaceAttributeType.Emotion}
                            // returnFaceAttributes:
                                /* new FaceServiceClient.FaceAttributeType[] {
                                    FaceServiceClient.FaceAttributeType.Age,
                                    FaceServiceClient.FaceAttributeType.Gender }
                                */

                    );
                    Log.d("d","Detecting..");
                    if (result == null){
                        publishProgress(
                                "Detection Finished. Nothing detected");
                        return null;
                    }
                    publishProgress(String.format(
                            "Detection Finished. %d face(s) detected",
                            result.length));
                    return result;
                } catch (Exception e) {
                    exception = String.format(
                            "Detection failed: %s", e.getMessage());
                    //Toast.makeText(activity, exception, Toast.LENGTH_LONG);
                    //throw new RuntimeException(e);
                    return null;
                }
            }

//            @Override
//            protected void onPreExecute() {
//                //TODO: show progress dialog
//                detecProgressDialog.show();
//            }
//            @Override
//            protected void onProgressUpdate(String... progress) {
//                //TODO: update progress
//                detecProgressDialog.setMessage(progress[0]);
//            }

            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            protected void onPostExecute(Face[] result) {
                //TODO: update face frames
                detecProgressDialog.dismiss();
                if(!exception.equals("")){
                    showError(exception);
                }
                if (result == null) return;
                emotionRes = result;
                closeCamera();
                openCamera();
                Toast.makeText(activity,getResult("happiness")+" asfasfas", Toast.LENGTH_LONG ).show();
               //createCameraPreview();
            }


        };
//           faceServiceClient.detect(
//                    inputStream,
//                    false,         // returnFaceId
//                    false,        // returnFaceLandmarks
//                    new FaceServiceClient.FaceAttributeType[]{FaceServiceRestClient.FaceAttributeType.Emotion}          // returnFaceAttributes:
//                                    /* new FaceServiceClient.FaceAttributeType[] {
//                                        FaceServiceClient.FaceAttributeType.Age,
//                                        FaceServiceClient.FaceAttributeType.Gender }
//                                    */
//            );
//            if (result == null) return;
//
//            Log.d("FaceAtr","abcdefg "+result[0].faceAttributes.emotion.anger+" "+result[0].faceAttributes.emotion.surprise + " " +
//                    result[0].faceAttributes.emotion.happiness+" "+result[0].faceAttributes.emotion.fear +" "
//                    +result[0].faceAttributes.emotion.disgust);
//
//            emotionRes = result;
//        } catch (ClientException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        faceThread.execute(inputStreams);

    }

    private void showError(String message) {
        new AlertDialog.Builder(activity)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }})
                .create().show();
    }

    public double getResult(String entry){
        if(this.emotionRes == null){
            return -1;
        }
        if(entry.equalsIgnoreCase("happiness")){
            return this.emotionRes[0].faceAttributes.emotion.happiness;
        } else if(entry.equalsIgnoreCase("anger")){
            return this.emotionRes[0].faceAttributes.emotion.anger;
        }else if(entry.equalsIgnoreCase("surprise")){
            return this.emotionRes[0].faceAttributes.emotion.surprise;
        } else if(entry.equalsIgnoreCase("fear")){
            return this.emotionRes[0].faceAttributes.emotion.fear;
        } else if(entry.equalsIgnoreCase("disgust")){
            return this.emotionRes[0].faceAttributes.emotion.disgust;
        }
        return -1;
    }

}
