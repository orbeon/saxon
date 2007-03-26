package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.ExpressionTool;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.instruct.NumberInstruction;
import org.orbeon.saxon.number.Numberer;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.StaticError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.*;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.math.BigDecimal;

/**
 * Implement the format-date() function in XPath 2.0
 */

public class FormatDate extends SystemFunction implements XSLTFunction {

    public void checkArguments(StaticContext env) throws XPathException {
        int numArgs = argument.length;
        if (numArgs != 2 && numArgs != 5) {
            throw new StaticError("Function " + getDisplayName(env.getNamePool()) +
                    " must have either two or five arguments",
                    ExpressionTool.getLocator(this));
        }
        super.checkArguments(env);
    }

    /**
     * Evaluate in a general context
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        CalendarValue value = (CalendarValue)argument[0].evaluateItem(context);
        if (value==null) {
            return null;
        }
        String format = argument[1].evaluateItem(context).getStringValue();

        String language;

        StringValue calendarVal = null;
        StringValue countryVal = null;
        if (argument.length > 2) {
            AtomicValue languageVal = (AtomicValue)argument[2].evaluateItem(context);
            calendarVal = (StringValue)argument[3].evaluateItem(context);
            countryVal = (StringValue)argument[4].evaluateItem(context);
            if (languageVal==null) {
                language = Locale.getDefault().getLanguage();
            } else {
                language = languageVal.getStringValue();
                if (language.length() >= 2) {
                    language = language.substring(0, 2);
                } else {
                    language = Locale.getDefault().getLanguage();
                }
            }
        } else {
            // Default language is "en", unless (a) the Java default locale is some other language, and
            // (b) there is a loadable Numberer for that language.
            language = Locale.getDefault().getLanguage();
            if (!language.equals("en")) {
                Numberer numberer = NumberInstruction.makeNumberer(language, "us", context);
                if (!numberer.getClass().getName().endsWith("Numberer_" + language)) {
                    language = "en";
                }
            }
        }

        String country = (countryVal == null ? null : countryVal.getStringValue());
        CharSequence result = formatDate(value, format, language, country, context);
        if (calendarVal != null) {
            String cal = calendarVal.getStringValue();
            if (!cal.equals("AD") && !cal.equals("ISO")) {
                result = "[Calendar: AD]" + result.toString();
            }
        }
        return new StringValue(result);
    }

    /**
     * This method analyzes the formatting picture and delegates the work of formatting
     * individual parts of the date.
     */

    private static CharSequence formatDate(CalendarValue value, String format, String language, String country, XPathContext context)
    throws XPathException {

        Numberer numberer = NumberInstruction.makeNumberer(language, country, context);
        FastStringBuffer sb = new FastStringBuffer(32);
        if (!numberer.getClass().getName().endsWith("Numberer_" + language)) {
            sb.append("[Language: en]");
        }
        int i = 0;
        while (true) {
            while (i < format.length() && format.charAt(i) != '[') {
                sb.append(format.charAt(i));
                if (format.charAt(i) == ']') {
                    i++;
                    if (i == format.length() || format.charAt(i) != ']') {
                        DynamicError e = new DynamicError("Closing ']' in date picture must be written as ']]'");
                        e.setErrorCode("XTDE1340");
                        e.setXPathContext(context);
                        throw e;
                    }
                }
                i++;
            }
            if (i == format.length()) {
                break;
            }
            // look for '[['
            i++;
            if (i < format.length() && format.charAt(i) == '[') {
                sb.append('[');
                i++;
            } else {
                int close = (i < format.length() ? format.indexOf("]", i) : -1);
                if (close == -1) {
                    DynamicError e = new DynamicError("Date format contains a '[' with no matching ']'");
                    e.setErrorCode("XTDE1340");
                    e.setXPathContext(context);
                    throw e;
                }
                String componentFormat = format.substring(i, close);
                sb.append(formatComponent(value, Whitespace.removeAllWhitespace(componentFormat), numberer, context));
                i = close+1;
            }
        }
        return sb;
    }

