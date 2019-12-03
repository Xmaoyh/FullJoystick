package com.dji.simulatorDemo;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;


public class Joystick extends SurfaceView implements
        SurfaceHolder.Callback, View.OnTouchListener {

    private Bitmap mJoystick;
    private SurfaceHolder mHolder;
    /**
     * 旋钮对应的显示范围
     */
    private Rect mKnobBounds;

    private JoystickThread mThread;

    /**
     * 旋钮中心的坐标
     */
    private int mKnobX, mKnobY;
    /**
     * 黑色旋钮大小
     */
    private int mKnobSize;
    /**
     * 背景大小——定死130
     */
    private int mBackgroundSize;
    /**
     * 半径——是背景大小的一般
     */
    private float mRadius;

    private JoystickListener mJoystickListener;

    private boolean mAutoCentering = true;
    /**
     * 是否需要显示旋钮
     */
    private boolean mIsShow;
    /**
     * 是否清屏过，清了一次就不在清了
     */
    private boolean mIsClear;
    /**
     * 手指按下时的X,Y坐标
     */
    private float mDownX, mDownY;
    /**
     * 外框背景的画笔
     */
    private Paint mBgPaint;
    /**
     * 画布的大小
     */
    private int mCanvasWidth, mCanvasHeight;

    public Joystick(Context context, AttributeSet attrs) {
        super(context, attrs);
        initGraphics(attrs);
        init();
    }

    private void initGraphics(AttributeSet attrs) {
        Resources res = getContext().getResources();
        mJoystick = BitmapFactory.decodeResource(res, R.mipmap.joystick);
        mBgPaint = new Paint();
        mBgPaint.setAntiAlias(true);
        mBgPaint.setStrokeWidth(3f);
        mBgPaint.setColor(Color.WHITE);
        mBgPaint.setStyle(Paint.Style.STROKE);
    }

    private void initCanvasSize(final Canvas pCanvas) {
        mCanvasWidth = pCanvas.getWidth();
        mCanvasHeight = pCanvas.getHeight();
    }

    private void init() {
        mHolder = getHolder();
        mHolder.addCallback(this);

        mThread = new JoystickThread();

        setZOrderOnTop(true);
        mHolder.setFormat(PixelFormat.TRANSPARENT);
        setOnTouchListener(this);
        setEnabled(true);
        setAutoCentering(true);

        mBackgroundSize = 130;
        mKnobSize = Math.round(mBackgroundSize * 0.8f);
        mKnobBounds = new Rect();
        mRadius = mBackgroundSize * 0.5f;
    }

    public void setAutoCentering(final boolean pAutoCentering) {
        mAutoCentering = pAutoCentering;
    }

    public boolean isAutoCentering() {
        return mAutoCentering;
    }

    public void setJoystickListener(final JoystickListener pJoystickListener) {
        mJoystickListener = pJoystickListener;
    }

    @Override
    public void surfaceChanged(final SurfaceHolder arg0, final int arg1,
                               final int arg2, final int arg3) {

//		mThread.setRunning(false);
    }

    @Override
    public void surfaceCreated(final SurfaceHolder arg0) {
        mThread.start();

    }

    @Override
    public void surfaceDestroyed(final SurfaceHolder arg0) {
        boolean retry = true;
        mThread.setRunning(false);

        while (retry) {
            try {
                // code to kill Thread
                mThread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }

    }

    public void doDraw(final Canvas pCanvas) {
        if (mCanvasWidth == 0) {
            initCanvasSize(pCanvas);
        }
        if (mDownX - (mRadius + mKnobSize * 0.5f) < 0
                || mDownX + (mRadius + mKnobSize * 0.5f) > mCanvasWidth
                || mDownY - (mRadius + mKnobSize * 0.5f) < 0
                || mDownY + (mRadius + mKnobSize * 0.5f) > mCanvasHeight
        ) {
            //超出四周范围，不画
            return;
        }
        mKnobBounds.set(mKnobX, mKnobY, mKnobX + mKnobSize, mKnobY + mKnobSize);
        //画旋钮
        pCanvas.drawBitmap(mJoystick, null, mKnobBounds, null);
        //画旋钮范围的背景
        pCanvas.drawCircle(mDownX, mDownY, mRadius + mKnobSize * 0.5f, mBgPaint);
    }

    @Override
    public boolean onTouch(final View arg0, final MotionEvent pEvent) {
        final float x = pEvent.getX();
        final float y = pEvent.getY();
        switch (pEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = x;
                mDownY = y;
                mKnobX = Math.round(x - mKnobSize * 0.5f);
                mKnobY = Math.round(y - mKnobSize * 0.5f);
                mIsShow = true;
                break;
            case MotionEvent.ACTION_UP:
                mIsShow = false;
                if (isAutoCentering()) {
                    mKnobX = Math.round(mDownX - mKnobSize * 0.5f);
                    mKnobY = Math.round(mDownY - mKnobSize * 0.5f);
                }
                break;
            default:
                // Check if coordinates are in bounds. 检查是不是在范围以内
                // If they aren't move the knob to the closest coordinate inbounds. 如果超出范围，就最多就在范围边上
                if (checkBounds(x, y)) {
                    mKnobX = Math.round(x - mKnobSize * 0.5f);
                    mKnobY = Math.round(y - mKnobSize * 0.5f);
                } else {
                    final double angle = Math.atan2(y - mDownY, x - mDownX);
                    mKnobX = (int) (mDownX + mRadius * Math.cos(angle) - mKnobSize * 0.5f);
                    mKnobY = (int) (mDownY + mRadius * Math.sin(angle) - mKnobSize * 0.5f);
                }
        }

        float px;
        float py;
        if (mKnobX <= mDownX) {
            px = (float) (((-1f + (1f - ((Math.round(mDownX) - (mKnobX + Math.round(mKnobSize / 2))) / (2 * mRadius - mKnobSize))))) / 2.5);
        } else {
            px = (float) (((-1f + 1f + (((mKnobX + Math.round(mKnobSize / 2)) - Math.round(mDownX)) / (2 * mRadius - mKnobSize)))) / 2.5);
        }
        if (mKnobY <= mDownY) {
            py = (float) (((1f - (1f - ((Math.round(mDownY) - (mKnobY + Math.round(mKnobSize / 2))) / (2 * mRadius - mKnobSize))))) / 2.5);
        } else {
            py = (float) (((1f - 1f - (((mKnobY + Math.round(mKnobSize / 2)) - Math.round(mDownY)) / (2 * mRadius - mKnobSize)))) / 2.5);
        }

        Log.d("touch", "px===" + px + "   kx===" + mKnobX + "  dx===" + Math.round(mDownX)
                + "       py===" + py + "   ky===" + mKnobY + "  dy===" + Math.round(mDownY));

        if (mJoystickListener != null) {
            mJoystickListener.onTouch(this,
                    px,
                    py);
        }

        return true;
    }

    private boolean checkBounds(final float pX, final float pY) {
        return Math.pow(mDownX - pX, 2) + Math.pow(mDownY - pY, 2) <= Math
                .pow(mRadius, 2);
    }

    private class JoystickThread extends Thread {

        private boolean running = false;

        @Override
        public synchronized void start() {
            running = true;
            super.start();
        }

        public void setRunning(final boolean pRunning) {
            running = pRunning;
        }

        @Override
        public void run() {
            while (running) {
                // draw everything to the canvas
                Canvas canvas = null;
                try {
                    if (!mIsShow) {
                        if (!mIsClear) {
                            canvas = mHolder.lockCanvas(null);
                            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                            mIsClear = true;
                        }
                        continue;
                    }

                    canvas = mHolder.lockCanvas(null);
                    synchronized (mHolder) {
                        // reset canvas
                        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                        mIsClear = false;
                        doDraw(canvas);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (canvas != null) {
                        mHolder.unlockCanvasAndPost(canvas);
                    }
                }

            }
        }
    }

}
