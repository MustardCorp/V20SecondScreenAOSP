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

public class CustomViewPager extends ViewPager
{

    private Display display;

    public CustomViewPager(Context context) {
        super(context);
        init();
    }

    public CustomViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        boolean isLandscape = display.getRotation() == Surface.ROTATION_90 || display.getRotation() == Surface.ROTATION_270;
        // The majority of the magic happens here
//        if (isLandscape) {
//            setPageTransformer(true, new VerticalPageTransformer());
//            setLayoutDirection(ViewPager.LAYOUT_DIRECTION_RTL);
//        }
        // The easiest way to get rid of the overscroll drawing that happens on the left and right
        setOverScrollMode(OVER_SCROLL_NEVER);
        setOrientationCorrections();
    }

    private void setOrientationCorrections() {
        OrientationEventListener listener = new OrientationEventListener(getContext())
        {
            @Override
            public void onOrientationChanged(int i)
            {
                Display display = ((WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

                switch (display.getRotation()) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
//                        setPageTransformer(true, null);
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
//                        setPageTransformer(true, new VerticalPageTransformer());
//                        setLayoutDirection(ViewPager.LAYOUT_DIRECTION_RTL);
                        break;
                }
            }
        };
        listener.enable();
    }

    private class VerticalPageTransformer implements ViewPager.PageTransformer {

        @Override
        public void transformPage(View view, float position) {
            if (position < -1)
            { // [-Infinity,-1)
                // This page is way off-screen to the left.
                view.setAlpha(0);

            } else if (position <= 1)
            { // [-1,1]
                view.setAlpha(1);

                // Counteract the default slide transition
                view.setTranslationX(view.getWidth() * -position);

                //set Y position to swipe in from top
                float yPosition = position * view.getHeight();
                view.setTranslationY(yPosition);

            } else
            { // (1,+Infinity]
                // This page is way off-screen to the right.
                view.setAlpha(0);
            }
        }
    }

//    /**
//     * Swaps the X and Y coordinates of your touch event.
//     */
//    private MotionEvent swapXY(MotionEvent ev) {
//
//        if (display.getRotation() == Surface.ROTATION_90 || display.getRotation() == Surface.ROTATION_270)
//        {
//            float width = getWidth();
//            float height = getHeight();
//
//            float newX = ev.getY() * width / height;
//            float newY = ev.getX() * height / width;
//
//            ev.setLocation(newX, newY);
////            ev.setLocation(ev.getY(), ev.getX());
//        }
//
//        return ev;
//    }
//
//    @Override
//    public boolean onInterceptTouchEvent(MotionEvent ev){
//        boolean intercepted = super.onInterceptTouchEvent(swapXY(ev));
//        swapXY(ev); // return touch coordinates to original reference frame for any child views
//        return intercepted;
//    }
//
//    @Override
//    public boolean onTouchEvent(MotionEvent ev) {
//        return super.onTouchEvent(swapXY(ev));
//    }
}
