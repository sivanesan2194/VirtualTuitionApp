package com.virtualtuition.app.whiteboard;

import android.graphics.Path;

public class StrokePath {
    public Path path;
    public int color;
    public float strokeWidth;
    public boolean isEraser;

    public StrokePath(Path path, int color, float strokeWidth, boolean isEraser) {
        this.path = path;
        this.color = color;
        this.strokeWidth = strokeWidth;
        this.isEraser = isEraser;
    }
}
