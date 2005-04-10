package net.sf.saxon.functions;
import net.sf.saxon.expr.ExpressionTool;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.instruct.NumberInstruction;
import net.sf.saxon.number.Numberer;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.FastStringBuffer;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.*;

import javax.xml.transform.TransformerException;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        //DateTimeValue value = (DateTimeValue)item.convert(Type.DATE_TIME, context);
        String format = argument[1].evaluateItem(context).getStringValue();

        String language;

        if (argument.length > 2) {
            AtomicValue languageVal = (AtomicValue)argument[2].evaluateItem(context);
            //StringValue calendarVal = (StringValue)argument[3].evaluateItem(context);
            //StringValue countryVal = (StringValue)argument[4].evaluateItem(context);
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
            language = Locale.getDefault().getLanguage();
        }

        return new StringValue(formatDate(value, format, language, context));
    }

    /**
     * This method analyzes the formatting picture and delegates the work of formatting
     * individual parts of the date.
     */

    private static CharSequence formatDate(CalendarValue value, String format, String language, XPathContext context)
    throws XPathException {

        Numberer numberer = NumberInstruction.makeNumberer(language, context);
        FastStringBuffer sb = new FastStringBuffer(32);
        int i = 0;
        while (true) {
            while (i < format.length() && format.charAt(i) != '[') {
                sb.append(format.charAt(i));
                if (format.charAt(i) == ']') {
                    i++;
                    if (i == format.length() || format.charAt(i) != ']') {
                        DynamicError e = new DynamicError("Closing ']' in date picture must be written as ']]'");
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
            if (format.charAt(i) == '[') {
                sb.append('[');
                i++;
            } else {
                int close = format.indexOf("]", i);
                if (close == -1) {
                    DynamicError e = new DynamicError("Date format contains a '[' with no matching ']'");
                    e.setXPathContext(context);
                    throw e;
                }
                String componentFormat = format.substring(i, close);
                sb.append(formatComponent(value, componentFormat.trim(), numberer, context));
                i = close+1;
            }
        }
        return sb;
    }

    private static Pattern componentPattern =
            Pattern.compile("([YMDdWwFHhmsfZzPCE])\\s*(.*)");

    private static CharSequence formatComponent(CalendarValue value, String specifier, Numberer numberer, XPathContext context)
    throws XPathException {
        boolean ignoreDate = (value instanceof TimeValue);
        boolean ignoreTime = (value instanceof DateValue);
        DateTimeValue dtvalue;
        if (ignoreDate) {
            dtvalue = ((TimeValue)value).toDateTime();
        } else if (ignoreTime) {
            dtvalue = (DateTimeValue)value.convert(Type.DATE_TIME, context);
        } else {
            dtvalue = (DateTimeValue)value;
        }
        Calendar cal = dtvalue.getCalendar();
        Matcher matcher = componentPattern.matcher(specifier);
        if (!matcher.matches()) {
            try {
                context.getController().getErrorListener().warning(
                        new DynamicError("Unrecognized date/time component [" + specifier + "] (ignored)"));
            } catch (TransformerException e) {
                throw DynamicError.makeDynamicError(e);
            }
            return "";
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
                    return "";
                } else {
                    return formatNumber(component, cal.get(Calendar.YEAR), format, defaultFormat, numberer, context);
                }
            case 'M':       // month
                if (ignoreDate) {
                    return "";
                } else {
                    return formatNumber(component, cal.get(Calendar.MONTH)+1, format, defaultFormat, numberer, context);
                }
            case 'D':       // day in month
                if (ignoreDate) {
                    return "";
                } else {
                    return formatNumber(component, cal.get(Calendar.DAY_OF_MONTH), format, defaultFormat, numberer, context);
                }
            case 'd':       // day in year
                if (ignoreDate) {
                    return "";
                } else {
                    return formatNumber(component, cal.get(Calendar.DAY_OF_YEAR), format, defaultFormat, numberer, context);
                }
            case 'W':       // week of year
                if (ignoreDate) {
                    return "";
                } else {
                    return formatNumber(component, cal.get(Calendar.WEEK_OF_YEAR), format, defaultFormat, numberer, context);
                }
            case 'w':       // week in month
                if (ignoreDate) {
                    return "";
                } else {
                    return formatNumber(component, cal.get(Calendar.WEEK_OF_MONTH), format, defaultFormat, numberer, context);
                }
            case 'H':       // hour in day
                if (ignoreTime) {
                    return "";
                } else {
                    return formatNumber(component, cal.get(Calendar.HOUR_OF_DAY), format, defaultFormat, numberer, context);
                }
            case 'h':       // hour in half-day (12 hour clock)
                if (ignoreTime) {
                    return "";
                } else {
                    int hr = cal.get(Calendar.HOUR);
                    if (hr==0) hr = 12;
                    return formatNumber(component, hr, format, defaultFormat, numberer, context);
                }
            case 'm':       // minutes
                if (ignoreTime) {
                    return "";
                } else {
                    return formatNumber(component, cal.get(Calendar.MINUTE), format, defaultFormat, numberer, context);
                }
            case 's':       // seconds
                if (ignoreTime) {
                    return "";
                } else {
                    return formatNumber(component, cal.get(Calendar.SECOND), format, defaultFormat, numberer, context);
                }
            case 'f':       // fractional seconds
                // ignore the format
                if (ignoreTime) {
                    return "";
                } else {
                    int millis = cal.get(Calendar.MILLISECOND) % 1000;
                    return ((1000 + millis)+"").substring(1);
                }
            case 'Z':       // timezone
            case 'z':       // timezone
                FastStringBuffer sbz = new FastStringBuffer(8);
                DateTimeValue.appendTimezone(cal, sbz);
                return sbz.toString();
                // TODO: this isn't the correct format for timezone

            case 'F':       // day of week
                if (ignoreDate) {
                    return "";
                } else {
                    return formatNumber(component, cal.get(Calendar.DAY_OF_WEEK), format, defaultFormat, numberer, context);
                }
            case 'P':       // am/pm marker
                if (ignoreTime) {
                    return "";
                } else {
                    int hour = cal.get(Calendar.HOUR_OF_DAY);
                    int minutes = cal.get(Calendar.MINUTE);
                    return formatNumber(component, hour*60 + minutes, format, defaultFormat, numberer, context);
                }
            case 'C':       // calendar
                return "Gregorian";
            case 'E':       // era
                if (ignoreDate) {
                    return "";
                } else {
                    return "*AD*";
                }
                //return formatEra(cal.get(Calendar.ERA), format);
            default:
                DynamicError e = new DynamicError("Unknown formatDate/time component specifier '" + format.charAt(0) + '\'');
                e.setXPathContext(context);
                throw e;
        }
    }

    private static Pattern formatPattern =
            Pattern.compile("([^ot,]*?)([ot]?)(,.*)?");

    private static Pattern widthPattern =
            Pattern.compile(",(\\*|[0-9]+)(\\-(\\*|[0-9]+))?");

    private static CharSequence formatNumber(String component, int value,
                                             String format, boolean defaultFormat, Numberer numberer, XPathContext context)
    throws XPathException {
        Matcher matcher = formatPattern.matcher(format);
        if (!matcher.matches()) {
            matcher = formatPattern.matcher("1");
            matcher.matches();
        }
        String primary = matcher.group(1);
        String modifier = matcher.group(2);
        String letterValue = ("t".equals(modifier) ? "traditional" : null);
        String ordinal = ("o".equals(modifier) ? numberer.getOrdinalSuffixForDateTime(component) : null);
        String widths = matcher.group(3);
        int min, max;

        if (widths==null || "".equals(widths)) {
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

        while (s.length() < min) {
            s = ("00000000"+s).substring(s.length()+8-min);
        }
        if (s.length() > max) {
            // the year is the only field we allow to be truncated
            if (component.charAt(0) == 'Y') {
                s = s.substring(s.length() - max);
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
                    try {
                        context.getController().getErrorListener().warning(
                                new DynamicError("Invalid width specifier '" + widths +
                                "' in date/time picture (ignored)"));
                    } catch (TransformerException e) {
                        throw DynamicError.makeDynamicError(e);
                    }
                    min = 1;
                    max = 50;
                }
            }

            if (min>max && max!=-1) {
                DynamicError e = new DynamicError("Minimum width in date/time picture exceeds maximum width");
                e.setXPathContext(context);
                throw e;
            }
            int[] result = new int[2];
            result[0] = min;
            result[1] = max;
            return result;
        } catch (NumberFormatException err) {
            DynamicError e = new DynamicError("Invalid integer used as width in date/time picture");
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

