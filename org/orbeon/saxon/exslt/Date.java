package org.orbeon.saxon.exslt;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.trans.XPathException;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
* This class implements extension functions in the
* http://exslt.org/dates-and-times namespace. <p>
*/

public final class Date  {

    /**
    * Private constructor to disallow instantiation
    */

    private Date() {}

    /**
     * The date:date-time function returns the current date and time as a date/time string.
     * The date/time string that's returned must be a string in the format defined as the
     * lexical representation of xs:dateTime in [3.2.7 dateTime] of [XML Schema Part 2: Datatypes].
     * The date/time format is basically CCYY-MM-DDThh:mm:ss+hh:mm.
     * The date/time string format must include a time zone, either a Z to indicate
     * Coordinated Universal Time or a + or - followed by the difference between the
     * difference from UTC represented as hh:mm.
     * @param context the XPath dynamic context
     * @return the current date and time as a date/time string
    */

    public static String dateTime(XPathContext context) throws XPathException {
        return context.getCurrentDateTime().getStringValue();
    }

    /**
     * The date:date function returns the date specified in the date/time string given as the
     * argument.
     * @param dateTime must start with [+|-]CCYY-MM-DD
     * @return the date portion of the supplied dateTime
    */

    public static String date(String dateTime) {
        int offset = 0;
        if (dateTime.length() >= 1 &&
                (dateTime.charAt(0) == '-' || dateTime.charAt(0) == '+')) {
            offset = 1;
        }
        if (dateTime.length() >= offset+10) {
            return dateTime.substring(0, offset+10);
        } else {
            return "";
        }
    }

    /**
     * The date:date function returns the current date.
     * @param context the XPath dynamic context
     * @return the current date as a string
    */

    public static String date(XPathContext context) throws XPathException {
        return date(dateTime(context));
    }

    /**
     * The date:time function returns the time specified in the date/time string given as the
     * argument.
     * @param dateTime must start with [+|-]CCYY-MM-DDThh:mm:ss
     * @return the time part of the string
    */

    public static String time(String dateTime) {
        int t = dateTime.indexOf('T');
        if (t<0 || t==dateTime.length()-1) {
            return "";
        } else {
            return dateTime.substring(t+1);
        }
    }

    /**
     * The date:time function returns the current time.
     * @param context the XPath dynamic context
     * @return the current time as a string
     */

    public static String time(XPathContext context) throws XPathException {
        return time(dateTime(context));
    }

    /**
     * The date:year function returns the year specified in the date/time string given as the
     * argument.
     * @param dateTime must begin with CCYY
     * @return the year part of the supplied date/time
    */

    public static double year(String dateTime) {
        if (dateTime.startsWith("-")) {
            return Double.NaN;
        }
        try {
            return (double)Integer.parseInt(dateTime.substring(0, 4));
        } catch (Exception err) {
            return Double.NaN;
        }
    }

    /**
     * The date:year function returns the current year.
     * @param context the XPath dynamic context
     * @return the current year as a double
    */

    public static double year(XPathContext context) throws XPathException {
        return year(dateTime(context));
    }

    /**
     * Return true if the year specified in the date/time string
     * given as the argument is a leap year.
     * @param dateTime a dateTime as a string
     * @return true if the year is a leap year
    */

    public static boolean leapYear(String dateTime) {
        double year = year(dateTime);
        if (Double.isNaN(year)) {
            return false;
        }
        int y = (int)year;
        return (y % 4 == 0) && !((y % 100 == 0) && !(y % 400 == 0));
    }

    /**
     * Returns true if the current year is a leap year
     * @param context the XPath dynamic context
     * @return true if the current year is a leap year
     */

    public static boolean leapYear(XPathContext context) throws XPathException {
        return leapYear(dateTime(context));
    }

    /**
     * Return the month number from a date.
     * The date must start with either "CCYY-MM" or "--MM"
     * @param dateTime a dateTime as a string
     * @return the month extracted from the dateTime
    */

