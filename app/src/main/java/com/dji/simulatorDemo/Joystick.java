package com.dji.simulatorDemo;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;


public class Joystick extends SurfaceView implements
        SurfaceHolder.Callback, View.OnTouchListener {
    public final static int DIRECTION_LEFT = 1;
    public final static int DIRECTION_RIGHT = 2;
    /**
     * 背景是左还是右
     */
    private int mBgOrientation = DIRECTION_LEFT;

    private Bitmap mJoystick;
    private Bitmap mJoystickBg;
    private Bitmap mJoystickArrow, mJoystickArrowHiLight;
    private SurfaceHolder mHolder;
    /**
     * 旋钮对应的显示范围
     */
    private Rect mKnobBounds;
    /**
     * 箭头对应的显示范围
     */
    private RectF mArrowBounds;
    /**
     * 背景对应的显示范围
     */
    private Rect mBgBounds;

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
     * 指示箭头大小
     */
    private float mArrowWidth, mArrowHeight;
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
     * 手指移动时的X,Y坐标
     */
    private float mMoveX, mMoveY;
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
        try {
            TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.Joystick);
            mBgOrientation = typedArray.getInt(R.styleable.Joystick_joy_bg, DIRECTION_LEFT);
            typedArray.recycle();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Resources res = getContext().getResources();
        mJoystick = BitmapFactory.decodeResource(res, R.drawable.joystick_cover_circle);
        mJoystickBg = BitmapFactory.decodeResource(res, mBgOrientation == DIRECTION_LEFT ? R.drawable.joystick_left : R.drawable.joystick_right);
        mJoystickArrow = BitmapFactory.decodeResource(res, R.drawable.joystick_highlight);
        mJoystickArrowHiLight = BitmapFactory.decodeResource(res, R.drawable.joystick_highlight_enable);
        mBgPaint = new Paint();
        mBgPaint.setAntiAlias(true);
        mBgPaint.setStrokeWidth(3f);
        mBgPaint.setColor(Color.BLACK);
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
        mArrowWidth = mBackgroundSize * 0.8f;
        mArrowHeight = mArrowWidth * 0.3f;
        mKnobBounds = new Rect();
        mArrowBounds = new RectF();
        mBgBounds = new Rect();
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
        //画旋钮
        mKnobBounds.set(mKnobX, mKnobY, mKnobX + mKnobSize, mKnobY + mKnobSize);
        pCanvas.drawBitmap(mJoystick, null, mKnobBounds, null);
        //画旋钮范围的背景
//        pCanvas.drawCircle(mDownX, mDownY, mRadius + mKnobSize * 0.5f, mBgPaint);
        mBgBounds.set(Math.round(mDownX - (mRadius + mKnobSize * 0.5f)), Math.round(mDownY - (mRadius + mKnobSize * 0.5f)), Math.round(mDownX + (mRadius + mKnobSize * 0.5f)), Math.round(mDownY + (mRadius + mKnobSize * 0.5f)));
        pCanvas.drawBitmap(mJoystickBg, null, mBgBounds, null);
        //画箭头
        double angle = Math.atan2(mMoveY - mDownY, mMoveX - mDownX);
        float r = mBgBounds.width() * 0.5f;
        float topX = (float) (mDownX + r * Math.cos(angle) - mArrowWidth * 0.6f);
        float topY = (float) (mDownY + r * Math.sin(angle) - mArrowHeight * 0.6f);
        float bottomX = (float) (mDownX + r * Math.cos(angle) + mArrowWidth * 0.6f);
        float bottomY = (float) (mDownY + r * Math.sin(angle) + mArrowHeight * 0.6f);
        float degree = (float) (Math.toDegrees(angle) + 90);
        Log.d("degree", "degree==" + degree);
        pCanvas.save();
        pCanvas.rotate(degree, topX + (bottomX - topX) * 0.5f, topY + (bottomY - topY) * 0.5f);
        mArrowBounds.set(topX, topY, bottomX, bottomY);
        boolean is0 = degree >= 0 && degree <= 2 || degree >= 358 && degree <= 360;
        boolean is90 = degree >= 88 && degree <= 92;
        boolean is180 = degree >= 178 && degree <= 182;
        boolean is270 = degree >= 268 && degree <= 272;
        if (is0 || is90 || is180 || is270) {
            pCanvas.drawBitmap(mJoystickArrowHiLight, null, mArrowBounds, null);
        } else {
            pCanvas.drawBitmap(mJoystickArrow, null, mArrowBounds, null);
        }
        pCanvas.restore();
    }

    @Override
    public boolean onTouch(final View arg0, final MotionEvent pEvent) {
        final float x = pEvent.getX();
        final float y = pEvent.getY();
        mMoveX = x;
        mMoveY = y;
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

//        px = (0.5f - (mKnobX / (mRadius * 2 - mKnobSize))) * -2;
//        py = (0.5f - (mKnobY / (mRadius * 2 - mKnobSize))) * 2;

        Log.d("touch222", "px===" + px + "   kx===" + mKnobX + "  dx===" + Math.round(mDownX)
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
