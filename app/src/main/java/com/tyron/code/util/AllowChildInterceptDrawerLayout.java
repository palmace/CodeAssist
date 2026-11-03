package com.tyron.code.util;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;

/**
 * Allows horizontally scrolling child of drawer layouts to intercept the touch event,
 * while preserving the drawer's edge swipe area.
 */
public class AllowChildInterceptDrawerLayout extends DrawerLayout {

    private final Rect rect = new Rect();
    private final float edgeSize;

    public AllowChildInterceptDrawerLayout(@NonNull Context context) {
        this(context, null);
    }

    public AllowChildInterceptDrawerLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AllowChildInterceptDrawerLayout(@NonNull Context context,
                                           @Nullable AttributeSet attrs,
                                           int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // Default drawer edge is usually 20dp. We'll use 25dp to be safe.
        edgeSize = 25 * context.getResources().getDisplayMetrics().density;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // If the touch is near the edge, let DrawerLayout handle it (to allow opening by swipe)
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (ev.getX() < edgeSize) {
                return super.onInterceptTouchEvent(ev);
            }
        }

        // If a child can scroll horizontally at this position, don't intercept.
        View scrollingChild = findScrollingChild(this, ev.getX(), ev.getY());
        if (scrollingChild != null) {
            return false;
        }

        try {
            return super.onInterceptTouchEvent(ev);
        } catch (Exception e) {
            return false;
        }
    }

    private View findScrollingChild(ViewGroup parent, float x, float y) {
        int n = parent.getChildCount();
        for (int i = 0; i < n; i++) {
            View child = parent.getChildAt(i);
            if (child.getVisibility() != View.VISIBLE) {
                continue;
            }
            child.getHitRect(rect);
            if (rect.contains((int) x, (int) y)) {
                if (child.canScrollHorizontally(1) || child.canScrollHorizontally(-1)) {
                    return child;
                } else if (child instanceof ViewGroup) {
                    View v = findScrollingChild((ViewGroup) child, x - rect.left, y - rect.top);
                    if (v != null) {
                        return v;
                    }
                }
            }
        }
        return null;
    }
}
