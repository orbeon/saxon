package org.orbeon.saxon.value;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.functions.CurrentDateTime;
import org.orbeon.saxon.trans.XPathException;

import java.util.GregorianCalendar;



/**
* Abstract superclass for Date, Time, and DateTime
*/

// TODO: there is probably scope for more re-use between these three classes

public abstract class CalendarValue extends AtomicValue implements Comparable {

    // calendar and zoneSpecified together represent the value space
    protected GregorianCalendar calendar;
    protected boolean zoneSpecified;

    public abstract CalendarValue add(DurationValue duration) throws XPathException;

    /**
     * Determine the difference between two points in time, as a duration
     * @param other the other point in time
     * @param context the dynamic context, used to obtain timezone information. May be set to null
     * only if both values contain an explicit timezone.
     * @return the duration as an xdt:dayTimeDuration
     * @throws net.sf.saxon.trans.XPathException for example if one value is a date and the other is a time
     */

    public SecondsDurationValue subtract(CalendarValue other, XPathContext context) throws XPathException {
        CalendarValue v1 =
                (zoneSpecified ? this : setTimezone(CurrentDateTime.getImplicitTimezone(context)));
        CalendarValue v2 =
                (other.zoneSpecified ? other : other.setTimezone(CurrentDateTime.getImplicitTimezone(context)));
        long t1 = v1.calendar.getTimeInMillis();
        long t2 = v2.calendar.getTimeInMillis();
        long diff = (t1 - t2);
        return SecondsDurationValue.fromMilliseconds(diff);
    }

    /**
     * Return a date, time, or dateTime with the same localized value, but
     * without the timezone component
     * @return the result of removing the timezone
     * @throws XPathException
     */

    public abstract CalendarValue removeTimezone() throws XPathException;

    /**
     * Return a date, time, or dateTime with the same normalized value, but
     * in a different timezone
     * @return the date/time in the new timezone
     * @throws XPathException
     */

    public abstract CalendarValue setTimezone(SecondsDurationValue tz) throws XPathException;
}

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//