    public static double monthInYear(String dateTime) {
        try {
            if (dateTime.startsWith("--")) {
                return (double)Integer.parseInt(dateTime.substring(2, 4));
            } else {
                if (dateTime.indexOf('-')!=4) {
                    return Double.NaN;
                }
                return (double)Integer.parseInt(dateTime.substring(5, 7));
            }
        } catch (Exception err) {
            return Double.NaN;
        }
    }

    /**
    * Return the month number from the current date.
     * @param context the XPath dynamic context
     * @return the current month number
    */

    public static double monthInYear(XPathContext context) throws XPathException {
        return monthInYear(dateTime(context));
    }

    /**
     * Return the month name from a date.
     * The date must start with either "CCYY-MM" or "--MM"
     * @param date the date/time as a string
     * @return the English month name, for example "January", "February"
    */

    public static String monthName(String date) {
        String[] months = {"January", "February", "March", "April", "May", "June",
                            "July", "August", "September", "October", "November", "December"};
        double m = monthInYear(date);
        if (Double.isNaN(m)) {
            return "";
        }
        return months[(int)m - 1];
    }

    /**
    * Return the month name from the current date.
    * @param context the XPath dynamic context
    * @return the English month name, for example "January", "February"
    */

    public static String monthName(XPathContext context) throws XPathException {
        return monthName(dateTime(context));
    }

    /**
    * Return the month abbreviation from a date.
    * @param date The date must start with either "CCYY-MM" or "--MM"
    * @return the English month abbreviation, for example "Jan", "Feb"
    */

    public static String monthAbbreviation(String date) {
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        double m = monthInYear(date);
        if (Double.isNaN(m)) {
            return "";
        }
        return months[(int)m - 1];
    }

    /**
    * Return the month abbreviation from the current date.
     * @param context the XPath dynamic context
    * @return the English month abbreviation, for example "Jan", "Feb"
    */

    public static String monthAbbreviation(XPathContext context) throws XPathException {
        return monthAbbreviation(dateTime(context));
    }

    /**
    * Return the ISO week number of a specified date within the year
    * (Note, this returns the ISO week number: the result in EXSLT is underspecified)
     * @param dateTime the current date starting CCYY-MM-DD
     * @return the ISO week number
    */

    public static double weekInYear(String dateTime) {
        int dayInYear = (int)dayInYear(dateTime);
        String firstJan = dateTime.substring(0,4) + "-01-01";
        int jan1day = ((int)dayInWeek(firstJan) + 5) % 7;
        int daysInFirstWeek = (jan1day==0 ? 0 : 7 - jan1day);

        int rawWeek = (dayInYear - daysInFirstWeek + 6) / 7;

        if (daysInFirstWeek >= 4) {
            return rawWeek + 1;
        } else {
            if (rawWeek>0) {
                return rawWeek;
            } else {
                // week number should be 52 or 53: same as 31 Dec in previous year
                int lastYear = Integer.parseInt(dateTime.substring(0,4)) - 1;
                String dec31 = lastYear + "-12-31";
                    // assumes year > 999
                return weekInYear(dec31);
            }
        }
    }

    /**
    * Return the ISO week number of the current date
     * @param context the XPath dynamic context
    * (Note, this returns the ISO week number: the result in EXSLT is underspecified)
     * @return the ISO week number
    */

    public static double weekInYear(XPathContext context) throws XPathException {
        return weekInYear(dateTime(context));
    }

    /**
    * Return the week number of a specified date within the month
    * (Note, this function is underspecified in EXSLT)
     * @param dateTime the date starting CCYY-MM-DD
     * @return the week number within the month
    */

    public static double weekInMonth(String dateTime) {
        return (double)(int)((dayInMonth(dateTime)-1) / 7 + 1);
    }

    /**
    * Return the ISO week number of the current date within the month
     * @param context the XPath dynamic context
     * @return the week number within the month
    */

