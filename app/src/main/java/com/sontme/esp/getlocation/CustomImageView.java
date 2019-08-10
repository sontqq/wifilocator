package com.sontme.esp.getlocation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;

public class CustomImageView extends android.support.v7.widget.AppCompatImageView {

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap.copy(Bitmap.Config.RGB_565, true);
    }

    Bitmap bitmap;

    public CustomImageView(Context context) {
        super(context);
    }

    public CustomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onDraw(Canvas canvas) {

        Paint p = new Paint();
        //p.setStrokeWidth(10);
        //p.setColor(Color.argb(100, 0, 0, 0));
        canvas.drawBitmap(bitmap, 0, 0, p);

        super.onDraw(canvas);
        this.postInvalidate();
    }

}
