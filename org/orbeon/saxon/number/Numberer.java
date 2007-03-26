package org.orbeon.saxon.number;

/**
  * Interface Numberer supports number formatting. There is a separate
  * implementation for each language, e.g. Numberer_en for English.
  * This supports the xsl:number element
  * @author Michael H. Kay
  */

public interface Numberer {

    /**
     * Set the country used by this numberer (currently used only for names of timezones)
     */

    public void setCountry(String country);

    /**
    * Format a number into a string
    * @param number The number to be formatted
    * @param picture The format token. This is a single component of the format attribute
    * of xsl:number, e.g. "1", "01", "i", or "a"
    * @param groupSize number of digits per group (0 implies no grouping)
    * @param groupSeparator string to appear between groups of digits
    * @param letterValue The letter-value specified to xsl:number: "alphabetic" or
    * "traditional". Can also be an empty string or null.
    * @param ordinal The value of the ordinal attribute specified to xsl:number
    * The value "yes" indicates that ordinal numbers should be used; "" or null indicates
    * that cardinal numbers
    * @return the formatted number. Note that no errors are reported; if the request
    * is invalid, the number is formatted as if the string() function were used.
    */

    public String format(long number,
                         String picture,
                         int groupSize,
                         String groupSeparator,
                         String letterValue,
                         String ordinal);

    /**
     * Get a month name or abbreviation
     * @param month The month number (1=January, 12=December)
     * @param minWidth The minimum number of characters
     * @param maxWidth The maximum number of characters
     */

    public String monthName(int month, int minWidth, int maxWidth);

    /**
     * Get a day name or abbreviation
     * @param day The month number (1=Monday, 7=Sunday)
     * @param minWidth The minimum number of characters
     * @param maxWidth The maximum number of characters
     */

    public String dayName(int day, int minWidth, int maxWidth);

    /**
     * Get an am/pm indicator
     * @param minutes the minutes within the day
     * @param minWidth minimum width of output
     * @param maxWidth maximum width of output
     * @return the AM or PM indicator
     */

    public String halfDayName(int minutes,
                              int minWidth, int maxWidth);

    /**
     * Get an ordinal suffix for a particular component of a date/time.
     * @param component the component specifier from a format-dateTime picture, for
     * example "M" for the month or "D" for the day.
     * @return a string that is acceptable in the ordinal attribute of xsl:number
     * to achieve the required ordinal representation. For example, "-e" for the day component
     * in German, to have the day represented as "dritte August".
     */

    public String getOrdinalSuffixForDateTime(String component);

    /**
     * Get the name for an era (e.g. "BC" or "AD")
     * @param year the proleptic gregorian year, using "0" for the year before 1AD
     */

    public String getEraName(int year);

    /**
     * Get the name of a timezone
     * @param tz the offset of the timezone from GMT in minutes
     */

    public String getTimezoneName(int tz);

    /**
     * Get the name of a calendar
     * @param code The code representing the calendar as in the XSLT 2.0 spec, e.g. AD for the Gregorian calendar
     */

    public String getCalendarName(String code);


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
