package com.example.gl552vx.gamemimic.Model;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.TextureView;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.rest.ClientException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class GameManager {
    //private FaceAPI_Manager api;

    // Add your Face endpoint to your environment variables.
    private final String apiEndpoint = "https://pascalfaceapisandbox.cognitiveservices.azure.com/face/v1.0/detect";
    // Add your Face subscription key to your environment variables.
    private final String subscriptionKey = "85d799141b4746d6827f4ffd52db6375";

    private final FaceServiceClient faceServiceClient = new FaceServiceRestClient(apiEndpoint,subscriptionKey);
//            new FaceServiceRestClient(apiEndpoint, subscriptionKey);


    public Face [] emotionRes;

    public void detectEmotion(byte[]arr) {

        final ByteArrayInputStream inputStream =
                new ByteArrayInputStream(arr);

           AsyncTask<InputStream,String,Face[]>  faceThread = new AsyncTask<InputStream, String, Face[]>() {
               String exception ="Something went wrong!";

               @Override
               protected Face[] doInBackground(InputStream... inputStreams) {
                   try {
                       publishProgress("Detecting...");
                       Face[] result = faceServiceClient.detect(
                               inputStreams[0],
                               true,         // returnFaceId
                               false,        // returnFaceLandmarks
                               new FaceServiceClient.FaceAttributeType[]{FaceServiceRestClient.FaceAttributeType.Emotion}
                               // returnFaceAttributes:
                                /* new FaceServiceClient.FaceAttributeType[] {
                                    FaceServiceClient.FaceAttributeType.Age,
                                    FaceServiceClient.FaceAttributeType.Gender }
                                */

                       );
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
                       return null;
                   }
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
        faceThread.execute();

    }

    public void detectF(){
       // FaceAPI_Manager manager = new FaceAPI_Manager();
       // float[] res = manager.getResult(manager.requestWithOctetStream(Environment.getExternalStorageDirectory().getAbsolutePath()+"/pic.jpg"));
        while (true){
        //    Log.d("TEST",res[0]+"");
        }
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