    private static Pattern componentPattern =
            Pattern.compile("([YMDdWwFHhmsfZzPCE])\\s*(.*)");

    private static CharSequence formatComponent(CalendarValue value, CharSequence specifier,
                                                Numberer numberer, XPathContext context)
    throws XPathException {
        boolean ignoreDate = (value instanceof TimeValue);
        boolean ignoreTime = (value instanceof DateValue);
        DateTimeValue dtvalue = value.toDateTime();

        Matcher matcher = componentPattern.matcher(specifier);
        if (!matcher.matches()) {
            DynamicError error = new DynamicError("Unrecognized date/time component [" + specifier + ']');
            error.setErrorCode("XTDE1340");
            error.setXPathContext(context);
            throw error;
        }
        String component = matcher.group(1);
        String format = matcher.group(2);
        if (format==null) {
            format = "";
        }
        boolean defaultFormat = false;
        if ("".equals(format) || format.startsWith(",")) {
            defaultFormat = true;
            switch (component.charAt(0) ) {
                case 'F':
                    format = "Nn" + format;
                    break;
                case 'P':
                    format = 'n' + format;
                    break;
                case 'C':
                case 'E':
                    format = 'N' + format;
                    break;
                case 'm':
                case 's':
                    format = "01" + format;
                    break;
                default:
                    format = '1' + format;
            }
        }

        switch (component.charAt(0)) {
            case 'Y':       // year
                if (ignoreDate) {
                    DynamicError error = new DynamicError("In formatTime(): an xs:time value does not contain a year component");
                    error.setErrorCode("XTDE1350");
                    error.setXPathContext(context);
                    throw error;
                } else {
                    int year = dtvalue.getYear();
                    if (year < 0) {
                        year = 1 - year;
                    }
                    return formatNumber(component, year, format, defaultFormat, numberer, context);
                }
            case 'M':       // month
                if (ignoreDate) {
                    DynamicError error = new DynamicError(
                            "In formatTime(): an xs:time value does not contain a month component");
                    error.setErrorCode("XTDE1350");
                    error.setXPathContext(context);
                    throw error;
                } else {
                    int month = dtvalue.getMonth();
                    return formatNumber(component, month, format, defaultFormat, numberer, context);
                }
            case 'D':       // day in month
                if (ignoreDate) {
                    DynamicError error = new DynamicError(
                            "In formatTime(): an xs:time value does not contain a day component");
                    error.setErrorCode("XTDE1350");
                    error.setXPathContext(context);
                    throw error;
                } else {
                    int day = dtvalue.getDay();
                    return formatNumber(component, day, format, defaultFormat, numberer, context);
                }
            case 'd':       // day in year
                if (ignoreDate) {
                    DynamicError error = new DynamicError(
                            "In formatTime(): an xs:time value does not contain a day component");
                    error.setErrorCode("XTDE1350");
                    error.setXPathContext(context);
                    throw error;
                } else {
                    int day = DateValue.getDayWithinYear(dtvalue.getYear(), dtvalue.getMonth(), dtvalue.getDay());
                    return formatNumber(component, day, format, defaultFormat, numberer, context);
                }
            case 'W':       // week of year
                if (ignoreDate) {
                    DynamicError error = new DynamicError(
                            "In formatTime(): cannot obtain the week number from an xs:time value");
                    error.setErrorCode("XTDE1350");
                    error.setXPathContext(context);
                    throw error;
                } else {
                    int week = DateValue.getWeekNumber(dtvalue.getYear(), dtvalue.getMonth(), dtvalue.getDay());
                    return formatNumber(component, week, format, defaultFormat, numberer, context);
                }
            case 'w':       // week in month
                if (ignoreDate) {
                    DynamicError error = new DynamicError(
                            "In formatTime(): cannot obtain the week number from an xs:time value");
                    error.setErrorCode("XTDE1350");
                    error.setXPathContext(context);
                    throw error;
                } else {
                    int week = DateValue.getWeekNumberWithinMonth(dtvalue.getYear(), dtvalue.getMonth(), dtvalue.getDay());
                    return formatNumber(component, week, format, defaultFormat, numberer, context);
                }
            case 'H':       // hour in day
                if (ignoreTime) {
                    DynamicError error = new DynamicError(
                            "In formatDate(): an xs:date value does not contain an hour component");
                    error.setErrorCode("XTDE1350");
                    error.setXPathContext(context);
                    throw error;
                } else {
                    IntegerValue hour = (IntegerValue)value.getComponent(Component.HOURS);
                    return formatNumber(component, (int)hour.longValue(), format, defaultFormat, numberer, context);
                }
            case 'h':       // hour in half-day (12 hour clock)
                if (ignoreTime) {
                    DynamicError error = new DynamicError(
                            "In formatDate(): an xs:date value does not contain an hour component");
                    error.setErrorCode("XTDE1350");
                    error.setXPathContext(context);
                    throw error;
                } else {
                    IntegerValue hour = (IntegerValue)value.getComponent(Component.HOURS);
                    int hr = (int)hour.longValue();
                    if (hr > 12) {
                        hr = hr - 12;
                    }
                    if (hr == 0) {
                        hr = 12;
                    }
                    return formatNumber(component, hr, format, defaultFormat, numberer, context);
                }
            case 'm':       // minutes
                if (ignoreTime) {
                    DynamicError error = new DynamicError(
                            "In formatDate(): an xs:date value does not contain a minutes component");
                    error.setErrorCode("XTDE1350");
                    error.setXPathContext(context);
                    throw error;
                } else {
                    IntegerValue min = (IntegerValue)value.getComponent(Component.MINUTES);
                    return formatNumber(component, (int)min.longValue(), format, defaultFormat, numberer, context);
                }
            case 's':       // seconds
                if (ignoreTime) {
                    DynamicError error = new DynamicError(
                            "In formatDate(): an xs:date value does not contain a seconds component");
                    error.setErrorCode("XTDE1350");
                    error.setXPathContext(context);
                    throw error;
                } else {
                    // TODO: should we be ignoring the fractional part of the seconds (use Component.WHOLE_SECONDS)
                    DecimalValue sec = (DecimalValue)value.getComponent(Component.SECONDS);
                    return formatNumber(component, sec.getValue().intValue(), format, defaultFormat, numberer, context);
                }
            case 'f':       // fractional seconds
                // ignore the format
                if (ignoreTime) {
                    DynamicError error = new DynamicError(
                            "In formatDate(): an xs:date value does not contain a fractional seconds component");
                    error.setErrorCode("XTDE1350");
                    error.setXPathContext(context);
                    throw error;
                } else {
                    int micros = (int)((IntegerValue)value.getComponent(Component.MICROSECONDS)).longValue();
                    return formatNumber(component, micros, format, defaultFormat, numberer, context);
                }
            case 'Z':       // timezone in +hh:mm format, unless format=N in which case use timezone name
                if (value.hasTimezone()) {
                    if (format.startsWith("N")) {
                        return numberer.getTimezoneName(value.getTimezoneInMinutes());
                    } else if (format.startsWith("n")) {
                        return numberer.getTimezoneName(value.getTimezoneInMinutes()).toLowerCase();
                    } else {
                        FastStringBuffer sbz = new FastStringBuffer(8);
                        value.appendTimezone(sbz);
                        return sbz.toString();
                    }
                } else {
                    return "";
                }
            case 'z':       // timezone
                if (value.hasTimezone()) {
                    int tz = value.getTimezoneInMinutes();
                    return "GMT" + (tz==0 ? "" : ((tz>0 ? "+" : "-") + Math.abs(tz/60) + (tz%60 == 0 ? "" : ".5")));
                } else {
                    return "";
                }
            case 'F':       // day of week
                if (ignoreDate) {
                    DynamicError error = new DynamicError(
                            "In formatTime(): an xs:time value does not contain day-of-week component");
                    error.setErrorCode("XTDE1350");
                    error.setXPathContext(context);
                    throw error;
                } else {
                    int day = DateValue.getDayOfWeek(dtvalue.getYear(), dtvalue.getMonth(), dtvalue.getDay());
                    return formatNumber(component, day, format, defaultFormat, numberer, context);
                }
            case 'P':       // am/pm marker
                if (ignoreTime) {
                    DynamicError error = new DynamicError(
                            "In formatDate(): an xs:date value does not contain an am/pm component");
                    error.setErrorCode("XTDE1350");
                    error.setXPathContext(context);
                    throw error;
                } else {
                    int minuteOfDay = dtvalue.getHour()*60 + dtvalue.getMinute();
                    return formatNumber(component, minuteOfDay, format, defaultFormat, numberer, context);
                }
            case 'C':       // calendar
                return numberer.getCalendarName("AD");
            case 'E':       // era
                if (ignoreDate) {
                    DynamicError error = new DynamicError(
                            "In formatTime(): an xs:time value does not contain an AD/BC component");
                    error.setErrorCode("XTDE1350");
                    error.setXPathContext(context);
                    throw error;
                } else {
                    int year = dtvalue.getYear();
                    return numberer.getEraName(year);
                }
            default:
                DynamicError e = new DynamicError(
                        "Unknown formatDate/time component specifier '" + format.charAt(0) + '\'');
                e.setErrorCode("XTDE1340");
                e.setXPathContext(context);
                throw e;
        }
    }

