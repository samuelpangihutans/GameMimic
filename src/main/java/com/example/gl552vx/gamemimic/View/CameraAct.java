package com.example.gl552vx.gamemimic.View;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import android.widget.Chronometer;

import android.widget.TextView;
import android.widget.Toast;

import com.example.gl552vx.gamemimic.Model.CameraModel;
import com.example.gl552vx.gamemimic.R;

import java.util.Locale;

public class CameraAct extends AppCompatActivity implements View.OnClickListener{

    private static final long START_TIME_IN_MILLIS = 60000;
    private TextureView textureView;
    private TextView tvMimic;
    private TextView tvScore;
    private Button btnCapture;
    private CameraModel camModel;
    private TextView mTextViewCountDown;
    private CountDownTimer mCountDownTimer;
    private long mTimeLeftInMillis = START_TIME_IN_MILLIS;
  
    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            camModel.openCamera();
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        this.mTextViewCountDown = findViewById(R.id.count_timer);
        this.btnCapture = findViewById(R.id.btn_capture);
        this.btnCapture.setOnClickListener(this);
        this.textureView = findViewById(R.id.textureView);
        this.tvMimic=findViewById(R.id.mimic);
        this.tvScore=findViewById(R.id.tv_score);

        startTimer();

        camModel = new CameraModel(this, this.textureView,this.tvMimic,this.tvScore);


    }

    private void startTimer() {
        mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mTimeLeftInMillis = millisUntilFinished;
                updateCountDownText();
            }

            @Override
            public void onFinish() {
                camModel.closeCamera();
            }
        }.start();

    }

    private void updateCountDownText() {
        int minutes = (int) (mTimeLeftInMillis / 1000) / 60;
        int seconds = (int) (mTimeLeftInMillis / 1000) % 60;

        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);

        mTextViewCountDown.setText(timeLeftFormatted);
    }

    private void pauseTimer() {
        mCountDownTimer.cancel();
    }

    private void resetTimer() {
        mTimeLeftInMillis = START_TIME_IN_MILLIS;
        updateCountDownText();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == this.camModel.getRequestCameraPermission()) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onResume() {
        super.onResume();
        Log.e("d", "onResume");
        this.camModel.startBackgroundThread();
        if (textureView.isAvailable()) {
            this.camModel.openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }
    @Override
    protected void onPause() {
        Log.e("d", "onPause");
        this.camModel.closeCamera();
        this.camModel.stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        this.camModel.takePicture();
    }


}
