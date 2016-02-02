package org.prevoz.android.util;

import android.content.Context;
import android.content.res.Resources;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.threeten.bp.LocalDate;

import static junit.framework.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class LocaleUtilTest {

    @Mock
    Resources resources;

    @Test
    public void testGetNotificationDayName() {
        String monday = LocaleUtil.getNotificationDayName(resources, LocalDate.of(1999, 1, 1));
        assertEquals("Ponedeljek", monday);
    }

}
