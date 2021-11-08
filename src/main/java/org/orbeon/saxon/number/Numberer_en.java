package org.orbeon.saxon.number;

/**
 * Numberer class for the English language
 */

public class Numberer_en extends AbstractNumberer {

    /**
     * Construct the ordinal suffix for a number, for example "st", "nd", "rd"
     * @param ordinalParam the value of the ordinal attribute (used in non-English
     * language implementations)
     * @param number the number being formatted
     * @return the ordinal suffix to be appended to the formatted number
     */

    protected String ordinalSuffix(String ordinalParam, long number) {
        int penult = ((int)(number % 100)) / 10;
        int ult = (int)(number % 10);
        if (penult==1) {
            // e.g. 11th, 12th, 13th
            return "th";
        } else {
            if (ult==1) {
                return "st";
            } else if (ult==2) {
                return "nd";
            } else if (ult==3) {
                return "rd";
            } else {
                return "th";
            }
        }
    }

    /**
     * Show the number as words in title case. (We choose title case because
     * the result can then be converted algorithmically to lower case or upper case).
     * @param number the number to be formatted
     * @return the number formatted as English words
    */

    public String toWords(long number) {
        if (number >= 1000000000) {
            long rem = number % 1000000000;
            return toWords(number / 1000000000) + " Billion" +
                    (rem==0 ? "" : (rem < 100 ? " and " : " ") + toWords(rem));
        } else if (number >= 1000000) {
            long rem = number % 1000000;
            return toWords(number / 1000000) + " Million" +
                    (rem==0 ? "" : (rem < 100 ? " and " : " ") + toWords(rem));
        } else if (number >= 1000) {
            long rem = number % 1000;
            return toWords(number / 1000) + " Thousand" +
                    (rem==0 ? "" : (rem < 100 ? " and " : " ") + toWords(rem));
        } else if (number >= 100) {
            long rem = number % 100;
            return toWords(number / 100) + " Hundred" +
                (rem==0 ? "" : " and " + toWords(rem));
        } else {
            if (number < 20) return englishUnits[(int)number];
            int rem = (int)(number % 10);
            return englishTens[(int)number / 10] +
                (rem==0 ? "" : ' ' + englishUnits[rem]);
        }
    }

    /**
     * Show an ordinal number as English words in a requested case (for example, Twentyfirst)
     * @param ordinalParam the value of the "ordinal" attribute as supplied by the user
     * @param number the number to be formatted
     * @param wordCase the required case for example {@link #UPPER_CASE},
     * {@link #LOWER_CASE}, {@link #TITLE_CASE}
     * @return the formatted number
    */

    public String toOrdinalWords(String ordinalParam, long number, int wordCase) {
        String s;
        if (number >= 1000000000) {
            long rem = number % 1000000000;
            s = toWords(number / 1000000000) + " Billion" +
                    (rem==0 ? "th" : (rem < 100 ? " and " : " ") +
                    toOrdinalWords(ordinalParam, rem, wordCase));
        } else if (number >= 1000000) {
            long rem = number % 1000000;
            s = toWords(number / 1000000) + " Million" +
                    (rem==0 ? "th" : (rem < 100 ? " and " : " ") +
                    toOrdinalWords(ordinalParam, rem, wordCase));
        } else if (number >= 1000) {
            long rem = number % 1000;
            s = toWords(number / 1000) + " Thousand" +
                    (rem==0 ? "th" : (rem < 100 ? " and " : " ") +
                    toOrdinalWords(ordinalParam, rem, wordCase));
        } else if (number >= 100) {
            long rem = number % 100;
            s = toWords(number / 100) + " Hundred" +
                    (rem==0 ? "th" : " and " +
                    toOrdinalWords(ordinalParam, rem, wordCase));
        } else {
            if (number < 20) {
                s = englishOrdinalUnits[(int)number];
            } else {
                int rem = (int)(number % 10);
                if (rem==0) {
                    s = englishOrdinalTens[(int)number / 10];
                } else {
                    s = englishTens[(int)number / 10] + '-' + englishOrdinalUnits[rem];
                }
            }
        }
        if (wordCase == UPPER_CASE) {
            return s.toUpperCase();
        } else if (wordCase == LOWER_CASE) {
            return s.toLowerCase();
        } else {
            return s;
        }
    }

    private static String[] englishUnits = {
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
        "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
        "Seventeen", "Eighteen", "Nineteen"};

    private static String[] englishTens = {
        "", "Ten", "Twenty", "Thirty", "Forty", "Fifty",
        "Sixty", "Seventy", "Eighty", "Ninety"};

    private static String[] englishOrdinalUnits = {
        "", "First", "Second", "Third", "Fourth", "Fifth", "Sixth", "Seventh", "Eighth", "Ninth",
        "Tenth", "Eleventh", "Twelfth", "Thirteenth", "Fourteenth", "Fifteenth", "Sixteenth",
        "Seventeenth", "Eighteenth", "Nineteenth"};

    private static String[] englishOrdinalTens = {
        "", "Tenth", "Twentieth", "Thirtieth", "Fortieth", "Fiftieth",
        "Sixtieth", "Seventieth", "Eightieth", "Ninetieth"};


    /**
     * Get a month name or abbreviation
     * @param month The month number (1=January, 12=December)
     * @param minWidth The minimum number of characters
     * @param maxWidth The maximum number of characters
     */

    public String monthName(int month, int minWidth, int maxWidth) {
        String name = englishMonths[month-1];
        if (maxWidth < 3) {
            maxWidth = 3;
        }
        if (name.length() > maxWidth) {
            name = name.substring(0, maxWidth);
        }
        while (name.length() < minWidth) {
            name = name + ' ';
        }
        return name;
    }

    private static String[] englishMonths = {
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    };

    /**
     * Get a day name or abbreviation
     * @param day The day of the week (1=Monday, 7=Sunday)
     * @param minWidth The minimum number of characters
     * @param maxWidth The maximum number of characters
     */

    public String dayName(int day, int minWidth, int maxWidth) {
        String name = englishDays[day-1];
        if (maxWidth < 2) {
            maxWidth = 2;
        }
        if (name.length() > maxWidth) {
            name = englishDayAbbreviations[day-1];
            if (name.length() > maxWidth) {
                name = name.substring(0, maxWidth);
            }
        }
        while (name.length() < minWidth) {
            name = name + ' ';
        }
        if (minWidth==1 && maxWidth==2) {
            // special case
            name = name.substring(0, minUniqueDayLength[day-1]);
        }
        return name;
    }

    private static String[] englishDays = {
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    };

    private static String[] englishDayAbbreviations = {
        "Mon", "Tues", "Weds", "Thurs", "Fri", "Sat", "Sun"
    };

    private static int[] minUniqueDayLength = {
        1, 2, 1, 2, 1, 2, 2
    };
    

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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

