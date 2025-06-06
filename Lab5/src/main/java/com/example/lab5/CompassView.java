package com.example.lab5;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class CompassView extends View {
    private float azimuth = 0;
    private Paint circlePaint;
    private Paint markerPaint;
    private Paint arrowPaint;
    private Paint textPaint;
    private String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
    private float[] directionAngles = {0, 45, 90, 135, 180, 225, 270, 315};

    public CompassView(Context context) {
        super(context);
        init();
    }

    public CompassView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CompassView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(Color.WHITE);
        circlePaint.setStyle(Paint.Style.FILL);
        circlePaint.setStrokeWidth(1);

        markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        markerPaint.setColor(Color.BLACK);
        markerPaint.setStyle(Paint.Style.STROKE);
        markerPaint.setStrokeWidth(2);

        arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arrowPaint.setColor(Color.RED);
        arrowPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(30);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void updateAzimuth(float azimuth) {
        this.azimuth = azimuth;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int centerX = width / 2;
        int centerY = height / 2;
        int radius = Math.min(centerX, centerY) - 20;

        canvas.drawCircle(centerX, centerY, radius, circlePaint);
        canvas.drawCircle(centerX, centerY, radius, markerPaint);

        for (int i = 0; i < directions.length; i++) {
            float angle = (float) Math.toRadians(directionAngles[i]);
            float markerX = (float) (centerX + radius * 0.8 * Math.sin(angle));
            float markerY = (float) (centerY - radius * 0.8 * Math.cos(angle));
            float textX = (float) (centerX + radius * 0.9 * Math.sin(angle));
            float textY = (float) (centerY - radius * 0.9 * Math.cos(angle) + 10);

            canvas.drawLine(centerX, centerY, markerX, markerY, markerPaint);
            canvas.drawText(directions[i], textX, textY, textPaint);
        }

        canvas.save();
        canvas.rotate(-azimuth, centerX, centerY);

        Path path = new Path();
        float arrowLength = radius * 0.7f;
        float arrowWidth = 15;

        path.moveTo(centerX, centerY - arrowLength);
        path.lineTo(centerX - arrowWidth, centerY);
        path.lineTo(centerX + arrowWidth, centerY);
        path.close();

        canvas.drawPath(path, arrowPaint);
        canvas.restore();
    }
}