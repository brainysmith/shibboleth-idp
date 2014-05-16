package com.identityblitz.shibboleth.idp.util;

import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;

public class DateTimeUtil {

    public static long getLifeTimeInMillis(DateTime expiryDate) {
        if (expiryDate == null) {
            throw new NullPointerException("expiryDate may not be null");
        }
        return toMillis(expiryDate) - System.currentTimeMillis();
    }

    public static long toMillis(DateTime dateTime) {
        if (dateTime == null) {
            throw new NullPointerException("dateTime may not be null");
        }
        return dateTime.toDateTime(ISOChronology.getInstanceUTC()).getMillis();
    }

}