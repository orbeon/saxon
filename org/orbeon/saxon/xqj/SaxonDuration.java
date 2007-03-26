package org.orbeon.saxon.xqj;

import org.orbeon.saxon.value.*;
import org.orbeon.saxon.functions.Component;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.om.NamespaceConstant;

import javax.xml.datatype.Duration;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.namespace.QName;
import java.util.Calendar;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Saxon implementation of the JAXP class javax.xml.datatype.Duration. This is currently used only by the XQJ
 * interface for XQuery: the normal representation of a duration in Saxon is the class {@link DurationValue}.
 * <p>
 * The JAXP specification for this class defines it in terms of XML Schema 1.0 semantics. This defines a structure
 * with six independent components (year, month, day, hour, minute, second). This implementation is more aligned
 * to the XPath 2.0 semantics of the data type, which essentially defines duration as an integer number of months plus
 * a decimal number of seconds.
 */
public class SaxonDuration extends Duration {

    private DurationValue duration;

    /**
     * Create a SaxonDuration that wraps a supplied DurationValue
     * @param duration the value to be wrapped.
     */

    public SaxonDuration(DurationValue duration) {
        this.duration = duration;
    }

    /**
     * Get the underlying DurationValue
     */

    public DurationValue getDurationValue() {
        return duration;
    }

    /**
     * Get the type of this duration, as one of the values xs:duration, xs:dayTimeDuration, or
     * xs:yearMonthDuration. (Note that the XML Schema namespace URI is used, whereas the current
     * implementation of the superclass uses a provisional URI allocated in a 2003 W3C working draft)
     * @return the type of this duration, as one of the values xs:duration, xs:dayTimeDuration, or
     * xs:yearMonthDuration
     */

    public QName getXMLSchemaType() {
        if (duration instanceof SecondsDurationValue) {
            return new QName(NamespaceConstant.SCHEMA, "dayTimeDuration");
        } else if (duration instanceof MonthDurationValue) {
            return new QName(NamespaceConstant.SCHEMA, "yearMonthDuration");
        } else {
            return new QName(NamespaceConstant.SCHEMA, "duration");
        }
    }

    /**
     * Returns the sign of this duration in -1,0, or 1.
     *
     * @return -1 if this duration is negative, 0 if the duration is zero,
     *         and 1 if the duration is positive.
     */
    public int getSign() {
        return duration.signum();
    }

    /**
     * Gets the value of a field.
     * <p/>
     * Fields of a duration object may contain arbitrary large value.
     * Therefore this method is designed to return a {@link Number} object.
     * <p/>
     * In case of YEARS, MONTHS, DAYS, HOURS, and MINUTES, the returned
     * number will be a non-negative integer. In case of seconds,
     * the returned number may be a non-negative decimal value.
     *
     * @param field one of the six Field constants (YEARS,MONTHS,DAYS,HOURS,
     *              MINUTES, or SECONDS.)
     * @return If the specified field is present, this method returns
     *         a non-null non-negative {@link Number} object that
     *         represents its value. If it is not present, return null.
     *         For YEARS, MONTHS, DAYS, HOURS, and MINUTES, this method
     *         returns a {@link java.math.BigInteger} object. For SECONDS, this
     *         method returns a {@link java.math.BigDecimal}.
     * @throws NullPointerException If the <code>field</code> is <code>null</code>.
     */
    public Number getField(DatatypeConstants.Field field) {
        try {
            if (field == DatatypeConstants.YEARS) {
                return BigInteger.valueOf(((IntegerValue)duration.getComponent(Component.YEAR)).longValue());
            } else if (field == DatatypeConstants.MONTHS) {
                return BigInteger.valueOf(((IntegerValue)duration.getComponent(Component.MONTH)).longValue());
            } else if (field == DatatypeConstants.DAYS) {
                return BigInteger.valueOf(((IntegerValue)duration.getComponent(Component.DAY)).longValue());
            } else if (field == DatatypeConstants.HOURS) {
                return BigInteger.valueOf(((IntegerValue)duration.getComponent(Component.HOURS)).longValue());
            } else if (field == DatatypeConstants.MINUTES) {
                return BigInteger.valueOf(((IntegerValue)duration.getComponent(Component.MINUTES)).longValue());
            } else if (field == DatatypeConstants.SECONDS) {
                return (((DecimalValue)duration.getComponent(Component.SECONDS)).getValue());
            } else {
                throw new IllegalArgumentException("Invalid field");
            }
        } catch (XPathException e) {
            throw new AssertionError("Component extraction on duration failed");
        }
    }

