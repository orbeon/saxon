package org.orbeon.saxon.number;

/**
 * Localization class for Italian
 * @author Karel Goossens
 *         BTR-Services Belgium.
 * Numberer class for the Italian language.
 * @see <a href="http://italian.about.com/library/weekly/aa042600a.htm">Italian numbers</a>
 * @see <a href="http://home.unilang.org/wiki3/index.php/Italian_months">Italian months</a>
 * @see <a href="http://home.unilang.org/wiki3/index.php/Italian_days">Italian days</a>
 */

public class Numberer_it extends AbstractNumberer {

	private static final long serialVersionUID = 1L;

	private static String[] italianOrdinalUnits = {
        "", "primo", "secondo", "terzo", "quarto", "quinto", "sesto", "settimo", "ottavo", "nono",
        "decimo", "undicesimo ", "dodicesimo", "tredicesimo" , "quattordicesimo", "quindiczesimo", "sedicesimo",
        "diciassettesimo", "diciottesimo", "novantesimo"};

    private static String[] italianOrdinalTens = {
        "", "decimo", "ventesimo", "trentesimo", "quarantesimo", "cinquantesimo",
        "sessantesimo", "settantesimo", "ottantesimo", "novantesimo"};

    private static String[] italianUnits = {
        "", "uno", "due", "tre", "quattro", "cinque", "sei", "sette", "otto", "nove",
        "dieci", "undici", "dodici", "tredici", "quattordici", "quindici", "sedici",
        "diciassette", "diciotto", "diciannove"};

    private static String[] italianTens = {
        "", "dieci", "venti", "trenta", "quaranta", "cinquanta",
        "sessanta", "settanta", "ottanta", "novanta"};

    /**
     * Show an ordinal number as Italian words in a requested case (for example, Twentyfirst)
     */

     public String toOrdinalWords(String ordinalParam, long number, int wordCase) {
         String s;
         /* there is no such thing as zero-est */
         if(number==0 && !"notNull".equalsIgnoreCase(ordinalParam))return "";
         ordinalParam="notNull";
         if (number >= 1000000000) {
             long rem = number % 1000000000;
             long num = number / 1000000000;
             s = (num==1?"un":toWords(num)) +
                     (rem==0 ?" miliardesimo":(num==1?" miliardo ":" miliardi ") +toOrdinalWords(ordinalParam, rem, wordCase));
         } else if (number >= 1000000) {
             long rem = number % 1000000;
             long num = number /1000000;
             s = (num==1?"un":toWords(num)) +
             (rem==0?" milionesimo":(num==1?" milione ":" milioni ") + toOrdinalWords(ordinalParam, rem, wordCase));
         } else if (number >= 1000) {
        	 if(number==100000){
        		 s="centomillesimo";
        	 }
        	 else if(number==10000){
        		 s="diecimillesimo";
        	 }
        	 else {
        		 long rem = number % 1000;
                 long num = number/1000;
            	 s = (num==1?"":toWords(num)) +
            	 (rem==0?"millesimo":(num==1?"mille":"mila") + toOrdinalWords(ordinalParam, rem, wordCase));
        	 }
         } else if (number >= 100) {
             int rem = (int)number % 100;
             int num = (int)number / 100;
             if(number==100){
            	 s="centesimo";
             }
             else{
            	 if(rem==0&&num!=1){
            		 s=toWords(num)+"centesimo";
            	 }
            	 else{
            		s=(num==1?"":toWords(num))+"cento"+ toOrdinalWords(ordinalParam, rem, wordCase);
            	 }
             }
         } else {
             if (number < 20) {
                 s = italianOrdinalUnits[(int)number];
             } else {
                 int rem = (int)(number % 10);
                 if (rem==0) {
                     s = italianOrdinalTens[(int)number / 10];
                 } else {
                	 s = italianTens[(int)number / 10];
                	 switch (rem){
                	 case 1:s= s.substring(0, s.length()-1)+"unesimo";
                	 break;
                	 case 3:
                	 case 6: s=s+italianUnits[rem]+"esimo";
                	   break;
                	 default:
                		 s=s+italianUnits[rem].substring(0,italianUnits[rem].length()-1)+"esimo";
                	 }
                 }
             }
         }
         s=s.toLowerCase();
         s=s.replaceAll("centoun", "centun");
         if (wordCase == UPPER_CASE) {
             s=s.toUpperCase();
         }
         return s;
     }

     public String toWords(long number) {
         if (number >= 1000000000) {
             long rem = number % 1000000000;
             long num = number / 1000000000;
             return (num==1?"un miliardo":toWords(num) + " miliardi" )+
                     (rem==0 ? "" : " ") + toWords(rem);
         } else if (number >= 1000000) {
             long rem = number % 1000000;
             long num = number / 1000000;
             return (num==1?"un milione":toWords(num) + " milioni") +
                     (rem==0 ? "" : " ") + toWords(rem);
         } else if (number >= 1000) {
             long rem = number % 1000;
             long num = number / 1000;
             return (num==1?"mille":toWords(num)+"mila") + toWords(rem);
         } else if (number >= 100) {
             long rem = number % 100;
             return (number/100==1?"":toWords(number / 100)) + "cento" + toWords(rem);
         } else {
             if (number < 20){
            	 return italianUnits[(int)number];
             }
             int rem = (int)(number % 10);
             String s=italianTens[(int)number / 10];
             switch(rem){
             case 0: return italianTens[(int)number / 10];
             case 1:
             case 8: return s.substring(0,s.length()-1)+italianUnits[rem];
             case 3: return s+"tr\u00e9";
             default: return s+italianUnits[rem];
             }
         }
     }

     public String toWords(long number, int wordCase) {
         String s;
         if (number == 0) {
             s = "zero";
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


    
    private static String[] italianMonths = {
        "gennaio", "febbraio", "marzo", "aprile", "maggio", "giugno",
        "luglio", "agosto", "settembre", "ottobre", "novembre", "dicembre"
    };

    private static String[] italianMonthAbbreviations = {
        "gen", "feb", "mar", "apr", "mag", "giu",
        "lug", "ago", "set", "ott", "nov", "dic"
    };

    /**
     * Get a month name or abbreviation
     * @param month The month number (1=January, 12=December)
     * @param minWidth The minimum number of characters
     * @param maxWidth The maximum number of characters
     */

	//@Override
	public String monthName(int month, int minWidth, int maxWidth) {
		String name = italianMonths[month-1];
        if (maxWidth < 3) {
            maxWidth = 3;
        }
        if (name.length() > maxWidth) {
            name = italianMonthAbbreviations[month-1];
            if (name.length() > maxWidth) {
                name = name.substring(0, maxWidth);
            }
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
        String name = italianDays[day-1];
        if (maxWidth < 2) {
            maxWidth = 2;
        }
        if (name.length() > maxWidth) {
            name = italianDayAbbreviations[day-1];
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

    private static String[] italianDays = {
        "luned\u00ec", "marted\u00ec ", "mercoled\u00ec", "gioved\u00ec", "venerd\u00ec", "sabato", "domenica"
    };

    private static String[] italianDayAbbreviations = {
        "lun", "mar", "mer", "gio", "ven", "sab", "dom"
    };

    /*@NotNull*/ private static int[] minUniqueDayLength = {
        1, 2, 2, 1, 1, 1, 1
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
// The Initial Developer of the Original Code is Karel Goossens, BTR-Services, Belgium.
// Portions created by ___ are Copyright (C) ___. All rights reserved.
//
// Contributor(s): Saxonica Limited
//
