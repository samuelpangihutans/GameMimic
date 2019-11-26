package com.example.gl552vx.gamemimic.Model;

import android.Manifest;
import android.Manifest.permission;
import android.app.Activity;
import android.app.AliasActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
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
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    private final String apiEndpoint = "https://westcentralus.api.cognitive.microsoft.com/face/v1.0/";
//            "https://pascalfaceapisandbox.cognitiveservices.azure.com/face/v1.0/";
    // Add your Face subscription key to your environment variables.
    private final String subscriptionKey = "d8587d3195104833b9d48008f8770a52";
    private Face[] emotionRes;

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
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    /**
     * Retrieves the JPEG orientation from the specified screen rotation.
     *
     * @param rotation The screen rotation.
     * @return The JPEG orientation (one of 0, 90, 270, and 360)
     */
    private int getOrientation(int rotation,int mSensorOrientation) {
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
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
                int width = 640;
                int height = 480;

                if(jpegSize != null && jpegSize.length >0){
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

                int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();

                 int sensorOrientation =  characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
               // Log.d("DETEK","rotasinya = "+rotation);

                //captureBuilder.set(CaptureRequest.JPEG_ORIENTATION,ORIENTATIONS.get(rotation));
               captureBuilder.set(CaptureRequest.JPEG_ORIENTATION,getJpegOrientation(characteristics,activity.getWindowManager().getDefaultDisplay().getRotation())+10);
                //captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(270,sensorOrientation));
                //captureBuilder.set(CaptureRequest.JPEG_ORIENTATION,ORIENTATIONS.indexOfValue(0));
                file = new File(Environment.getExternalStorageDirectory()+"/pic.jpg");

                ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReader imageReader) {
                        Image image = null;
                        try {
                            image = reader.acquireLatestImage();
                            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                            byte[] bytes = new byte[buffer.capacity()];
                            buffer.get(bytes);
                            save(bytes);
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
                        Log.d("DETEK","kedetek, di ambil buaat jadi bitmap");
                        Uri uri=Uri.fromFile(file);
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), uri);
                            detectEmotion(bitmap);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

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


    public void detectEmotion(final Bitmap imageBitmap) {
        Log.d("DETEK","Gambar lagi di terjemahin ke byte gens");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(outputStream.toByteArray());

        AsyncTask<InputStream,String, Face[]> faceThread = new AsyncTask<InputStream, String, Face[]>() {
            String exception ="";
            @Override
            protected Face[] doInBackground(InputStream... inputStreams) {
                try {
                    publishProgress("Detecting...");
                    Face[] result = faceServiceClient.detect(
                            inputStreams[0],
                            true,         // returnFaceId
                            false,        // returnFaceLandmarks
                            new FaceServiceClient.FaceAttributeType[]{FaceServiceRestClient.FaceAttributeType.Emotion}  // returnFaceAttributes:
                                    /* new FaceServiceClient.FaceAttributeType[] {
                                        FaceServiceClient.FaceAttributeType.Age,
                                        FaceServiceClient.FaceAttributeType.Gender }
                                    */
                    );
                    Log.d("DETEK","kedetek WOIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII");
                    if (result == null){
                        Log.d("UNDETECTED","2.2");

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
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Face[] faces) {
                Log.d("DETEK","Masuk ke OnPost execute");

                if(faces==null){
                    Log.d("DETEK", "status = error");
                    return;
                };

                for(Face res : faces){
                    String status=getEmo(res);
                    Log.d("DETEK", "status="+status);
                }

            }

        };
        faceThread.execute(inputStream);
    }

    private String getEmo(Face res){
        Log.d("DETEK","muka di periksa gens");
        List<Double> list=new ArrayList<>();

        list.add(res.faceAttributes.emotion.anger);
        list.add(res.faceAttributes.emotion.happiness);
        list.add(res.faceAttributes.emotion.contempt);
        list.add(res.faceAttributes.emotion.disgust);
        list.add(res.faceAttributes.emotion.fear);
        list.add(res.faceAttributes.emotion.neutral);
        list.add(res.faceAttributes.emotion.sadness);
        list.add(res.faceAttributes.emotion.surprise);

        Collections.sort(list);

        double maxNum=list.get(list.size()-1);
        if(maxNum==res.faceAttributes.emotion.anger){
            return "Anger";
        }
        else if(maxNum==res.faceAttributes.emotion.happiness){
            return "Happy";
        }else if(maxNum==res.faceAttributes.emotion.contempt){
            return "Contemp";
        }else if(maxNum==res.faceAttributes.emotion.disgust){
            return "Disgust";
        }else if(maxNum==res.faceAttributes.emotion.fear){
            return "Fear";
        }else if(maxNum==res.faceAttributes.emotion.neutral){
            return "Neutral";
        }else if(maxNum==res.faceAttributes.emotion.sadness){
            return "Sadness";
        }else if(maxNum==res.faceAttributes.emotion.surprise){
            return "Surprise";
        }
        else{
            return "Neutral";
        }

    }

}
