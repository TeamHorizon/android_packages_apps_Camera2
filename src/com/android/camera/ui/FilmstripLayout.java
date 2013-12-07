/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.camera.ui;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.android.camera2.R;

/**
 * A {@link android.widget.FrameLayout} used for the parent layout of a
 * {@link com.android.camera.ui.FilmstripView} to support animating in/out the
 * filmstrip.
 */
public class FilmstripLayout extends FrameLayout {

    // TODO: Remove this quick hack.
    public interface Listener {
        void onFilmstripHidden();
    }

    private static final long DEFAULT_DURATION_MS = 200;
    private static final int ANIM_DIRECTION_IN = 1;
    private static final int ANIM_DIRECTION_OUT = 2;
    private FilmstripView mFilmstripView;
    private FilmstripGestureRecognizer mGestureRecognizer;
    private FilmstripGestureRecognizer.Listener mFilmstripGestureListener;
    private final ValueAnimator mFilmstripAnimator = ValueAnimator.ofFloat(null);
    private Listener mListener;
    private int mSwipeTrend;
    private MyBackgroundDrawable mBackgroundDrawable;
    private int mAnimationDirection;
    private boolean mHiding;

    private Animator.AnimatorListener mFilmstripAnimatorListener = new Animator.AnimatorListener() {
        private boolean mCanceled;

        @Override
        public void onAnimationStart(Animator animator) {
            mCanceled = false;
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            if (!mCanceled) {
                if (mFilmstripView.getTranslationX() != 0f) {
                    mFilmstripView.getController().goToFilmStrip();
                    setVisibility(INVISIBLE);
                    // TODO: Remove this quick hack.
                    if (mListener != null) {
                        mListener.onFilmstripHidden();
                    }
                    setHiding(false);
                } else {
                    setHiding(true);
                }
            }
        }

        @Override
        public void onAnimationCancel(Animator animator) {
            mCanceled = true;
        }

        @Override
        public void onAnimationRepeat(Animator animator) {
            // Nothing.
        }
    };