    public static double weekInMonth(XPathContext context) throws XPathException {
        return weekInMonth(dateTime(context));
    }

    /**
    * Return the day number of a specified date within the year
     * @param dateTime the date starting with CCYY-MM-DD
     * @return the day number within the year, as a double
    */

    public static double dayInYear(String dateTime) {
        int month=(int)monthInYear(dateTime);
        int day = (int)dayInMonth(dateTime);
        int[] prev = {  0,
                        31,
                        31+28,
                        31+28+31,
                        31+28+31+30,
                        31+28+31+30+31,
                        31+28+31+30+31+30,
                        31+28+31+30+31+30+31,
                        31+28+31+30+31+30+31+31,
                        31+28+31+30+31+30+31+31+30,
                        31+28+31+30+31+30+31+31+30+31,
                        31+28+31+30+31+30+31+31+30+31+30,
                        31+28+31+30+31+30+31+31+30+31+30+31 };
        int leap = (month>2 && leapYear(dateTime) ? 1 : 0);
        return prev[month-1] + leap + day;
    }

    /**
    * Return the day number of the current date within the year
     * @param context the XPath dynamic context
     * @return the day number within the year, as a double
    */

    public static double dayInYear(XPathContext context) throws XPathException {
        return dayInYear(dateTime(context));
    }

    /**
    * Return the day number of a specified date within the month
    * @param dateTime must start with CCYY-MM-DD, or --MM-DD, or ---DD
     * @return the day number within the month, as a double
    */

    public static double dayInMonth(String dateTime) {
        try {
            if (dateTime.startsWith("---")) {
                return (double)Integer.parseInt(dateTime.substring(3,5));
            } else if (dateTime.startsWith("--")) {
                return (double)Integer.parseInt(dateTime.substring(5,7));
            } else {
                return (double)Integer.parseInt(dateTime.substring(8,10));
            }
        } catch (Exception err) {
            return Double.NaN;
        }
    }

    /**
    * Return the day number of the current date within the month
     * @param context the XPath dynamic context
     * @return the current day number, as a double
    */

    public static double dayInMonth(XPathContext context) throws XPathException {
        return dayInMonth(dateTime(context));
    }

    /**
    * Return the day-of-the-week in a month of a date as a number
    * (for example 3 for the 3rd Tuesday in May).
    * @param dateTime must start with CCYY-MM-DD
     * @return the the day-of-the-week in a month of a date as a number
    * (for example 3 for the 3rd Tuesday in May).
    */

    public static double dayOfWeekInMonth(String dateTime) {
        double dd = dayInMonth(dateTime);
        if (Double.isNaN(dd)) {
            return dd;
        }
        return (((int)dd) - 1) / 7 + 1;
    }

    /**
    * Return the day-of-the-week in a month of the current date as a number
    * (for example 3 for the 3rd Tuesday in May).
     * @param context the XPath dynamic context
     * @return the the day-of-the-week in a month of the current date as a number
    * (for example 3 for the 3rd Tuesday in May).
    */

    public static double dayOfWeekInMonth(XPathContext context) throws XPathException {
        return dayOfWeekInMonth(dateTime(context));
    }

    /**
    * Return the day of the week given in a date as a number.
    * The numbering of days of the week starts at 1 for Sunday, 2 for Monday
    * and so on up to 7 for Saturday.
    * @param dateTime must start with CCYY-MM-DD
     * @return the day of the week as a number
    */

    public static double dayInWeek(String dateTime) {
        double yy = year(dateTime);
        double mm = monthInYear(dateTime);
        double dd = dayInMonth(dateTime);
        if (Double.isNaN(yy) || Double.isNaN(mm) || Double.isNaN(dd)) {
            return Double.NaN;
        }
        GregorianCalendar calDate =
            new GregorianCalendar(
                        (int)yy,
                        (int)mm-1,
                        (int)dd);
        calDate.setFirstDayOfWeek(Calendar.SUNDAY);
        return calDate.get(Calendar.DAY_OF_WEEK);
    }