    private static Pattern formatPattern =
            //Pattern.compile("([^ot,]*?)([ot]?)(,.*)?");   // GNU Classpath has problems with this one
            Pattern.compile("([^,]*)(,.*)?");           // Note, the group numbers are different from above

    private static Pattern widthPattern =
            Pattern.compile(",(\\*|[0-9]+)(\\-(\\*|[0-9]+))?");

    private static Pattern alphanumericPattern =
            Pattern.compile("([A-Za-z0-9]|\\p{L}|\\p{N})*");
                // the first term is redundant, but GNU Classpath can't cope with the others...

    private static Pattern digitsPattern =
            Pattern.compile("\\p{Nd}*");

    private static CharSequence formatNumber(String component, int value,
                                             String format, boolean defaultFormat, Numberer numberer, XPathContext context)
    throws XPathException {
        Matcher matcher = formatPattern.matcher(format);
        if (!matcher.matches()) {
            DynamicError error = new DynamicError("Unrecognized format picture [" + component + format + ']');
            error.setErrorCode("XTDE1340");
            error.setXPathContext(context);
            throw error;
        }
        //String primary = matcher.group(1);
        //String modifier = matcher.group(2);
        String primary = matcher.group(1);
        String modifier = null;
        if (primary.endsWith("t")) {
            primary = primary.substring(0, primary.length()-1);
            modifier = "t";
        } else if (primary.endsWith("o")) {
            primary = primary.substring(0, primary.length()-1);
            modifier = "o";
        }
        String letterValue = ("t".equals(modifier) ? "traditional" : null);
        String ordinal = ("o".equals(modifier) ? numberer.getOrdinalSuffixForDateTime(component) : null);
        String widths = matcher.group(2);   // was 3

        if (!alphanumericPattern.matcher(primary).matches()) {
            DynamicError error = new DynamicError("In format picture at '" + primary +
                    "', primary format must be alphanumeric");
            error.setErrorCode("XTDE1340");
            error.setXPathContext(context);
            throw error;
        }
        int min = 1;
        int max = Integer.MAX_VALUE;

        if (widths==null || "".equals(widths)) {
            if (digitsPattern.matcher(primary).matches()) {
                int len = StringValue.getStringLength(primary);
                if (len > 1) {
                    // "A format token containing leading zeroes, such as 001, sets the minimum and maximum width..."
                    // We interpret this literally: a format token of "1" does not set a maximum, because it would
                    // cause the year 2006 to be formatted as "6".
                    min = len;
                    max = len;
                }
            }
        } else if (primary.equals("I") || primary.equals("i")) {
            // for roman numerals, ignore the width specifier
            min = 1;
            max = Integer.MAX_VALUE;
        } else {
            int[] range = getWidths(widths, context);
            min = range[0];
            max = range[1];
            if (defaultFormat) {
                // if format was defaulted, the explicit widths override the implicit format
                if (primary.endsWith("1") && min != primary.length()) {
                    FastStringBuffer sb = new FastStringBuffer(min+1);
                    for (int i=1; i<min; i++) {
                        sb.append('0');
                    }
                    sb.append('1');
                    primary = sb.toString();
                }
            }
        }

        if ("P".equals(component)) {
            // A.M./P.M. can only be formatted as a name
            if (!("N".equals(primary) || "n".equals(primary) || "Nn".equals(primary))) {
                primary = "n";
            }
        } else if ("f".equals(component)) {
            // value is supplied as integer number of microseconds
            String s;
            if (value==0) {
                s = "0";
            } else {
                s = ((1000000 + value) + "").substring(1);
                if (s.length() > max) {
                    DecimalValue dec = new DecimalValue(new BigDecimal("0." + s));
                    dec = (DecimalValue)dec.roundHalfToEven(max);
                    s = dec.getStringValue();
                    if (s.length() > 2) {
                        // strip the ".0"
                        s = s.substring(2);
                    } else {
                        // fractional seconds value was 0
                        s = "";
                    }
                }
            }
            while (s.length() < min) {
                s = s + '0';
            }
            while (s.length() > min && s.charAt(s.length()-1) == '0') {
                s = s.substring(0, s.length()-1);
            }
            return s;
        }

        if ("N".equals(primary) || "n".equals(primary) || "Nn".equals(primary)) {
            String s = "";
            if ("M".equals(component)) {
                s = numberer.monthName(value, min, max);
            } else if ("F".equals(component)) {
                s = numberer.dayName(value, min, max);
            } else if ("P".equals(component)) {
                s = numberer.halfDayName(value, min, max);
            } else {
                primary = "1";
            }
            if ("N".equals(primary)) {
                return s.toUpperCase();
            } else if ("n".equals(primary)) {
                return s.toLowerCase();
            } else {
                return s;
            }
        }

        String s = numberer.format(value, primary, 0, ",", letterValue, ordinal);
        int len = StringValue.getStringLength(s);
        while (len < min) {
            // assert: this can only happen as a result of width specifiers, in which case we're using ASCII digits
            s = ("00000000"+s).substring(s.length()+8-min);
            len = StringValue.getStringLength(s);
        }
        if (len > max) {
            // the year is the only field we allow to be truncated
            if (component.charAt(0) == 'Y') {
                if (len == s.length()) {
                    // no wide characters
                    s = s.substring(s.length() - max);
                } else {
                    // assert: each character must be two bytes long
                    s = s.substring(s.length() - 2*max);
                }

            }
        }
        return s;
    }