    private ValueAnimator.AnimatorUpdateListener mFilmstripAnimatorUpdateListener =
            new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    if (mAnimationDirection == ANIM_DIRECTION_IN && !mHiding) {
                        mBackgroundDrawable.setFraction(valueAnimator.getAnimatedFraction());
                    }
                    mFilmstripView.setTranslationX((Float) valueAnimator.getAnimatedValue());
                    invalidate();
                }
            };

    public FilmstripLayout(Context context) {
        super(context);
        init(context);
    }

    public FilmstripLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FilmstripLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mGestureRecognizer = new FilmstripGestureRecognizer(context, new MyGestureListener());
        mFilmstripAnimator.setDuration(DEFAULT_DURATION_MS);
        mFilmstripAnimator.addUpdateListener(mFilmstripAnimatorUpdateListener);
        mFilmstripAnimator.addListener(mFilmstripAnimatorListener);
    }

    public void setListener(Listener l) {
        mListener = l;
    }

    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed && mFilmstripView != null && getVisibility() == INVISIBLE) {
            mFilmstripView.setTranslationX(getMeasuredWidth());
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return mGestureRecognizer.onTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            // TODO: Remove this after the touch flow refactor is done in
            // MainAtivityLayout.
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        return false;
    }

    @Override
    public void onFinishInflate() {
        mBackgroundDrawable = new MyBackgroundDrawable();
        setBackground(mBackgroundDrawable);
        mFilmstripView = (FilmstripView) findViewById(R.id.filmstrip_view);
        mFilmstripView.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                // Adjust the coordinates back since they are relative to the
                // child view.
                motionEvent.setLocation(motionEvent.getX() + view.getX(),
                        motionEvent.getY() + view.getY());
                mGestureRecognizer.onTouchEvent(motionEvent);
                return true;
            }
        });
        mFilmstripGestureListener = mFilmstripView.getGestureListener();
    }

    /**
     * Called when the back key is pressed.
     *
     * @return Whether the UI responded to the key event.
     */
    public boolean onBackPressed() {
        if (getVisibility() == VISIBLE) {
            if (!mFilmstripAnimator.isRunning()) {
                hideFilmstrip();
            }
            return true;
        }
        return false;
    }

    private void hideFilmstrip() {
        mAnimationDirection = ANIM_DIRECTION_OUT;
        runAnimation(mFilmstripView.getTranslationX(), getMeasuredWidth());
    }

    private void showFilmstrip() {
        mAnimationDirection = ANIM_DIRECTION_IN;
        runAnimation(mFilmstripView.getTranslationX(), 0);
    }

    private void runAnimation(float begin, float end) {
        if (mFilmstripAnimator.isRunning()) {
            return;
        }
        if (begin == end) {
            // No need to start animation.
            mFilmstripAnimatorListener.onAnimationEnd(mFilmstripAnimator);
            return;
        }
        mFilmstripAnimator.setFloatValues(begin, end);
        mFilmstripAnimator.start();
    }

    private void setHiding(boolean hiding) {
        mHiding = hiding;
        if (!mHiding) {
            mBackgroundDrawable.setFraction(0f);
        }
    }

    /**
     * A gesture listener which passes all the gestures to the
     * {@code mFilmstripView} by default and only intercepts scroll gestures
     * when the {@code mFilmstripView} is not in full-screen.
     */
    private class MyGestureListener implements FilmstripGestureRecognizer.Listener {
        @Override
        public boolean onScroll(float x, float y, float dx, float dy) {
            if (mFilmstripAnimator.isRunning()) {
                return true;
            }
            if (mFilmstripView.getTranslationX() == 0f &&
                    mFilmstripGestureListener.onScroll(x, y, dx, dy)) {
                return true;
            }
            mSwipeTrend = (((int) dx) >> 1) + (mSwipeTrend >> 1);
            float translate = mFilmstripView.getTranslationX() - dx;
            if (translate < 0f) {
                translate = 0f;
            } else {
                if (translate > getMeasuredWidth()) {
                    translate = getMeasuredWidth();
                }
            }
            mFilmstripView.setTranslationX(translate);
            invalidate();
            return true;
        }

        @Override
        public boolean onSingleTapUp(float x, float y) {
            if (mFilmstripView.getTranslationX() == 0f) {
                return mFilmstripGestureListener.onSingleTapUp(x, y);
            }
            return false;
        }

        @Override
        public boolean onDoubleTap(float x, float y) {
            if (mFilmstripView.getTranslationX() == 0f) {
                return mFilmstripGestureListener.onDoubleTap(x, y);
            }
            return false;
        }

        @Override
        public boolean onFling(float velocityX, float velocityY) {
            if (mFilmstripView.getTranslationX() == 0f) {
                return mFilmstripGestureListener.onFling(velocityX, velocityY);
            }
            return false;
        }

        @Override
        public boolean onScaleBegin(float focusX, float focusY) {
            if (mFilmstripView.getTranslationX() == 0f) {
                return mFilmstripGestureListener.onScaleBegin(focusX, focusY);
            }
            return false;
        }

        @Override
        public boolean onScale(float focusX, float focusY, float scale) {
            if (mFilmstripView.getTranslationX() == 0f) {
                return mFilmstripGestureListener.onScale(focusX, focusY, scale);
            }
            return false;
        }

        @Override
        public boolean onDown(float x, float y) {
            if (mFilmstripView.getTranslationX() == 0f) {
                return mFilmstripGestureListener.onDown(x, y);
            }
            return false;
        }

        @Override
        public boolean onUp(float x, float y) {
            if (mFilmstripView.getTranslationX() == 0f) {
                return mFilmstripGestureListener.onUp(x, y);
            }
            if (mSwipeTrend < 0) {
                hideFilmstrip();
            } else {
                showFilmstrip();
            }
            mSwipeTrend = 0;
            return false;
        }

        @Override
        public void onScaleEnd() {
            if (mFilmstripView.getTranslationX() == 0f) {
                mFilmstripGestureListener.onScaleEnd();
            }
        }
    }

    private class MyBackgroundDrawable extends Drawable {
        private Paint mPaint;
        private float mFraction;

        public MyBackgroundDrawable() {
            mPaint = new Paint();
            mPaint.setColor(0);
            mPaint.setAlpha(255);
        }

        public void setFraction(float f) {
            mFraction = f;
        }

        @Override
        public void setAlpha(int i) {
            mPaint.setAlpha(i);
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            mPaint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        @Override
        public void draw(Canvas canvas) {
            int width = getMeasuredWidth();
            float translation = mFilmstripView.getTranslationX();
            if (translation == width) {
                return;
            }
            if (mHiding) {
                drawHiding(canvas);
            } else {
                drawShowing(canvas);
            }
        }

        private void drawHiding(Canvas canvas) {
            canvas.drawRect(mFilmstripView.getLeft() + mFilmstripView.getTranslationX(),
                    mFilmstripView.getTop() + mFilmstripView.getTranslationY(), getMeasuredWidth(),
                    getMeasuredHeight(), mPaint);
        }

        private void drawShowing(Canvas canvas) {
            int width = getMeasuredWidth();
            float translation = mFilmstripView.getTranslationX();
            if (translation == 0f) {
                canvas.drawRect(getBounds(), mPaint);
                return;
            }
            final float height = getMeasuredHeight();
            float x = width * (1.1f + mFraction * 0.9f);
            float y = height / 2f;
            float refX = width * (1 - mFraction);
            float refY = y * (1 - mFraction);
            canvas.drawCircle(x, getMeasuredHeight() / 2,
                    FloatMath.sqrt((x - refX) * (x - refX) + (y - refY) * (y - refY)), mPaint);
        }
    }
}
