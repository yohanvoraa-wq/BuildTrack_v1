package com.example.buildtrack_v1;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.appcompat.widget.AppCompatImageView;

/** ImageView with pinch-to-zoom, drag and double-tap zoom. No extra library needed. */
public class ZoomImageView extends AppCompatImageView {

    private static final float MAX_ZOOM = 5f;

    private final Matrix matrix = new Matrix();
    private final float[] values = new float[9];
    private float baseScale = 1f;
    private float lastX, lastY;
    private boolean dragging;

    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector tapDetector;

    public ZoomImageView(Context c) { this(c, null); }

    public ZoomImageView(Context c, AttributeSet a) { this(c, a, 0); }

    public ZoomImageView(Context c, AttributeSet a, int s) {
        super(c, a, s);
        setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(c, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(ScaleGestureDetector d) {
                zoomBy(d.getScaleFactor(), d.getFocusX(), d.getFocusY());
                return true;
            }
        });
        tapDetector = new GestureDetector(c, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) { return true; }

            @Override public boolean onDoubleTap(MotionEvent e) {
                if (currentScale() > baseScale * 1.05f) fitToView();
                else zoomBy(2.5f, e.getX(), e.getY());
                return true;
            }
        });
    }

    @Override public void setImageDrawable(Drawable d) {
        super.setImageDrawable(d);
        fitToView();
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        fitToView();
    }

    private float currentScale() {
        matrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }

    private void fitToView() {
        Drawable d = getDrawable();
        int vw = getWidth(), vh = getHeight();
        if (d == null || vw == 0 || vh == 0 || d.getIntrinsicWidth() <= 0) return;
        float dw = d.getIntrinsicWidth(), dh = d.getIntrinsicHeight();
        baseScale = Math.min(vw / dw, vh / dh);
        matrix.reset();
        matrix.postScale(baseScale, baseScale);
        matrix.postTranslate((vw - dw * baseScale) / 2f, (vh - dh * baseScale) / 2f);
        setImageMatrix(matrix);
    }

    private void zoomBy(float factor, float px, float py) {
        float target = currentScale() * factor;
        float min = baseScale, max = baseScale * MAX_ZOOM;
        if (target < min) factor = min / currentScale();
        else if (target > max) factor = max / currentScale();
        matrix.postScale(factor, factor, px, py);
        keepInside();
        setImageMatrix(matrix);
    }

    /** Stops the picture from being dragged off-screen; small pictures stay centred. */
    private void keepInside() {
        Drawable d = getDrawable();
        if (d == null) return;
        RectF r = new RectF(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        matrix.mapRect(r);
        float dx = 0, dy = 0;
        int vw = getWidth(), vh = getHeight();
        if (r.width() <= vw) dx = (vw - r.width()) / 2f - r.left;
        else if (r.left > 0) dx = -r.left;
        else if (r.right < vw) dx = vw - r.right;
        if (r.height() <= vh) dy = (vh - r.height()) / 2f - r.top;
        else if (r.top > 0) dy = -r.top;
        else if (r.bottom < vh) dy = vh - r.bottom;
        matrix.postTranslate(dx, dy);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        scaleDetector.onTouchEvent(e);
        tapDetector.onTouchEvent(e);

        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = e.getX();
                lastY = e.getY();
                dragging = true;
                getParent().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_MOVE:
                if (dragging && !scaleDetector.isInProgress() && e.getPointerCount() == 1) {
                    matrix.postTranslate(e.getX() - lastX, e.getY() - lastY);
                    keepInside();
                    setImageMatrix(matrix);
                }
                lastX = e.getX();
                lastY = e.getY();
                break;
            case MotionEvent.ACTION_POINTER_UP:
                // Re-anchor on the finger that stays down so the picture does not jump.
                int keep = e.getActionIndex() == 0 ? 1 : 0;
                lastX = e.getX(keep);
                lastY = e.getY(keep);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                break;
        }
        return true;
    }
}
