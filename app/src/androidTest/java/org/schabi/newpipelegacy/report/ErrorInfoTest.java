package org.schabi.newpipelegacy.report;

import android.os.Parcel;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.schabi.newpipelegacy.R;
import org.schabi.newpipelegacy.report.ErrorActivity.ErrorInfo;

import static org.junit.Assert.assertEquals;

/**
 * Instrumented tests for {@link ErrorInfo}.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ErrorInfoTest {

    @Test
    public void errorInfoTestParcelable() {
        final ErrorInfo info = ErrorInfo.make(UserAction.USER_REPORT, "youtube", "request",
                R.string.general_error);
        // Obtain a Parcel object and write the parcelable object to it:
        final Parcel parcel = Parcel.obtain();
        info.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        final ErrorInfo infoFromParcel = ErrorInfo.CREATOR.createFromParcel(parcel);

        assertEquals(UserAction.USER_REPORT, infoFromParcel.userAction);
        assertEquals("youtube", infoFromParcel.serviceName);
        assertEquals("request", infoFromParcel.request);
        assertEquals(R.string.general_error, infoFromParcel.message);

        parcel.recycle();
    }
}
