package com.anvexgroup.apnuvyapar.utils;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class WavePlaceholderDrawable extends Drawable implements Animatable {

    private static final int BASE_COLOR = Color.rgb(235, 240, 245);
    private static final int HIGHLIGHT_COLOR = Color.rgb(250, 252, 255);
    private static final long DURATION_MS = 1150L;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final boolean circle;
    private ValueAnimator animator;
    private float progress = -1f;
    private boolean running = false;

    public WavePlaceholderDrawable() {
        this(false);
    }

    public WavePlaceholderDrawable(boolean circle) {
        this.circle = circle;
        paint.setStyle(Paint.Style.FILL);
        start();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.width() <= 0 || bounds.height() <= 0) return;

        float width = bounds.width();
        float height = bounds.height();
        float waveWidth = width * 0.55f;
        float startX = bounds.left + (progress * (width + waveWidth)) - waveWidth;

        LinearGradient gradient = new LinearGradient(
                startX, bounds.top,
                startX + waveWidth, bounds.bottom,
                new int[]{BASE_COLOR, HIGHLIGHT_COLOR, BASE_COLOR},
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP
        );
        paint.setShader(gradient);

        if (circle) {
            float radius = Math.min(width, height) / 2f;
            canvas.drawCircle(bounds.exactCenterX(), bounds.exactCenterY(), radius, paint);
        } else {
            float radius = Math.min(width, height) * 0.08f;
            canvas.drawRoundRect(bounds.left, bounds.top, bounds.right, bounds.bottom, radius, radius, paint);
        }

        paint.setShader(null);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void start() {
        if (running) return;
        running = true;

        if (animator == null) {
            animator = ValueAnimator.ofFloat(-1f, 1f);
            animator.setDuration(DURATION_MS);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(animation -> {
                progress = (float) animation.getAnimatedValue();
                invalidateSelf();
            });
        }
        animator.start();
    }

    @Override
    public void stop() {
        running = false;
        if (animator != null) {
            animator.cancel();
        }
    }

    @Override
    public boolean isRunning() {
        return running && animator != null && animator.isRunning();
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        boolean changed = super.setVisible(visible, restart);
        if (visible) {
            if (restart) stop();
            start();
        } else {
            stop();
        }
        return changed;
    }
}
