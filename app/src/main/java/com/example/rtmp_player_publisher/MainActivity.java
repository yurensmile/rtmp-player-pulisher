package com.example.rtmp_player_publisher;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.pedro.encoder.input.video.CameraOpenException;
import com.pedro.rtplibrary.rtmp.RtmpCamera1;

import net.ossrs.rtmp.ConnectCheckerRtmp;

import cn.nodemedia.NodePlayer;
import cn.nodemedia.NodePlayerDelegate;
import cn.nodemedia.NodePlayerView;

public class MainActivity extends AppCompatActivity
        implements ConnectCheckerRtmp, View.OnClickListener, SurfaceHolder.Callback, NodePlayerDelegate {
    private final String[] PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private RtmpCamera1 rtmpCamera1;
    private Button button;
    private Button switchCamera;
    private Button pullBut;
    private EditText urlpush;
    private EditText urlpull;
    private NodePlayer np;
    SurfaceView surfaceViewTop;
    private String[] options = new String[]{":fullscreen", ":network-caching=350"};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, 1);
        }
        init();

        rtmpCamera1 = new RtmpCamera1(surfaceViewTop, this);
        rtmpCamera1.setReTries(10);
        surfaceViewTop.getHolder().addCallback(this);
//        拉流
        np = new NodePlayer(this);
        NodePlayerView npv = findViewById(R.id.surfaceView_bottom);
        npv.setRenderType(NodePlayerView.RenderType.SURFACEVIEW);
        npv.setUIViewContentMode(NodePlayerView.UIViewContentMode.ScaleAspectFill);
        np.setPlayerView(npv);
        np.setNodePlayerDelegate(this);
        np.setHWEnable(true);


    }
    private void init(){
        surfaceViewTop = findViewById(R.id.surfaceView_top);
        button =  findViewById(R.id.b_start_stop);
        pullBut = findViewById(R.id.start_pull);
        switchCamera = findViewById(R.id.switch_camera);
        pullBut = findViewById(R.id.start_pull);
        urlpull = findViewById(R.id.url_pull);
        urlpush = findViewById(R.id.url_push);
        switchCamera.setOnClickListener(this);
        button.setOnClickListener(this);
        pullBut.setOnClickListener(this);
    }
    private boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

//拉流事件回调
    /**
     * 事件回调
     * @param nodePlayer 对象
     * @param event 事件状态码
     * @param msg   事件描述
     */
    @Override
    public void onEventCallback(NodePlayer nodePlayer, int event, String msg) {
        Log.i("NodeMedia.NodePlayer","onEventCallback:"+event+" msg:"+msg);

        switch (event) {
            case 1000:
                // 正在连接视频
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Connecting", Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            case 1001:
                // 视频连接成功
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Connection success", Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            case 1002:
                // 视频连接失败 流地址不存在，或者本地网络无法和服务端通信，回调这里。5秒后重连， 可停止
//                nodePlayer.stopPlay();
                break;
            case 1003:
                // 视频开始重连,自动重连总开关
//                nodePlayer.stopPlay();
                break;
            case 1004:
                // 视频播放结束
                break;
            case 1005:
                // 网络异常,播放中断,播放中途网络异常，回调这里。1秒后重连，如不需要，可停止
//                nodePlayer.stopPlay();
                break;
            case 1006:
                //RTMP连接播放超时
                break;
            case 1100:
                // 播放缓冲区为空
//				System.out.println("NetStream.Buffer.Empty");
                break;
            case 1101:
                // 播放缓冲区正在缓冲数据,但还没达到设定的bufferTime时长
//				System.out.println("NetStream.Buffer.Buffering");
                break;
            case 1102:
                // 播放缓冲区达到bufferTime时长,开始播放.
                // 如果视频关键帧间隔比bufferTime长,并且服务端没有在缓冲区间内返回视频关键帧,会先开始播放音频.直到视频关键帧到来开始显示画面.
//				System.out.println("NetStream.Buffer.Full");
                break;
            case 1103:
//				System.out.println("Stream EOF");
                // 客户端明确收到服务端发送来的 StreamEOF 和 NetStream.Play.UnpublishNotify时回调这里
                // 注意:不是所有云cdn会发送该指令,使用前请先测试
                // 收到本事件，说明：此流的发布者明确停止了发布，或者因其网络异常，被服务端明确关闭了流
                // 本sdk仍然会继续在1秒后重连，如不需要，可停止
//                nodePlayer.stopPlay();
                break;
            case 1104:
                //解码后得到的视频高宽值 格式为:{width}x{height}
                break;
            default:
                break;
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        /**
         * 停止播放
         */
        np.stop();

        /**
         * 释放资源
         */
        np.release();
    }
    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (rtmpCamera1.isStreaming()) {
            rtmpCamera1.stopStream();
            button.setText(getResources().getString(R.string.start_button));
        }
        rtmpCamera1.stopPreview();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.b_start_stop:
                if (!rtmpCamera1.isStreaming()) {
                    if (rtmpCamera1.isRecording()
                            || rtmpCamera1.prepareVideo(640, 480, 30, 1200 * 1024, false, 0) &&  rtmpCamera1.prepareAudio()) {
                        button.setText(R.string.stop_button);
                        rtmpCamera1.startStream(urlpush.getText().toString());
                    } else {
                        Toast.makeText(this, "Error preparing stream, This device cant do it",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    button.setText(R.string.start_button);
                    rtmpCamera1.stopStream();
                }
                break;
            case R.id.switch_camera:
                try {
                    rtmpCamera1.switchCamera();
                } catch (CameraOpenException e) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.start_pull:
                if(!np.isPlaying()){
                    np.setInputUrl(urlpull.getText().toString());
                    np.start();
                    pullBut.setText(getString(R.string.stop_player));
                }else {
                    np.stop();
                    pullBut.setText(getString(R.string.start_player));
                }
                break;
            default:
                break;
        }
    }
    @Override
    public void onConnectionSuccessRtmp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Connection success", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onConnectionFailedRtmp(@NonNull final String reason) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (rtmpCamera1.reTry(5000, reason)) {
                    Toast.makeText(MainActivity.this, "Retry", Toast.LENGTH_SHORT)
                            .show();
                } else {
                    Toast.makeText(MainActivity.this, "Connection failed. " + reason, Toast.LENGTH_SHORT)
                            .show();
                    rtmpCamera1.stopStream();
                    button.setText(R.string.start_button);
                }
            }
        });
    }

    @Override
    public void onNewBitrateRtmp(long bitrate) {

    }

    @Override
    public void onDisconnectRtmp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onAuthErrorRtmp() {

    }

    @Override
    public void onAuthSuccessRtmp() {

    }
}
