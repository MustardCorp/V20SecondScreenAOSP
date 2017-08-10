package xyz.mustardcorp.secondscreen.custom;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.Display;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;

import com.mcs.viewpager.OrientationViewPager;

import static android.view.View.OVER_SCROLL_NEVER;

/**
 * Mostly unnecessary class at the moment
 */

public class CustomViewPager extends OrientationViewPager
{
    public static final int ORIENTATION_HORIZONTAL = 0;
    public static final int ORIENTATION_VERTICAL = 1;

    private Display display;
    private int mOrientation = ORIENTATION_HORIZONTAL;

    public CustomViewPager(Context context) {
        super(context);
        setOffscreenPageLimit(10000000);
        init();
    }

    public CustomViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setOrientation(int orientation) {
        mOrientation = orientation;

        super.setOrientation(orientation);

//        if (mOrientation == ORIENTATION_VERTICAL) setPageTransformer(true, new VerticalPageTransformer());
//        else setPageTransformer(false, null);
    }

    public int getOrientation() {
        return mOrientation;
    }

    private void init() {
        display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        setOverScrollMode(OVER_SCROLL_NEVER);
    }

//    private class VerticalPageTransformer implements ViewPager.PageTransformer {
//
//        @Override
//        public void transformPage(View view, float position) {
////            // Counteract the default slide transition
////            view.setTranslationX(view.getWidth() * -position);
////
////            //set Y position to swipe in from top
////            float yPosition = position * view.getHeight();
////            view.setTranslationY(yPosition);
//        }
//    }

//    /**
//     * Swaps the X and Y coordinates of your touch event.
//     */
//    private MotionEvent swapXY(MotionEvent ev) {
////        if (mOrientation == ORIENTATION_VERTICAL && (ev.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_MOVE)
////        {
////            float width = getWidth();
////            float height = getHeight();
////
////            float newX = ev.getY() * width / height;
////            float newY = ev.getX() * height / width;
////
////            ev.setLocation(newX, newY);
////        }
//
//        return ev;
//    }

//    @Override
//    public boolean onInterceptTouchEvent(MotionEvent ev){
////        if ((ev.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_MOVE) {
////            boolean intercepted = super.onInterceptTouchEvent(swapXY(ev));
////            swapXY(ev); // return touch coordinates to original reference frame for any child views
////            return intercepted;
////        } else {
////            return super.onInterceptTouchEvent(ev);
////        }
//        return super.onInterceptTouchEvent(ev);
//    }
//
//    @Override
//    public boolean onTouchEvent(MotionEvent ev) {
////        if ((ev.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_MOVE) {
////            return super.onTouchEvent(swapXY(ev));
////        } else if ((ev.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
////            performClick();
////        }
////
////        return super.onTouchEvent(ev);
//        return super.onTouchEvent(ev);
//    }
//
//    @Override
//    public boolean performClick()
//    {
//        return super.performClick();
//    }
}