    private static int[] getWidths(String widths, XPathContext context) throws XPathException {
        try {
            int min = -1;
            int max = -1;

            if (!"".equals(widths)) {
                Matcher widthMatcher = widthPattern.matcher(widths);
                if (widthMatcher.matches()) {
                    String smin = widthMatcher.group(1);
                    if (smin==null || "".equals(smin) || "*".equals(smin)) {
                        min = 1;
                    } else {
                        min = Integer.parseInt(smin);
                    }
                    String smax = widthMatcher.group(3);
                    if (smax==null || "".equals(smax) || "*".equals(smax)) {
                        max = Integer.MAX_VALUE;
                    } else {
                        max = Integer.parseInt(smax);
                    }
                } else {
                    DynamicError error = new DynamicError("Unrecognized width specifier");
                    error.setErrorCode("XTDE1340");
                    error.setXPathContext(context);
                    throw error;
                }
            }

            if (min>max && max!=-1) {
                DynamicError e = new DynamicError("Minimum width in date/time picture exceeds maximum width");
                e.setErrorCode("XTDE1340");
                e.setXPathContext(context);
                throw e;
            }
            int[] result = new int[2];
            result[0] = min;
            result[1] = max;
            return result;
        } catch (NumberFormatException err) {
            DynamicError e = new DynamicError("Invalid integer used as width in date/time picture");
            e.setErrorCode("XTDE1340");
            e.setXPathContext(context);
            throw e;
        }
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//

