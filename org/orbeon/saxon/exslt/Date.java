package net.sf.saxon.exslt;
import java.text.SimpleDateFormat;
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

    private Date() {};

    /**
    * The date:date-time function returns the current date and time as a date/time string.
    * The date/time string that's returned must be a string in the format defined as the
    * lexical representation of xs:dateTime in [3.2.7 dateTime] of [XML Schema Part 2: Datatypes].
    * The date/time format is basically CCYY-MM-DDThh:mm:ss+hh:mm.
    * The date/time string format must include a time zone, either a Z to indicate
    * Coordinated Universal Time or a + or - followed by the difference between the
    * difference from UTC represented as hh:mm.
    */

    public static String dateTime() {
        Calendar calendar = new GregorianCalendar();
        int tzoffset = calendar.get(Calendar.ZONE_OFFSET) +
                        calendar.get(Calendar.DST_OFFSET);
        char sign = '+';
        if (tzoffset < 0) {
            sign = '-';
            tzoffset = -tzoffset;
        }
        int tzminutes = tzoffset / 60000;
        int tzhours = tzminutes / 60;
        tzminutes = tzminutes % 60;
        String tzh = "" + tzhours;
        while (tzh.length() < 2) tzh = "0" + tzh;
        String tzm = "" + tzminutes;
        while (tzm.length() < 2) tzm = "0" + tzm;

        SimpleDateFormat formatter
            = new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss");
        String base = formatter.format(new java.util.Date());
        return base + sign + tzh + ':' + tzm;
    }

    /**
    * The date:date function returns the date specified in the date/time string given as the
    * argument.
    * @param dateTime must start with [+|-]CCYY-MM-DD
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
    */

    public static String date() {
        return date(dateTime());
    }

    /**
    * The date:time function returns the time specified in the date/time string given as the
    * argument.
    * @param dateTime must start with [+|-]CCYY-MM-DDThh:mm:ss
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
    */

    public static String time() {
        return time(dateTime());
    }

    /**
    * The date:year function returns the year specified in the date/time string given as the
    * argument.
    * @param dateTime must begin with CCYY
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
    */

    public static double year() {
        return year(dateTime());
    }

    /**
    * Return true if the year specified in the date/time string
    * given as the argument is a leap year.
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
    */

    public static boolean leapYear() {
        return leapYear(dateTime());
    }

    /**
    * Return the month number from a date.
    * The date must start with either "CCYY-MM" or "--MM"
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
    */

    public static double monthInYear() {
        return monthInYear(date());
    }

    /**
    * Return the month name from a date.
    * The date must start with either "CCYY-MM" or "--MM"
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
    * @return the English month name, for example "January", "February"
    */

    public static String monthName() {
        return monthName(date());
    }

    /**
    * Return the month abbreviation from a date.
    * The date must start with either "CCYY-MM" or "--MM"
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
    * @return the English month abbreviation, for example "Jan", "Feb"
    */

    public static String monthAbbreviation() {
        return monthAbbreviation(date());
    }

    /**
    * Return the ISO week number of a specified date within the year
    * (Note, this returns the ISO week number: the result in EXSLT is underspecified)
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
    * Return the ISO week number of the current date within the year
    * (Note, this returns the ISO week number: the result in EXSLT is underspecified)
    */

    public static double weekInYear() {
        return weekInYear(date());
    }

    /**
    * Return the week number of a specified date within the month
    * (Note, this function is underspecified in EXSLT)
    */

    public static double weekInMonth(String dateTime) {
        return (double)(int)((dayInMonth(dateTime)-1) / 7 + 1);
    }

    /**
    * Return the ISO week number of the current date within the month
    */

    public static double weekInMonth() {
        return weekInMonth(date());
    }

    /**
    * Return the day number of a specified date within the year
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
    */

    public static double dayInYear() {
        return dayInYear(date());
    }

    /**
    * Return the day number of a specified date within the month
    * @param dateTime must start with CCYY-MM-DD, or --MM-DD, or ---DD
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
    */

    public static double dayInMonth() {
        return dayInMonth(date());
    }

    /**
    * Return the day-of-the-week in a month of a date as a number
    * (for example 3 for the 3rd Tuesday in May).
    * @param dateTime must start with CCYY-MM-DD
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
    */

    public static double dayOfWeekInMonth() {
        return dayOfWeekInMonth(date());
    }

    /**
    * Return the day of the week given in a date as a number.
    * The numbering of days of the week starts at 1 for Sunday, 2 for Monday
    * and so on up to 7 for Saturday.
    * @param dateTime must start with CCYY-MM-DD
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
    */

    public static double dayInWeek() {
        return dayInWeek(date());
    }

    /**
    * Return the day of the week given in a date as an English day name:
    * one of 'Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday' or 'Friday'.
    * @param dateTime must start with CCYY-MM-DD
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
    */

    public static String dayName() {
        return dayName(date());
    }

    /**
    * Return the day of the week given in a date as an English day abbreviation:
    * one of 'Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', or 'Sat'.
    * @param dateTime must start with CCYY-MM-DD
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
    */

    public static String dayAbbreviation() {
        return dayAbbreviation(date());
    }

    /**
    * Return the hour of the day in the specified date or date/time
    * @param dateTime must start with CCYY-MM-DDThh:mm:ss or hh:mm:ss
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
    */

    public static double hourInDay() {
        return hourInDay(time());
    }

    /**
    * Return the minute of the hour in the specified date or date/time
    * @param dateTime must start with CCYY-MM-DDThh:mm:ss or hh:mm:ss
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
    */

    public static double minuteInHour() {
        return minuteInHour(time());
    }

    /**
    * Return the second of the minute in the specified date or date/time
    * @param dateTime must start with CCYY-MM-DDThh:mm:ss or hh:mm:ss
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
    */

    public static double secondInMinute() {
        return secondInMinute(time());
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
