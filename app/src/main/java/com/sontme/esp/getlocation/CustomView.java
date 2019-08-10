package com.sontme.esp.getlocation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

class CustomView extends View {
    Camera.Face face;
    float left;
    float right;
    float top;
    float bottom;
    int score;
    Point eye1;
    Point eye2;
    Point mouth;
    Rect rect;

    //region GETTERS SETTERS

    public void setEye1(float x, float y) {
        this.eye1.x = (int) x;
        this.eye1.y = (int) y;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void setEye1(Point eye1) {
        this.eye1 = eye1;
    }

    public void setEye2(Point eye2) {
        this.eye2 = eye2;
    }

    public void setMouth(Point mouth) {
        this.mouth = mouth;
    }

    public Rect getRect() {
        return this.rect;
    }

    public void setRect(Rect rect) {
        this.rect = rect;
    }

    public float getLeftx() {
        return this.left;
    }

    public float getRightx() {
        return this.right;
    }

    public float getTopx() {
        return this.top;
    }

    public float getBottomx() {
        return this.bottom;
    }

    public void setLeft(float left) {
        this.left = left;
    }

    public void setRight(float right) {
        this.right = right;
    }

    public void setTop(float top) {
        this.top = top;
    }

    public void setBottom(float bottom) {
        this.bottom = bottom;
    }

    public void setObject(Camera.Face face) {
        this.face = face;
        this.left = face.rect.left;
        this.right = face.rect.right;
        this.top = face.rect.top;
        this.bottom = face.rect.bottom;
        //Log.d("camera_api", "FACE_1: " + face.rect.toString());
    }

    public void setTag(Camera.Face face) {
        this.face = face;
        this.left = face.rect.left;
        this.right = face.rect.right;
        this.top = face.rect.top;
        this.bottom = face.rect.bottom;
        //Log.d("camera_api", "FACE_2: " + face.rect.toString());
    }
    //endregion

    public CustomView(Context context) {
        this(context, null);
    }

    public CustomView(Context context, AttributeSet at) {
        super(context, at);
        Log.d("camera_api", "constructor called 1");
    }

    public void rajz() {
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Paint paint_Face = new Paint();
        Paint paint_Eye = new Paint();
        Paint paint_Mouth = new Paint();
        paint_Face.setStrokeWidth(10);
        paint_Eye.setStrokeWidth(10);
        paint_Mouth.setStrokeWidth(10);
        /*
        paint_Face.setColor(Color.argb(70, 30, 30, 255));
        paint_Eye.setColor(Color.argb(70, 255, 30, 30));
        paint_Mouth.setColor(Color.argb(70, 30, 255, 30));
        */
        paint_Face.setColor(Color.RED);
        paint_Eye.setColor(Color.GREEN);
        paint_Mouth.setColor(Color.BLUE);
        if (eye1 != null) {
            /*SZEM1*/
            canvas.drawCircle(Math.abs(eye1.x), Math.abs(eye1.y), 30, paint_Eye);
        }
        if (eye2 != null) {
            /*SZEM2*/
            canvas.drawCircle(Math.abs(eye2.x), Math.abs(eye2.y), 30, paint_Eye);
        }
        if (mouth != null) {
            /*SZAJ*/
            canvas.drawCircle(Math.abs(mouth.x), Math.abs(mouth.y), 30, paint_Mouth);
        }
        /*ARC*/
        if (rect != null) {
            canvas.drawRect(rect, paint_Face);
            Log.d("camera_api", "drawing: " + left + "_" + right + "_" + top + "_" + bottom);
            if (Math.abs(left) != 0.0f) {
                canvas.drawRect(Math.abs(left), Math.abs(top), Math.abs(right), Math.abs(bottom), paint_Face);
            }
        }
        //invalidate();
    }
}
