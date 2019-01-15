package com.dakshin.button;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.IOException;
import java.security.Key;

import static android.content.ContentValues.TAG;

public class ButtonService extends Service implements View.OnTouchListener
{
    private WindowManager windowManager;
    private ImageView button;
    private WindowManager.LayoutParams params;
    Process process;
    public ButtonService() {
    }
    final Handler handler = new Handler();
    Runnable longPressed = new Runnable() {
        public void run() {
            try {
                //disable the button before taking screenshot
                params.alpha=0;
                windowManager.updateViewLayout(button,params);
                process.getOutputStream().write(("input keyevent "+KeyEvent.KEYCODE_POWER+'\n').getBytes());
                process.getOutputStream().flush();
                Thread.sleep(1000);
                params.alpha=1.0f;
                windowManager.updateViewLayout(button,params);
            } catch (IOException | InterruptedException e) {
                Log.e("tag","error");
            }
        }
    };
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
//        throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            process=Runtime.getRuntime().exec("su");
        } catch (IOException e) {
            Log.e("tag","error executing su");
        }
        windowManager=(WindowManager)getSystemService(WINDOW_SERVICE);
        button=new ImageView(this);
        button.setImageResource(R.drawable.button);
        params= new WindowManager.LayoutParams(
                100,100,
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL| WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT
                );
//        params.gravity= Gravity.CENTER_VERTICAL|Gravity.END;
        windowManager.addView(button,params);
        button.setOnTouchListener(this);


    }
    @Override
    public void onDestroy() {
        if(button!=null)
            windowManager.removeView(button);
        super.onDestroy();
    }

    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;
    private long downTapTime;
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        long curTime=System.currentTimeMillis();
        Log.d("tag", "onTouch: "+event.toString());
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //remember the initial position.
                initialX = params.x;
                initialY = params.y;

                //get the touch location
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();

                downTapTime= System.currentTimeMillis();

                //long click detector
                handler.postDelayed(longPressed, ViewConfiguration.getLongPressTimeout());
                return true;
            case MotionEvent.ACTION_UP:
                //delete the long click listener if it hasn't been called yet
                handler.removeCallbacks(longPressed);
                
                //As we implemented on touch listener with ACTION_MOVE,
                //we have to check if the previous action was ACTION_DOWN
                //to identify if the user clicked the view or not.

                    //down followed by up = a click
                    long timeDiff=curTime-downTapTime;
                    if(timeDiff<=150)
                        try {
                            process.getOutputStream().write(("su -c input keyevent "+KeyEvent.KEYCODE_BACK+"\n").getBytes());
                            process.getOutputStream().flush();
                        } catch (IOException e) {
                            Log.e("tag","Shell command failed");
                        }
                    return true;
            case MotionEvent.ACTION_MOVE:
                if(Math.abs(event.getRawY()-initialTouchY)>100&&Math.abs(event.getRawX()-initialTouchX)>100)
                    handler.removeCallbacks(longPressed);
                //Calculate the X and Y coordinates of the view.
                params.x = initialX + (int) (event.getRawX() - initialTouchX);
                params.y = initialY + (int) (event.getRawY() - initialTouchY);

                //Update the layout with new X & Y coordinate
                windowManager.updateViewLayout(button, params);
                return true;
        }
        return false;
    }

}
