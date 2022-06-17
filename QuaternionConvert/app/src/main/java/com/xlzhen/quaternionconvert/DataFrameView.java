package com.xlzhen.quaternionconvert;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.Map;

public class DataFrameView extends View {
    private Paint paint;
    private Path path;
    private DataFrame dataFrame;
    private int width,height;
    public DataFrameView(Context context) {
        super(context);
        init(context);
    }

    public DataFrameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10);

    }

    public void setDataFrame(DataFrame dataFrame) {
        this.dataFrame = dataFrame;
        for(double[] doubles:dataFrame){
            float x = (float) doubles[0];
            float y = (float) doubles[1];
            float z = (float) doubles[2];
            Log.v("xyz",x+" , "+y+" , "+z);
            if(path == null){
                path = new Path();
                path.moveTo(x,y);
            }else {
                path.lineTo(x, y);
            }
        }
        invalidate();
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        canvas.translate(width/3f,height/3f);
        canvas.drawPath(path,paint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
    }
}
