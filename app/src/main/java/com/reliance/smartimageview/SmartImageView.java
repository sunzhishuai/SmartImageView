package com.reliance.smartimageview;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by sunzhishuai on 17/2/23.
 * E-mail itzhishuaisun@sina.com
 */

public class SmartImageView extends View {

    private Bitmap bgBitmap;
    private Paint mPaint;
    private Paint mMarkPaint;
    private int r = 30;
    private List<CirclePoint> points = new ArrayList<>();
    private ArrayList<CirclePoint> normalPoints = new ArrayList<>();
    private boolean isDelete = false;
    private float mMaxY = 0;
    private int measuredHeight;

    public SmartImageView(Context context) {
        this(context, null);
    }

    public SmartImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SmartImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
        getAttrs(context, attrs, defStyleAttr);
    }

    private void getAttrs(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SmartImageView, defStyleAttr, 0);
        int indexCount = typedArray.getIndexCount();
        for (int i = 0; i < indexCount; i++) {
            int index = typedArray.getIndex(i);
            switch (index) {
                case R.styleable.SmartImageView_smart_bg:
                    bgBitmap = ((BitmapDrawable) typedArray.getDrawable(index)).getBitmap();
                    break;
            }
        }

    }

    private void init() {
        mPaint = new Paint();
        mMarkPaint = new Paint();

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        if (widthMode == MeasureSpec.AT_MOST) {
            widthSize = bgBitmap.getWidth();
        }
        if (heightMode == MeasureSpec.AT_MOST) {
            heightSize = bgBitmap.getHeight();
        }
        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        measuredHeight = getMeasuredHeight();
        mMaxY = measuredHeight;
        if (!isDelete) {
            drawBg(canvas);
        } else {
            drawPoint(canvas, points);
        }
        makePoints();
    }

    private void drawPoint(Canvas canvas, List<CirclePoint> points) {
        for (int i = 0; i < points.size(); i++) {
            CirclePoint captureCircle = points.get(i);
            canvas.drawBitmap(captureCircle.circle, captureCircle.x - r, captureCircle.y - r, mPaint);
        }
    }

    private void makePoints() {
        new Thread() {
            @Override
            public void run() {
                if (points != null && points.size() == 0) {
                    int measuredWidth = getMeasuredWidth();
                    int measuredHeight = getMeasuredHeight();
                    int xCircleCounts = Math.round(measuredWidth / (2 * r) + 0.5f);
                    int YCircleCounts = Math.round(measuredHeight / (2 * r) + 0.5f);
                    for (int i = 0; i < YCircleCounts; i++) {
                        for (int j = 0; j < xCircleCounts; j++) {
                            CirclePoint captureCircle = getCaptureCircle(j * 2 * r + r, i * 2 * r + r, r);
                            points.add(captureCircle);
                            normalPoints.add(captureCircle.copy());
                        }
                    }
                }
            }
        }.start();

    }

    private void drawBg(Canvas canvas) {
        Rect rect = new Rect();
        rect.set(0, 0, bgBitmap.getWidth(), bgBitmap.getHeight());
        Rect rect1 = new Rect();
        rect1.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
        canvas.drawBitmap(bgBitmap, rect, rect1, mPaint);
    }

    public CirclePoint getCaptureCircle(int x, int y, int r) {
        CirclePoint circlePoint = new CirclePoint(r, x, y);
        int with = Math.round(getMeasuredWidth() / (2 * r) + 0.5f) * (2 * r);
        int height = Math.round(getMeasuredHeight() / (2 * r) + 0.5f) * (2 * r);
        Bitmap tempBitmap = Bitmap.createBitmap(with, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(tempBitmap);
        canvas.drawCircle(x, y, r, mMarkPaint);
        mMarkPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        Rect rect = new Rect();
        rect.set(0, 0, bgBitmap.getWidth(), bgBitmap.getHeight());
        Rect rect1 = new Rect();
        rect1.set(0, 0, tempBitmap.getWidth(), tempBitmap.getHeight());
        canvas.drawBitmap(bgBitmap, rect, rect1, mMarkPaint);
        //  canvas.drawBitmap(bgBitmap, 0, 0, mMarkPaint);
        mMarkPaint.setXfermode(null);
        Bitmap bitmap = Bitmap.createBitmap(tempBitmap, x - r, y - r, 2 * r, 2 * r, getMatrix(), false);
        circlePoint.circle = bitmap;
        return circlePoint;
    }


    private class CirclePoint {
        float r;
        float x;
        float y;
        Bitmap circle;

        public CirclePoint(float r, float x, float y) {
            this.r = r;
            this.x = x;
            this.y = y;
        }

        public CirclePoint copy() {
            CirclePoint circlePoint = new CirclePoint(this.r, this.x, this.y);
            circlePoint.circle = this.circle;
            return circlePoint;
        }
    }

    private int SHAKE_ANIMATION_FLAG = 1;
    private int MOVE_ANIMATION_FLAG = 2;
    private int shakeCount = 0;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == SHAKE_ANIMATION_FLAG && shakeCount < 5) {
                for (int i = 0; i < points.size(); i++) {
                    int randomX = new Random().nextInt(2 * r) - r;
                    int randomY = new Random().nextInt(2 * r) - r;
                    points.get(i).x = normalPoints.get(i).x + randomX;
                    points.get(i).y = normalPoints.get(i).y + randomY;
                }
                postInvalidate();
                if (shakeCount == 4) {
                    handler.sendEmptyMessageDelayed(MOVE_ANIMATION_FLAG, 50);
                } else {
                    handler.sendEmptyMessageDelayed(SHAKE_ANIMATION_FLAG, 50);
                }
                shakeCount++;
            }
            if (msg.what == MOVE_ANIMATION_FLAG) {
                for (int i = 0; i < points.size(); i++) {
                    int randomX = new Random().nextInt(r / 2);
                    mMaxY = Math.min(points.get(i).y, mMaxY);
                    points.get(i).y = points.get(i).y + randomX;
                }
                Log.e("postInvalidate", "postInvalidate");
                if (mMaxY < measuredHeight) {
                    handler.sendEmptyMessageDelayed(MOVE_ANIMATION_FLAG, 50);
                }
                postInvalidate();
            }
        }
    };


    private void shake() {
        if (isDelete) {
            handler.sendEmptyMessage(SHAKE_ANIMATION_FLAG);
        }
    }

    public void delete() {
        isDelete = true;
        shakeCount = 0;
        shake();
    }
}