    /**
     * Checks if a field is set. In this implementation, all fields are always set.
     * @param field one of the six Field constants (YEARS,MONTHS,DAYS,HOURS,
     *              MINUTES, or SECONDS.)
     * @return This implementation always returns true.
     */
    public boolean isSet(DatatypeConstants.Field field) {
        return true;
    }

    /**
     * <p>Computes a new duration whose value is <code>this+rhs</code>.</p>
     * <p>This implementation follows the XPath semantics. This means that the operation will fail
     * if the duration is not a yearMonthDuration or a dayTimeDuration.
     * @param rhs <code>Duration</code> to add to this <code>Duration</code>
     * @return non-null valid Duration object.
     * @throws NullPointerException  If the rhs parameter is null.
     * @throws IllegalStateException If the durations are not both dayTimeDurations, or
     * both yearMonthDurations.
     * @see #subtract(javax.xml.datatype.Duration)
     */
    public Duration add(Duration rhs) {
        try {
            return new SaxonDuration(duration.add(((SaxonDuration)rhs).duration));
        } catch (XPathException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    /**
     * <p>Computes a new duration whose value is <code>this-rhs</code>.</p>
     * <p>This implementation follows the XPath semantics. This means that the operation will fail
     * if the duration is not a yearMonthDuration or a dayTimeDuration.
     * @param rhs <code>Duration</code> to subtract from this <code>Duration</code>
     * @return non-null valid Duration object.
     * @throws NullPointerException  If the rhs parameter is null.
     * @throws IllegalStateException If the durations are not both dayTimeDurations, or
     * both yearMonthDurations.
     * @see #add(javax.xml.datatype.Duration)
     */
    public Duration subtract(Duration rhs) {
        try {
            return new SaxonDuration(duration.subtract(((SaxonDuration)rhs).duration));
        } catch (XPathException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    /**
     * Adds this duration to a {@link java.util.Calendar} object.
     * <p/>
     * <p/>
     * Calls {@link java.util.Calendar#add(int,int)} in the
     * order of YEARS, MONTHS, DAYS, HOURS, MINUTES, SECONDS, and MILLISECONDS
     * if those fields are present. Because the {@link java.util.Calendar} class
     * uses int to hold values, there are cases where this method
     * won't work correctly (for example if values of fields
     * exceed the range of int.)
     * </p>
     * <p/>
     * <p/>
     * Also, since this duration class is a Gregorian duration, this
     * method will not work correctly if the given {@link java.util.Calendar}
     * object is based on some other calendar systems.
     * </p>
     * <p/>
     * <p/>
     * Any fractional parts of this <code>Duration</code> object
     * beyond milliseconds will be simply ignored. For example, if
     * this duration is "P1.23456S", then 1 is added to SECONDS,
     * 234 is added to MILLISECONDS, and the rest will be unused.
     * </p>
     * <p/>
     * <p/>
     * Note that because {@link java.util.Calendar#add(int, int)} is using
     * <tt>int</tt>, <code>Duration</code> with values beyond the
     * range of <tt>int</tt> in its fields
     * will cause overflow/underflow to the given {@link java.util.Calendar}.
     * {@link javax.xml.datatype.XMLGregorianCalendar#add(javax.xml.datatype.Duration)} provides the same
     * basic operation as this method while avoiding
     * the overflow/underflow issues.
     *
     * @param calendar A calendar object whose value will be modified.
     * @throws NullPointerException if the calendar parameter is null.
     */
    public void addTo(Calendar calendar) {
        int sign = getSign();
        if (sign == 0) {
            return;
        }
        try {
            calendar.add(getYears()*sign, Calendar.YEAR);
            calendar.add(getMonths()*sign, Calendar.MONTH);
            calendar.add(getDays()*sign, Calendar.DAY_OF_MONTH);
            calendar.add(getHours()*sign, Calendar.HOUR_OF_DAY);
            calendar.add(getMinutes()*sign, Calendar.MINUTE);
            calendar.add((int)((IntegerValue)duration.getComponent(Component.WHOLE_SECONDS)).longValue()*sign, Calendar.SECOND);
            calendar.add((int)((IntegerValue)duration.getComponent(Component.MICROSECONDS)).longValue()*sign/1000, Calendar.MILLISECOND);
        } catch (XPathException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    /**
     * Computes a new duration whose value is <code>factor</code> times
     * longer than the value of this duration.
     * <p/>
     * This implementation follows the XPath semantics. This means that it is defined
     * only on yearMonthDuration and dayTimeDuration. Other cases produce an IllegalStateException.
     *
     * @param factor to multiply by
     * @return returns a non-null valid <code>Duration</code> object
     * @throws IllegalStateException if operation produces fraction in
     *                               the months field.
     * @throws NullPointerException  if the <code>factor</code> parameter is
     *                               <code>null</code>.
     */
    public Duration multiply(BigDecimal factor) {
        try {
            return new SaxonDuration(duration.multiply(factor.doubleValue()));
        } catch (XPathException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    /**
     * Returns a new <code>Duration</code> object whose
     * value is <code>-this</code>.
     * <p/>
     * <p/>
     * Since the <code>Duration</code> class is immutable, this method
     * doesn't change the value of this object. It simply computes
     * a new Duration object and returns it.
     *
     * @return always return a non-null valid <code>Duration</code> object.
     */
    public Duration negate() {
        return new SaxonDuration(duration.negate());
    }

    /**
     * <p>Converts the years and months fields into the days field
     * by using a specific time instant as the reference point.</p>
     * <p/>
     * This implementation does not support this method
     * @param startTimeInstant <code>Calendar</code> reference point.
     * @return <code>Duration</code> of years and months of this <code>Duration</code> as days.
     * @throws NullPointerException If the startTimeInstant parameter is null.
     * @throws UnsupportedOperationException Always thrown by this implementation.
     */
    public Duration normalizeWith(Calendar startTimeInstant) {
        throw new UnsupportedOperationException();  // TODO: implement this
    }

    /**
     * <p>Partial order relation comparison with this <code>Duration</code> instance.</p>
     * <p>This implementation follows the XPath semantics. This means that the result is defined only
     * for dayTimeDuration and yearMonthDuration values, and the result is never indeterminate.
     * <p/>
     * <p>Return:</p>
     * <ul>
     * <li>{@link javax.xml.datatype.DatatypeConstants#LESSER} if this <code>Duration</code> is shorter than <code>duration</code> parameter</li>
     * <li>{@link javax.xml.datatype.DatatypeConstants#EQUAL} if this <code>Duration</code> is equal to <code>duration</code> parameter</li>
     * <li>{@link javax.xml.datatype.DatatypeConstants#GREATER} if this <code>Duration</code> is longer than <code>duration</code> parameter</li>
     * <li>{@link javax.xml.datatype.DatatypeConstants#INDETERMINATE} if a conclusive partial order relation cannot be determined</li>
     * </ul>
     *
     * @param rhs duration to compare
     * @return the relationship between <code>this</code> <code>Duration</code>and <code>duration</code> parameter as
     *         {@link javax.xml.datatype.DatatypeConstants#LESSER}, {@link javax.xml.datatype.DatatypeConstants#EQUAL}, {@link javax.xml.datatype.DatatypeConstants#GREATER}
     *         or {@link javax.xml.datatype.DatatypeConstants#INDETERMINATE}.
     * @throws UnsupportedOperationException If the underlying implementation
     *                                       cannot reasonably process the request, e.g. W3C XML Schema allows for
     *                                       arbitrarily large/small/precise values, the request may be beyond the
     *                                       implementations capability.
     * @throws NullPointerException          if <code>duration</code> is <code>null</code>.
     * @throws IllegalArgumentException if the operands are not dayTimeDuration or yearMonthDuration values.
     * @see #isShorterThan(javax.xml.datatype.Duration)
     * @see #isLongerThan(javax.xml.datatype.Duration)
     */
    public int compare(Duration rhs) {
        // TODO: Saxon is implementing the XPath semantics, not the schema semantics
        if (duration instanceof MonthDurationValue) {
            return ((MonthDurationValue)duration).compareTo((MonthDurationValue)((SaxonDuration)rhs).duration);
        } else if (duration instanceof SecondsDurationValue) {
            return ((SecondsDurationValue)duration).compareTo((SecondsDurationValue)((SaxonDuration)rhs).duration);
        } else {
            throw new IllegalArgumentException("Non-comparable durations");
        }
    }

    /**
     * Returns a hash code consistent with the definition of the equals method.
     *
     * @see Object#hashCode()
     */
    public int hashCode() {
        return duration.hashCode();
    }
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s):
//