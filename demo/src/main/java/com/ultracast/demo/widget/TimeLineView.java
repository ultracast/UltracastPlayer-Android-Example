package com.ultracast.demo.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class TimeLineView extends View {

    public interface Listener {
        //        void onScrollingStart();
        void onScrolling(int msec);

        void onScrollingStop(int msec);
    }

    private static final String TAG = "ProgressBar";
    //all value in dp
    private static final int SLIDER_HEIGHT = 48;
    private static final int SLIDER_RADIUS = 8;
    private static final int PADDING = 6;
    private static final int TEXT_SIZE = 14;

    private int mCurrentTime;
    private int mDuration;

    private Rect mBackgroundRect;
    private Rect mScrubberRect;
    private Rect mPlayedRect;
    private Rect mDeffaultTextRect;
    private Paint mBackgroundPaint;
    private Paint mScrubberPaint;
    private Paint mPlayedPaint;
    private Paint mTextPaint;
    private Paint mSliderPaint;

    private int mSliderX;
    private int mSliderY;
    private int mSliderRadius;
    private int mSliderHeight;
    private boolean mSliding;
    private boolean mShowProgress;

    private int mPadding;
    private Rect mBounds = new Rect();
    private Listener mListener;

    public TimeLineView(Context context) {
        super(context);
        init();
    }

    public TimeLineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mShowProgress = true;
        mBackgroundRect = new Rect();
        mScrubberRect = new Rect();
        mPlayedRect = new Rect();
        mDeffaultTextRect = new Rect();

        mBackgroundPaint = new Paint();
        mPlayedPaint = new Paint();
        mScrubberPaint = new Paint();
        mSliderPaint = new Paint();
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mBackgroundPaint.setColor(0x00FFFFFF);//invisible
        mScrubberPaint.setColor(0xFF9E9E9E);
        mPlayedPaint.setColor(0xFF0077EA);
        mSliderPaint.setColor(0xFFFFFFFF);

        mTextPaint.setColor(0xFFFFFFFF);
        mTextPaint.setTextAlign(Paint.Align.LEFT);
        DisplayMetrics metrics = getResources().getDisplayMetrics();

        mTextPaint.setTextSize(TEXT_SIZE * metrics.density);
        mTextPaint.getTextBounds("000:00:00", 0, 8, mDeffaultTextRect);
        mSliderRadius = (int) (SLIDER_RADIUS * metrics.density);
        mSliderHeight = (int) (SLIDER_HEIGHT * metrics.density);
        mPadding = (int) (PADDING * metrics.density);

        //              SLIDER_RADIUS       SLIDER_HEIGHT       PADDING
        //------------------------------------------------------------------------------------------
        //   mdpi 1         6           |       48          |       6
        //   hdpi 1.5       9           |       64          |       9
        //  xhdpi 2         12          |       96          |       12
        // xxhdpi 3         18          |       144         |       18
        //xxxhdpi 4         24          |       184         |       24
        //------------------------------------------------------------------------------------------
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(mBackgroundRect, mBackgroundPaint);
        canvas.drawRect(mScrubberRect, mScrubberPaint);


        String time = toString(mCurrentTime);
        mTextPaint.getTextBounds(time, 0, time.length(), mBounds);

        canvas.drawText(toString(mCurrentTime),
                mScrubberRect.left - mSliderRadius - mPadding - mBounds.width(),
                getHeight() / 2 + mDeffaultTextRect.height() / 2 - mDeffaultTextRect.bottom,
                mTextPaint);

        if (mShowProgress) {
            canvas.drawRect(mPlayedRect, mPlayedPaint);
            canvas.drawCircle(mSliderX, mSliderY, mSliderRadius, mSliderPaint);

            canvas.drawText(toString(mDuration),
                    mScrubberRect.right + mPadding + mSliderRadius,
                    getHeight() / 2 + mDeffaultTextRect.height() / 2 - mDeffaultTextRect.bottom,
                    mTextPaint);
        } else {
            String live = "LIVE";
            mTextPaint.getTextBounds(live, 0, live.length(), mBounds);

            mTextPaint.setColor(0xFFF44336); // set the red color of "LIVE" text
            canvas.drawText(live,
                    mScrubberRect.right + mPadding + mSliderRadius,
                    getHeight() / 2 + mBounds.height() / 2 - mBounds.bottom,
                    mTextPaint);
            mTextPaint.setColor(0xFFFFFFFF); //return the white text color
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;
        int half = (height) / 2;

        mBackgroundRect.set(getPaddingLeft(),
                getPaddingTop(),
                width - getPaddingRight(),
                height - getPaddingBottom());

        mScrubberRect.set(
                getPaddingLeft() + 2 * mPadding + mDeffaultTextRect.width(),
                half - 2,
                width - getPaddingRight() - 2 * mPadding - mDeffaultTextRect.width(),
                half + 2);

        mSliderY = half;
        update();
    }

    public void setTime(int currentTime, int duration) {
        mCurrentTime = currentTime;
        mDuration = duration;
        mShowProgress = mDuration > 0;
        update();
    }

    public int getProgressbarHeight() {
        return mSliderHeight;
    }

    private void update() {
        mPlayedRect.set(mScrubberRect);

        if (mSliding) {
            mPlayedRect.right = mSliderX;
        } else {
            if (mShowProgress) {
                if (mCurrentTime > 0) {
                    //move to current video's position
                    mPlayedRect.right = mPlayedRect.left + ((mScrubberRect.width() * mCurrentTime) / mDuration);
                } else {
                    //move to play position
                    mPlayedRect.right = mScrubberRect.left;
                }
                mSliderX = mPlayedRect.right;
            }
        }

        //redraw
        invalidate();
    }

    private int getTime() {
        return ((mPlayedRect.right - mScrubberRect.left) * mDuration) / mScrubberRect.width();
    }

    private String toString(int msec) {
        int sec = (msec / 1000) % 60;
        int min = ((msec / 1000) / 60) % 60;
        int hour = (msec / 1000) / 3600;
        String time;
        if (hour > 0) {
            time = String.format("%d:%02d:%02d", hour, min, sec);
        } else {
            time = String.format("%02d:%02d", min, sec);
        }
        return time;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        if (x >= mScrubberRect.left && x <= mScrubberRect.right) {
            Log.d(TAG, "Event " + event);
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    mSliding = true;
//                    if (mListener != null)
//                        mListener.onScrollingStart();
                }
                case MotionEvent.ACTION_MOVE: {
                    mSliderX = x;
                    update();
                    if (mListener != null)
                        mListener.onScrolling(getTime());
                    break;
                }
                case MotionEvent.ACTION_UP: {
                    mSliding = false;
                    if (mListener != null)
                        mListener.onScrollingStop(getTime());
                    break;
                }
                default:
                    super.onTouchEvent(event);
            }
        } else {
            mSliding = false;
        }
        return true;
    }
}