package org.schabi.newpipelegacy.report;

import android.app.Activity;

import org.junit.Test;
import org.schabi.newpipelegacy.MainActivity;
import org.schabi.newpipelegacy.RouterActivity;
import org.schabi.newpipelegacy.fragments.detail.VideoDetailFragment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for {@link ErrorActivity}
 */
public class ErrorActivityTest {
    @Test
    public void getReturnActivity() {
        Class<? extends Activity> returnActivity;
        returnActivity = ErrorActivity.getReturnActivity(MainActivity.class);
        assertEquals(MainActivity.class, returnActivity);

        returnActivity = ErrorActivity.getReturnActivity(RouterActivity.class);
        assertEquals(RouterActivity.class, returnActivity);

        returnActivity = ErrorActivity.getReturnActivity(null);
        assertNull(returnActivity);

        returnActivity = ErrorActivity.getReturnActivity(Integer.class);
        assertEquals(MainActivity.class, returnActivity);

        returnActivity = ErrorActivity.getReturnActivity(VideoDetailFragment.class);
        assertEquals(MainActivity.class, returnActivity);
    }



}