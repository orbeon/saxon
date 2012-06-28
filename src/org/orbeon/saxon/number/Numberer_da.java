package org.orbeon.saxon.number;

/**
 * @author Karel Goossens
 *         BTR-Services Belgium.
 *
 *  Numberer class for the Danish language.
 *
 *  @see  "http://en.wikipedia.org/wiki/Danish_grammar#Numerals" 
 */
public class Numberer_da extends AbstractNumberer {

	private static final long serialVersionUID = 1L;

	private static String[] danishOrdinalUnits = {
        "", "f\u00f8rste", "anden", "tredje", "fjerde", "femte", "sjette", "syvende", "ottende", "niende",
        "tiende", "ellevte", "tolvte", "trettende" , "fjortende", "femtende", "sekstende",
        "syttende", "attende", "nittende"};

    private static String[] danishOrdinalTens = {
        "", "tiende", "tyvende", "tredivte", "fyrretyvende", "halvtredsindstyvende",
        "tresindstyvende", "halvfjerdsindstyvende", "firsindstyvende", "halvfemstyvende"};

    private static String[] danishUnits = {
        "", "et", "to", "tre", "fire", "fem", "seks", "syv", "otte", "ni",
        "ti", "elleve" /*or elvte*/, "tolv", "tretten", "fjorten", "femten", "seksten",
        "sytten", "atten", "nitten"};

    private static String[] danishTens = {
        "", "ti", "tyve", "tredive", "fyrre", "halvtreds",
        "tres", "halvfjerds", "firs", "halvfems"};

    /**
     * Show an ordinal number as Danish words in a requested case (for example, Twentyfirst)
     */

     public String toOrdinalWords(String ordinalParam, long number, int wordCase) {
         String s;
         if(number==1000000000){
        	 s="millardte";
         }
         else if(number==1000000){
        	 s="millonte";
         }
         else if(number==1000){
        	 s="tusinde";
         }
         else if(number==100){
        	 s="hundrede";
         } else if (number >= 1000000000) {
             long rem = number % 1000000000;
             s = (number / 1000000000==1?"en":toWords(number / 1000000000)) + " milliard " +
                     toOrdinalWords(ordinalParam, rem, wordCase);
         } else if (number >= 1000000) {
             long rem = number % 1000000;
             s = (number / 1000000==1?"en":toWords(number / 1000000)) + " million " +
                     toOrdinalWords(ordinalParam, rem, wordCase);
         } else if (number >= 1000) {
             long rem = number % 1000;
             s = (number/1000==1?"et":toWords(number / 1000)) + "tusind" +" "+
                     toOrdinalWords(ordinalParam, rem, wordCase);
         } else if (number >= 100) {
             long rem = number % 100;
             s = (number/100==1?"":toWords(number / 100)) + "hundred" +
                     (rem==0||rem > 19 ? "" : "en") +
                     toOrdinalWords(ordinalParam, rem, wordCase);
         } else {
             if (number < 20) {
                 s = danishOrdinalUnits[(int)number];
             } else {
                 int rem = (int)(number % 10);
                 if (rem==0) {
                     s = danishOrdinalTens[(int)number / 10];
                 } else {
                     s =  danishTens[(int)number / 10]+danishOrdinalUnits[rem];
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

     public String toWords(long number) {
         if (number >= 1000000000) {
             long rem = number % 1000000000;
             return (number / 1000000000==1?"en ":toWords(number / 1000000000)) + "milliard" +
                     (rem==0 ? "" : " ") + toWords(rem);
         } else if (number >= 1000000) {
             long rem = number % 1000000;
             return (number/1000000==1?"en ":toWords(number / 1000000)) + "million" +
                     (rem==0 ? "" :" ") + toWords(rem);
         } else if (number >= 1000) {
             long rem = number % 1000;
             return toWords(number / 1000) + "tusind" +
                     (rem==0 ? "" : " ") + toWords(rem);
         } else if (number >= 100) {
             long rem = number % 100;
             return toWords(number / 100) + "hundred" + (rem>0?"og"+toWords(rem):"");
         } else {
             if (number < 20) return danishUnits[(int)number];
             int rem = (int)(number % 10);
             return danishUnits[rem]+"og"+danishTens[(int)number / 10] ;
         }
     }

     public String toWords(long number, int wordCase) {
         String s;
         if (number == 0) {
             s = "nul";
         } else {
             s = toWords(number);
         }
         if (wordCase == UPPER_CASE) {
             return s.toUpperCase();
         } else if (wordCase == LOWER_CASE) {
             return s.toLowerCase();
         } else {
             return s;
         }
     }



    private static String[] swedishMonths = {
        "januar", "februar", "marts", "april", "maj", "juni",
        "juli", "august", "september", "oktober", "november", "december"
    };

    /**
     * Get a month name or abbreviation
     * @param month The month number (1=January, 12=December)
     * @param minWidth The minimum number of characters
     * @param maxWidth The maximum number of characters
     */

	//@Override
	public String monthName(int month, int minWidth, int maxWidth) {
		String name = swedishMonths[month-1];
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

	/**
     * Get a day name or abbreviation
     * @param day The day of the week (1=Monday, 7=Sunday)
     * @param minWidth The minimum number of characters
     * @param maxWidth The maximum number of characters
     */

    public String dayName(int day, int minWidth, int maxWidth) {
        String name = danishDays[day-1];
        if (maxWidth < 2) {
            maxWidth = 2;
        }
        if (name.length() > maxWidth) {
            name = danishDayAbbreviations[day-1];
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

    private static String[] danishDays = {
        "mandag", "tirsdag", "onsdag", "torsdag", "fredag", "l\u00f8rdag", "s\u00f8ndag"
    };

    private static String[] danishDayAbbreviations = {
        "ma", "ti", "on", "to", "fr", "l\u00f8", "s\u00f8"
    };

    private static int[] minUniqueDayLength = {
        1, 2, 1, 2, 1, 2, 2
    };

}
//
//The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
//you may not use this file except in compliance with the License. You may obtain a copy of the
//License at http://www.mozilla.org/MPL/
//
//Software distributed under the License is distributed on an "AS IS" basis,
//WITHOUT WARRANTY OF ANY KIND, either express or implied.
//See the License for the specific language governing rights and limitations under the License.
//
//The Original Code is: all this file.
//
//The Initial Developer of the Original Code is Michael H. Kay.
//
//Portions created by Karel Goossens are Copyright (C) BTR-services inc. All Rights Reserved.
//
//Contributor(s): Karel Goossens.
//