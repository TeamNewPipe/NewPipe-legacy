package org.schabi.newpipelegacy.player.event;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import androidx.appcompat.content.res.AppCompatResources;
import org.schabi.newpipelegacy.R;
import org.schabi.newpipelegacy.player.BasePlayer;
import org.schabi.newpipelegacy.player.MainPlayer;
import org.schabi.newpipelegacy.player.VideoPlayerImpl;
import org.schabi.newpipelegacy.player.helper.PlayerHelper;

import static org.schabi.newpipelegacy.player.BasePlayer.STATE_PLAYING;
import static org.schabi.newpipelegacy.player.VideoPlayer.DEFAULT_CONTROLS_DURATION;
import static org.schabi.newpipelegacy.player.VideoPlayer.DEFAULT_CONTROLS_HIDE_TIME;
import static org.schabi.newpipelegacy.util.AnimationUtils.Type.SCALE_AND_ALPHA;
import static org.schabi.newpipelegacy.util.AnimationUtils.animateView;

public class PlayerGestureListener
        extends GestureDetector.SimpleOnGestureListener
        implements View.OnTouchListener {
    private static final String TAG = ".PlayerGestureListener";
    private static final boolean DEBUG = BasePlayer.DEBUG;

    private final VideoPlayerImpl playerImpl;
    private final MainPlayer service;

    private int initialPopupX;
    private int initialPopupY;

    private boolean isMovingInMain;
    private boolean isMovingInPopup;

    private boolean isResizing;

    private final int tossFlingVelocity;

    private final boolean isVolumeGestureEnabled;
    private final boolean isBrightnessGestureEnabled;
    private final int maxVolume;
    private static final int MOVEMENT_THRESHOLD = 40;

    // [popup] initial coordinates and distance between fingers
    private double initPointerDistance = -1;
    private float initFirstPointerX = -1;
    private float initFirstPointerY = -1;
    private float initSecPointerX = -1;
    private float initSecPointerY = -1;


    public PlayerGestureListener(final VideoPlayerImpl playerImpl, final MainPlayer service) {
        this.playerImpl = playerImpl;
        this.service = service;
        this.tossFlingVelocity = PlayerHelper.getTossFlingVelocity(service);

        isVolumeGestureEnabled = PlayerHelper.isVolumeGestureEnabled(service);
        isBrightnessGestureEnabled = PlayerHelper.isBrightnessGestureEnabled(service);
        maxVolume = playerImpl.getAudioReactor().getMaxVolume();
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Helpers
    //////////////////////////////////////////////////////////////////////////*/

    /*
     * Main and popup players' gesture listeners is too different.
     * So it will be better to have different implementations of them
     * */
    @Override
    public boolean onDoubleTap(final MotionEvent e) {
        if (DEBUG) {
            Log.d(TAG, "onDoubleTap() called with: e = [" + e + "]" + "rawXy = "
                    + e.getRawX() + ", " + e.getRawY() + ", xy = " + e.getX() + ", " + e.getY());
        }

        if (playerImpl.popupPlayerSelected()) {
            return onDoubleTapInPopup(e);
        } else {
            return onDoubleTapInMain(e);
        }
    }

    @Override
    public boolean onSingleTapConfirmed(final MotionEvent e) {
        if (DEBUG) {
            Log.d(TAG, "onSingleTapConfirmed() called with: e = [" + e + "]");
        }

        if (playerImpl.popupPlayerSelected()) {
            return onSingleTapConfirmedInPopup(e);
        } else {
            return onSingleTapConfirmedInMain(e);
        }
    }

    @Override
    public boolean onDown(final MotionEvent e) {
        if (DEBUG) {
            Log.d(TAG, "onDown() called with: e = [" + e + "]");
        }

        if (playerImpl.popupPlayerSelected()) {
            return onDownInPopup(e);
        } else {
            return true;
        }
    }

    @Override
    public void onLongPress(final MotionEvent e) {
        if (DEBUG) {
            Log.d(TAG, "onLongPress() called with: e = [" + e + "]");
        }

        if (playerImpl.popupPlayerSelected()) {
            onLongPressInPopup(e);
        }
    }

    @Override
    public boolean onScroll(final MotionEvent initialEvent, final MotionEvent movingEvent,
                            final float distanceX, final float distanceY) {
        if (playerImpl.popupPlayerSelected()) {
            return onScrollInPopup(initialEvent, movingEvent, distanceX, distanceY);
        } else {
            return onScrollInMain(initialEvent, movingEvent, distanceX, distanceY);
        }
    }

    @Override
    public boolean onFling(final MotionEvent e1, final MotionEvent e2,
                           final float velocityX, final float velocityY) {
        if (DEBUG) {
            Log.d(TAG, "onFling() called with velocity: dX=["
                    + velocityX + "], dY=[" + velocityY + "]");
        }

        if (playerImpl.popupPlayerSelected()) {
            return onFlingInPopup(e1, e2, velocityX, velocityY);
        } else {
            return true;
        }
    }

    @Override
    public boolean onTouch(final View v, final MotionEvent event) {
        /*if (DEBUG && false) {
            Log.d(TAG, "onTouch() called with: v = [" + v + "], event = [" + event + "]");
        }*/

        if (playerImpl.popupPlayerSelected()) {
            return onTouchInPopup(v, event);
        } else {
            return onTouchInMain(v, event);
        }
    }


    /*//////////////////////////////////////////////////////////////////////////
    // Main player listener
    //////////////////////////////////////////////////////////////////////////*/

    private boolean onDoubleTapInMain(final MotionEvent e) {
        if (e.getX() > playerImpl.getRootView().getWidth() * 2.0 / 3.0) {
            playerImpl.onFastForward();
        } else if (e.getX() < playerImpl.getRootView().getWidth() / 3.0) {
            playerImpl.onFastRewind();
        } else {
            playerImpl.getPlayPauseButton().performClick();
        }

        return true;
    }


    private boolean onSingleTapConfirmedInMain(final MotionEvent e) {
        if (DEBUG) {
            Log.d(TAG, "onSingleTapConfirmed() called with: e = [" + e + "]");
        }

        if (playerImpl.getCurrentState() == BasePlayer.STATE_BLOCKED) {
            return true;
        }

        if (playerImpl.isControlsVisible()) {
            playerImpl.hideControls(150, 0);
        } else {
            if (playerImpl.getCurrentState() == BasePlayer.STATE_COMPLETED) {
                playerImpl.showControls(0);
            } else {
                playerImpl.showControlsThenHide();
            }
        }
        return true;
    }

    private boolean onScrollInMain(final MotionEvent initialEvent, final MotionEvent movingEvent,
                                   final float distanceX, final float distanceY) {
        if ((!isVolumeGestureEnabled && !isBrightnessGestureEnabled)
                || !playerImpl.isFullscreen()) {
            return false;
        }

        final boolean isTouchingStatusBar = initialEvent.getY() < getStatusBarHeight(service);
        final boolean isTouchingNavigationBar = initialEvent.getY()
                > playerImpl.getRootView().getHeight() - getNavigationBarHeight(service);
        if (isTouchingStatusBar || isTouchingNavigationBar) {
            return false;
        }

        /*if (DEBUG && false) Log.d(TAG, "onScrollInMain = " +
                ", e1.getRaw = [" + initialEvent.getRawX() + ", " + initialEvent.getRawY() + "]" +
                ", e2.getRaw = [" + movingEvent.getRawX() + ", " + movingEvent.getRawY() + "]" +
                ", distanceXy = [" + distanceX + ", " + distanceY + "]");*/

        final boolean insideThreshold =
                Math.abs(movingEvent.getY() - initialEvent.getY()) <= MOVEMENT_THRESHOLD;
        if (!isMovingInMain && (insideThreshold || Math.abs(distanceX) > Math.abs(distanceY))
                || playerImpl.getCurrentState() == BasePlayer.STATE_COMPLETED) {
            return false;
        }

        isMovingInMain = true;

        final boolean acceptAnyArea = isVolumeGestureEnabled != isBrightnessGestureEnabled;
        final boolean acceptVolumeArea = acceptAnyArea
                || initialEvent.getX() > playerImpl.getRootView().getWidth() / 2.0;

        if (isVolumeGestureEnabled && acceptVolumeArea) {
            playerImpl.getVolumeProgressBar().incrementProgressBy((int) distanceY);
            final float currentProgressPercent = (float) playerImpl
                    .getVolumeProgressBar().getProgress() / playerImpl.getMaxGestureLength();
            final int currentVolume = (int) (maxVolume * currentProgressPercent);
            playerImpl.getAudioReactor().setVolume(currentVolume);

            if (DEBUG) {
                Log.d(TAG, "onScroll().volumeControl, currentVolume = " + currentVolume);
            }

            playerImpl.getVolumeImageView().setImageDrawable(
                    AppCompatResources.getDrawable(service, currentProgressPercent <= 0
                            ? R.drawable.ic_volume_off_white_24dp
                            : currentProgressPercent < 0.25 ? R.drawable.ic_volume_mute_white_24dp
                            : currentProgressPercent < 0.75 ? R.drawable.ic_volume_down_white_24dp
                            : R.drawable.ic_volume_up_white_24dp)
            );

            if (playerImpl.getVolumeRelativeLayout().getVisibility() != View.VISIBLE) {
                animateView(playerImpl.getVolumeRelativeLayout(), SCALE_AND_ALPHA, true, 200);
            }
            if (playerImpl.getBrightnessRelativeLayout().getVisibility() == View.VISIBLE) {
                playerImpl.getBrightnessRelativeLayout().setVisibility(View.GONE);
            }
        } else {
            final Activity parent = playerImpl.getParentActivity();
            if (parent == null) {
                return true;
            }

            final Window window = parent.getWindow();
            final WindowManager.LayoutParams layoutParams = window.getAttributes();
            final ProgressBar bar = playerImpl.getBrightnessProgressBar();
            final float oldBrightness = layoutParams.screenBrightness;
            bar.setProgress((int) (bar.getMax() * Math.max(0, Math.min(1, oldBrightness))));
            bar.incrementProgressBy((int) distanceY);

            final float currentProgressPercent = (float) bar.getProgress() / bar.getMax();
            layoutParams.screenBrightness = currentProgressPercent;
            window.setAttributes(layoutParams);

            // Save current brightness level
            PlayerHelper.setScreenBrightness(parent, currentProgressPercent);

            if (DEBUG) {
                Log.d(TAG, "onScroll().brightnessControl, "
                        + "currentBrightness = " + currentProgressPercent);
            }

            playerImpl.getBrightnessImageView().setImageDrawable(
                    AppCompatResources.getDrawable(service,
                            currentProgressPercent < 0.25
                                    ? R.drawable.ic_brightness_low_white_24dp
                                    : currentProgressPercent < 0.75
                                    ? R.drawable.ic_brightness_medium_white_24dp
                                    : R.drawable.ic_brightness_high_white_24dp)
            );

            if (playerImpl.getBrightnessRelativeLayout().getVisibility() != View.VISIBLE) {
                animateView(playerImpl.getBrightnessRelativeLayout(), SCALE_AND_ALPHA, true, 200);
            }
            if (playerImpl.getVolumeRelativeLayout().getVisibility() == View.VISIBLE) {
                playerImpl.getVolumeRelativeLayout().setVisibility(View.GONE);
            }
        }
        return true;
    }

    private void onScrollEndInMain() {
        if (DEBUG) {
            Log.d(TAG, "onScrollEnd() called");
        }

        if (playerImpl.getVolumeRelativeLayout().getVisibility() == View.VISIBLE) {
            animateView(playerImpl.getVolumeRelativeLayout(), SCALE_AND_ALPHA, false, 200, 200);
        }
        if (playerImpl.getBrightnessRelativeLayout().getVisibility() == View.VISIBLE) {
            animateView(playerImpl.getBrightnessRelativeLayout(), SCALE_AND_ALPHA, false, 200, 200);
        }

        if (playerImpl.isControlsVisible() && playerImpl.getCurrentState() == STATE_PLAYING) {
            playerImpl.hideControls(DEFAULT_CONTROLS_DURATION, DEFAULT_CONTROLS_HIDE_TIME);
        }
    }

    private boolean onTouchInMain(final View v, final MotionEvent event) {
        playerImpl.getGestureDetector().onTouchEvent(event);
        if (event.getAction() == MotionEvent.ACTION_UP && isMovingInMain) {
            isMovingInMain = false;
            onScrollEndInMain();
        }
        // This hack allows to stop receiving touch events on appbar
        // while touching video player's view
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                v.getParent().requestDisallowInterceptTouchEvent(playerImpl.isFullscreen());
                return true;
            case MotionEvent.ACTION_UP:
                v.getParent().requestDisallowInterceptTouchEvent(false);
                return false;
            default:
                return true;
        }
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Popup player listener
    //////////////////////////////////////////////////////////////////////////*/

    private boolean onDoubleTapInPopup(final MotionEvent e) {
        if (playerImpl == null || !playerImpl.isPlaying()) {
            return false;
        }

        playerImpl.hideControls(0, 0);

        if (e.getX() > playerImpl.getPopupWidth() / 2) {
            playerImpl.onFastForward();
        } else {
            playerImpl.onFastRewind();
        }

        return true;
    }

    private boolean onSingleTapConfirmedInPopup(final MotionEvent e) {
        if (playerImpl == null || playerImpl.getPlayer() == null) {
            return false;
        }
        if (playerImpl.isControlsVisible()) {
            playerImpl.hideControls(100, 100);
        } else {
            playerImpl.getPlayPauseButton().requestFocus();
            playerImpl.showControlsThenHide();
        }
        return true;
    }

    private boolean onDownInPopup(final MotionEvent e) {
        // Fix popup position when the user touch it, it may have the wrong one
        // because the soft input is visible (the draggable area is currently resized).
        playerImpl.updateScreenSize();
        playerImpl.checkPopupPositionBounds();

        initialPopupX = playerImpl.getPopupLayoutParams().x;
        initialPopupY = playerImpl.getPopupLayoutParams().y;
        playerImpl.setPopupWidth(playerImpl.getPopupLayoutParams().width);
        playerImpl.setPopupHeight(playerImpl.getPopupLayoutParams().height);
        return super.onDown(e);
    }

    private void onLongPressInPopup(final MotionEvent e) {
        playerImpl.updateScreenSize();
        playerImpl.checkPopupPositionBounds();
        playerImpl.updatePopupSize((int) playerImpl.getScreenWidth(), -1);
    }

    private boolean onScrollInPopup(final MotionEvent initialEvent,
                                    final MotionEvent movingEvent,
                                    final float distanceX,
                                    final float distanceY) {
        if (isResizing || playerImpl == null) {
            return super.onScroll(initialEvent, movingEvent, distanceX, distanceY);
        }

        if (!isMovingInPopup) {
            animateView(playerImpl.getCloseOverlayButton(), true, 200);
        }

        isMovingInPopup = true;

        final float diffX = (int) (movingEvent.getRawX() - initialEvent.getRawX());
        float posX = (int) (initialPopupX + diffX);
        final float diffY = (int) (movingEvent.getRawY() - initialEvent.getRawY());
        float posY = (int) (initialPopupY + diffY);

        if (posX > (playerImpl.getScreenWidth() - playerImpl.getPopupWidth())) {
            posX = (int) (playerImpl.getScreenWidth() - playerImpl.getPopupWidth());
        } else if (posX < 0) {
            posX = 0;
        }

        if (posY > (playerImpl.getScreenHeight() - playerImpl.getPopupHeight())) {
            posY = (int) (playerImpl.getScreenHeight() - playerImpl.getPopupHeight());
        } else if (posY < 0) {
            posY = 0;
        }

        playerImpl.getPopupLayoutParams().x = (int) posX;
        playerImpl.getPopupLayoutParams().y = (int) posY;

        final View closingOverlayView = playerImpl.getClosingOverlayView();
        if (playerImpl.isInsideClosingRadius(movingEvent)) {
            if (closingOverlayView.getVisibility() == View.GONE) {
                animateView(closingOverlayView, true, 250);
            }
        } else {
            if (closingOverlayView.getVisibility() == View.VISIBLE) {
                animateView(closingOverlayView, false, 0);
            }
        }

//            if (DEBUG) {
//                Log.d(TAG, "onScrollInPopup = "
//                        + "e1.getRaw = [" + initialEvent.getRawX() + ", "
//                        + initialEvent.getRawY() + "], "
//                        + "e1.getX,Y = [" + initialEvent.getX() + ", "
//                        + initialEvent.getY() + "], "
//                        + "e2.getRaw = [" + movingEvent.getRawX() + ", "
//                        + movingEvent.getRawY() + "], "
//                        + "e2.getX,Y = [" + movingEvent.getX() + ", " + movingEvent.getY() + "], "
//                        + "distanceX,Y = [" + distanceX + ", " + distanceY + "], "
//                        + "posX,Y = [" + posX + ", " + posY + "], "
//                        + "popupW,H = [" + popupWidth + " x " + popupHeight + "]");
//            }
        playerImpl.windowManager
                .updateViewLayout(playerImpl.getRootView(), playerImpl.getPopupLayoutParams());
        return true;
    }

    private void onScrollEndInPopup(final MotionEvent event) {
        if (playerImpl == null) {
            return;
        }
        if (playerImpl.isControlsVisible() && playerImpl.getCurrentState() == STATE_PLAYING) {
            playerImpl.hideControls(DEFAULT_CONTROLS_DURATION, DEFAULT_CONTROLS_HIDE_TIME);
        }

        if (playerImpl.isInsideClosingRadius(event)) {
            playerImpl.closePopup();
        } else {
            animateView(playerImpl.getClosingOverlayView(), false, 0);

            if (!playerImpl.isPopupClosing) {
                animateView(playerImpl.getCloseOverlayButton(), false, 200);
            }
        }
    }

    private boolean onFlingInPopup(final MotionEvent e1,
                                   final MotionEvent e2,
                                   final float velocityX,
                                   final float velocityY) {
        if (playerImpl == null) {
            return false;
        }

        final float absVelocityX = Math.abs(velocityX);
        final float absVelocityY = Math.abs(velocityY);
        if (Math.max(absVelocityX, absVelocityY) > tossFlingVelocity) {
            if (absVelocityX > tossFlingVelocity) {
                playerImpl.getPopupLayoutParams().x = (int) velocityX;
            }
            if (absVelocityY > tossFlingVelocity) {
                playerImpl.getPopupLayoutParams().y = (int) velocityY;
            }
            playerImpl.checkPopupPositionBounds();
            playerImpl.windowManager
                    .updateViewLayout(playerImpl.getRootView(), playerImpl.getPopupLayoutParams());
            return true;
        }
        return false;
    }

    private boolean onTouchInPopup(final View v, final MotionEvent event) {
        if (playerImpl == null) {
            return false;
        }
        playerImpl.getGestureDetector().onTouchEvent(event);

        if (event.getPointerCount() == 2 && !isMovingInPopup && !isResizing) {
            if (DEBUG) {
                Log.d(TAG, "onTouch() 2 finger pointer detected, enabling resizing.");
            }
            playerImpl.showAndAnimateControl(-1, true);
            playerImpl.getLoadingPanel().setVisibility(View.GONE);

            playerImpl.hideControls(0, 0);
            animateView(playerImpl.getCurrentDisplaySeek(), false, 0, 0);
            animateView(playerImpl.getResizingIndicator(), true, 200, 0);
            //record coordinates of fingers
            initFirstPointerX = event.getX(0);
            initFirstPointerY = event.getY(0);
            initSecPointerX = event.getX(1);
            initSecPointerY = event.getY(1);
            //record distance between fingers
            initPointerDistance = Math.hypot(initFirstPointerX - initSecPointerX,
                    initFirstPointerY - initSecPointerY);

            isResizing = true;
        }

        if (event.getAction() == MotionEvent.ACTION_MOVE && !isMovingInPopup && isResizing) {
            if (DEBUG) {
                Log.d(TAG, "onTouch() ACTION_MOVE > v = [" + v + "],  "
                        + "e1.getRaw = [" + event.getRawX() + ", " + event.getRawY() + "]");
            }
            return handleMultiDrag(event);
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (DEBUG) {
                Log.d(TAG, "onTouch() ACTION_UP > v = [" + v + "],  "
                        + "e1.getRaw = [" + event.getRawX() + ", " + event.getRawY() + "]");
            }
            if (isMovingInPopup) {
                isMovingInPopup = false;
                onScrollEndInPopup(event);
            }

            if (isResizing) {
                isResizing = false;

                initPointerDistance = -1;
                initFirstPointerX = -1;
                initFirstPointerY = -1;
                initSecPointerX = -1;
                initSecPointerY = -1;

                animateView(playerImpl.getResizingIndicator(), false, 100, 0);
                playerImpl.changeState(playerImpl.getCurrentState());
            }

            if (!playerImpl.isPopupClosing) {
                playerImpl.savePositionAndSize();
            }
        }

        v.performClick();
        return true;
    }

    private boolean handleMultiDrag(final MotionEvent event) {
        if (initPointerDistance != -1 && event.getPointerCount() == 2) {
            // get the movements of the fingers
            final double firstPointerMove = Math.hypot(event.getX(0) - initFirstPointerX,
                    event.getY(0) - initFirstPointerY);
            final double secPointerMove = Math.hypot(event.getX(1) - initSecPointerX,
                    event.getY(1) - initSecPointerY);

            // minimum threshold beyond which pinch gesture will work
            final int minimumMove = ViewConfiguration.get(service).getScaledTouchSlop();

            if (Math.max(firstPointerMove, secPointerMove) > minimumMove) {
                // calculate current distance between the pointers
                final double currentPointerDistance =
                        Math.hypot(event.getX(0) - event.getX(1),
                                event.getY(0) - event.getY(1));

                final double popupWidth = playerImpl.getPopupWidth();
                // change co-ordinates of popup so the center stays at the same position
                final double newWidth = (popupWidth * currentPointerDistance / initPointerDistance);
                initPointerDistance = currentPointerDistance;
                playerImpl.getPopupLayoutParams().x += (popupWidth - newWidth) / 2;

                playerImpl.checkPopupPositionBounds();
                playerImpl.updateScreenSize();

                playerImpl.updatePopupSize(
                        (int) Math.min(playerImpl.getScreenWidth(), newWidth),
                        -1);
                return true;
            }
        }
        return false;
    }


    /*
     * Utils
     * */

    private int getNavigationBarHeight(final Context context) {
        final int resId = context.getResources()
                .getIdentifier("navigation_bar_height", "dimen", "android");
        if (resId > 0) {
            return context.getResources().getDimensionPixelSize(resId);
        }
        return 0;
    }

    private int getStatusBarHeight(final Context context) {
        final int resId = context.getResources()
                .getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) {
            return context.getResources().getDimensionPixelSize(resId);
        }
        return 0;
    }
}


