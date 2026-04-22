package com.jamvote.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public class GeometricPatternView extends View {
    private Paint paint;
    private int goldColor;

    public GeometricPatternView(Context context) {
        super(context);
        init();
    }

    public GeometricPatternView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        goldColor = ContextCompat.getColor(getContext(), R.color.wakandan_gold);
        paint = new Paint();
        paint.setColor(goldColor);
        paint.setStrokeWidth(1.5f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAlpha(18); // Approx 7% alpha
        paint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        int spacing = 60; // Grid spacing in pixels

        for (int y = 0; y < height + spacing; y += spacing) {
            for (int x = 0; x < width + spacing; x += spacing) {
                // Drawing triangle grid
                // Triangle 1
                canvas.drawLine(x, y, x + spacing, y, paint);
                canvas.drawLine(x, y, x + spacing / 2f, y + spacing, paint);
                canvas.drawLine(x + spacing, y, x + spacing / 2f, y + spacing, paint);
            }
        }
    }
}