    /**
    * Return the day of the week in the current date as a number.
    * The numbering of days of the week starts at 1 for Sunday, 2 for Monday
    * and so on up to 7 for Saturday.
     * @param context the XPath dynamic context
     * @return the day of the week as a number
    */

    public static double dayInWeek(XPathContext context) throws XPathException {
        return dayInWeek(dateTime(context));
    }

    /**
    * Return the day of the week given in a date as an English day name:
    * one of 'Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday' or 'Friday'.
    * @param dateTime must start with CCYY-MM-DD
     * @return the English name of the day of the week
    */

    public static String dayName(String dateTime) {
        String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday",
                            "Saturday" };
        double d = dayInWeek(dateTime);
        if (Double.isNaN(d)) {
            return "";
        }
        return days[(int)d - 1];
    }

    /**
    * Return the day of the week given in the current date as an English day name:
    * one of 'Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday' or 'Friday'.
     * @param context the XPath dynamic context
     * @return the English name of the day of the week
    */

    public static String dayName(XPathContext context) throws XPathException {
        return dayName(dateTime(context));
    }

    /**
    * Return the day of the week given in a date as an English day abbreviation:
    * one of 'Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', or 'Sat'.
    * @param dateTime must start with CCYY-MM-DD
     * @return the English day abbreviation
    */

    public static String dayAbbreviation(String dateTime) {
        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
        double d = dayInWeek(dateTime);
        if (Double.isNaN(d)) {
            return "";
        }
        return days[(int)d - 1];
    }

    /**
    * Return the day of the week given in the current date as an English day abbreviation:
    * one of 'Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', or 'Sat'.
     * @param context the XPath dynamic context
     * @return the English day abbreviation
    */

    public static String dayAbbreviation(XPathContext context) throws XPathException {
        return dayAbbreviation(dateTime(context));
    }

    /**
    * Return the hour of the day in the specified date or date/time
    * @param dateTime must start with CCYY-MM-DDThh:mm:ss or hh:mm:ss
     * @return the hour
    */

    public static double hourInDay(String dateTime) {
        int t = dateTime.indexOf('T');
        try {
            int hh = Integer.parseInt(dateTime.substring(t+1, t+3));
            return (double)hh;
        } catch (Exception err) {
            return Double.NaN;
        }
    }

    /**
    * Return the current hour of the day
     * @param context the XPath dynamic context
     * @return the hour
    */

    public static double hourInDay(XPathContext context) throws XPathException {
        return hourInDay(dateTime(context));
    }

    /**
    * Return the minute of the hour in the specified date or date/time
    * @param dateTime must start with CCYY-MM-DDThh:mm:ss or hh:mm:ss
     * @return the minute
    */

    public static double minuteInHour(String dateTime) {
        int t = dateTime.indexOf('T');
        try {
            int mm = Integer.parseInt(dateTime.substring(t+4, t+6));
            return (double)mm;
        } catch (Exception err) {
            return Double.NaN;
        }
    }

    /**
    * Return the current minute of the hour
     * @param context the XPath dynamic context
     * @return the minute
    */

    public static double minuteInHour(XPathContext context) throws XPathException {
        return minuteInHour(dateTime(context));
    }

    /**
    * Return the second of the minute in the specified date or date/time
    * @param dateTime must start with CCYY-MM-DDThh:mm:ss or hh:mm:ss
     * @return the second
    */

    public static double secondInMinute(String dateTime) {
        int t = dateTime.indexOf('T');
        try {
            int ss = Integer.parseInt(dateTime.substring(t+7, t+9));
            return (double)ss;
        } catch (Exception err) {
            return Double.NaN;
        }
    }

    /**
    * Return the current second of the minute
     * @param context the XPath dynamic context
     * @return the second
    */

    public static double secondInMinute(XPathContext context) throws XPathException {
        return secondInMinute(dateTime(context));
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
// Contributor(s): none.
//
