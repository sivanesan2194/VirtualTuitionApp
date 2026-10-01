package com.virtualtuition.app.whiteboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * DrawView:
 * Custom native Android View for smooth freehand drawing, virtual whiteboard,
 * color customization, brush size adjustment, eraser mode, undo/clear,
 * and high-resolution bitmap export.
 */
public class DrawView extends View {

    public static final int DEFAULT_COLOR = Color.BLACK;
    public static final int BACKGROUND_COLOR = Color.WHITE;
    public static final float DEFAULT_STROKE_WIDTH = 10f;
    private static final float TOUCH_TOLERANCE = 4f;

    private int currentColor = DEFAULT_COLOR;
    private float currentStrokeWidth = DEFAULT_STROKE_WIDTH;
    private boolean isEraserMode = false;

    private Path currentPath;
    private Paint drawPaint;
    private final List<StrokePath> paths = new ArrayList<>();
    private final List<StrokePath> redoPaths = new ArrayList<>();

    private float mX, mY;

    public DrawView(Context context) {
        super(context);
        init();
    }

    public DrawView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DrawView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        currentPath = new Path();
        drawPaint = new Paint();
        drawPaint.setAntiAlias(true);
        drawPaint.setDither(true);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
        drawPaint.setColor(currentColor);
        drawPaint.setStrokeWidth(currentStrokeWidth);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw whiteboard white background
        canvas.drawColor(BACKGROUND_COLOR);

        // Render recorded paths
        for (StrokePath stroke : paths) {
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setDither(true);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(stroke.strokeWidth);
            paint.setColor(stroke.isEraser ? BACKGROUND_COLOR : stroke.color);
            canvas.drawPath(stroke.path, paint);
        }

        // Render currently active path
        if (currentPath != null && !currentPath.isEmpty()) {
            drawPaint.setColor(isEraserMode ? BACKGROUND_COLOR : currentColor);
            drawPaint.setStrokeWidth(currentStrokeWidth);
            canvas.drawPath(currentPath, drawPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStart(x, y);
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                touchMove(x, y);
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
                touchUp();
                invalidate();
                return true;

            default:
                return false;
        }
    }

    private void touchStart(float x, float y) {
        redoPaths.clear();
        currentPath = new Path();
        currentPath.moveTo(x, y);
        mX = x;
        mY = y;
    }

    private void touchMove(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);

        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            // Quadratic Bezier curve smoothing
            currentPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;
        }
    }

    private void touchUp() {
        currentPath.lineTo(mX, mY);
        paths.add(new StrokePath(currentPath, currentColor, currentStrokeWidth, isEraserMode));
        currentPath = new Path();
    }

    public void undo() {
        if (!paths.isEmpty()) {
            StrokePath removed = paths.remove(paths.size() - 1);
            redoPaths.add(removed);
            invalidate();
        }
    }

    public void redo() {
        if (!redoPaths.isEmpty()) {
            StrokePath restored = redoPaths.remove(redoPaths.size() - 1);
            paths.add(restored);
            invalidate();
        }
    }

    public void clearCanvas() {
        paths.clear();
        redoPaths.clear();
        currentPath = new Path();
        invalidate();
    }

    public void setStrokeColor(int color) {
        this.currentColor = color;
        this.isEraserMode = false;
    }

    public void setBrushThickness(float thickness) {
        this.currentStrokeWidth = thickness;
    }

    public float getBrushThickness() {
        return this.currentStrokeWidth;
    }

    public void setEraserMode(boolean eraserMode) {
        this.isEraserMode = eraserMode;
    }

    public boolean isEraserMode() {
        return isEraserMode;
    }

    /**
     * Exports the current whiteboard drawing as a high-resolution Bitmap.
     */
    public Bitmap exportBitmap() {
        int width = getWidth() > 0 ? getWidth() : 1080;
        int height = getHeight() > 0 ? getHeight() : 1920;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        draw(canvas);
        return bitmap;
    }
}
