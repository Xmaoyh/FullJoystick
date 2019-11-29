package com.dji.simulatorDemo;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import dji.common.error.DJIError;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;

/**
 * @author MaoYiHan
 * @date 2019/11/28
 */
public class TestActivity extends Activity {
    private Joystick mScreenJoystickRight, mScreenJoystickLeft;
    private float mPitch;
    private float mRoll;
    private float mYaw;
    private float mThrottle;
    private Timer mSendVirtualStickDataTimer;
    private SendVirtualStickDataTask mSendVirtualStickDataTask;
    private FlightController mFlightController;
    private CheckForLongPress mPendingCheckForLongPress;
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_test);

        initView();
    }

    private void initView() {
        Aircraft aircraft = DJISimulatorApplication.getAircraftInstance();
        if (aircraft == null || !aircraft.isConnected()) {
            mFlightController = null;
            return;
        }
        mFlightController = aircraft.getFlightController();
        mFlightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
        mFlightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
        mFlightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
        mFlightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
        mScreenJoystickLeft = findViewById(R.id.left_joyStick);
        mScreenJoystickRight = findViewById(R.id.right_joyStick);
        mScreenJoystickRight.setJoystickListener(new JoystickListener() {
            @Override
            public void onTouch(Joystick joystick, float pX, float pY) {
                if (Math.abs(pX) < 0.02) {
                    pX = 0;
                }

                if (Math.abs(pY) < 0.02) {
                    pY = 0;
                }

                float pitchJoyControlMaxSpeed = 10;
                float rollJoyControlMaxSpeed = 10;

                mPitch = (float) (pitchJoyControlMaxSpeed * pX);

                mRoll = (float) (rollJoyControlMaxSpeed * pY);

                if (null == mSendVirtualStickDataTimer) {
                    mSendVirtualStickDataTask = new SendVirtualStickDataTask();
                    mSendVirtualStickDataTimer = new Timer();
                    mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 100, 200);
                }

            }

        });

        mScreenJoystickLeft.setJoystickListener(new JoystickListener() {

            @Override
            public void onTouch(Joystick joystick, float pX, float pY) {
                if (Math.abs(pX) < 0.02) {
                    pX = 0;
                }

                if (Math.abs(pY) < 0.02) {
                    pY = 0;
                }
                float verticalJoyControlMaxSpeed = 2;
                float yawJoyControlMaxSpeed = 30;

                mYaw = (float) (yawJoyControlMaxSpeed * pX);
                mThrottle = (float) (verticalJoyControlMaxSpeed * pY);

                if (null == mSendVirtualStickDataTimer) {
                    mSendVirtualStickDataTask = new SendVirtualStickDataTask();
                    mSendVirtualStickDataTimer = new Timer();
                    mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 0, 200);
                }

            }
        });
    }

    class SendVirtualStickDataTask extends TimerTask {
        @Override
        public void run() {

            if (mFlightController != null) {
                Log.e("222", "mPitch = " + mPitch + ",mRoll = " + mRoll + ",mYaw = " + mYaw + ",mThrottle" + mThrottle);
                if (!mFlightController.getState().isFlying()) {
                    checkPress();
                }
                mFlightController.sendVirtualStickFlightControlData(
                        new FlightControlData(
                                mPitch, mRoll, mYaw, mThrottle
                        ), new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {

                            }
                        }
                );
            }
        }
    }

    private void checkPress() {
        if (mPendingCheckForLongPress == null) {
            mPendingCheckForLongPress = new CheckForLongPress();
        }
        mPendingCheckForLongPress.rememberPressedState();
        mHandler.postDelayed(mPendingCheckForLongPress, 500);
    }

    private boolean isPressed() {
        return mThrottle < -1 && mPitch < -6.5 && mRoll < -6.9;
    }

    private final class CheckForLongPress implements Runnable {
        private boolean mOriginalPressedState;

        @Override
        public void run() {
            //如果500毫秒后这个状态还是press（按住）的状态，那说明进行了长按。
            if (mOriginalPressedState && isPressed() && !mFlightController.getState().isFlying()) {
                Log.e("222", "takeOffTrue");
                mFlightController.startTakeoff(null);
            } else {
                Log.e("222", "takeOffFalse");
            }
        }

        public void rememberPressedState() {
            mOriginalPressedState = isPressed();
        }
    }
}
