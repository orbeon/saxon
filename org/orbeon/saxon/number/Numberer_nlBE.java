package org.orbeon.saxon.number;

/**
 * @author Karel Goossens
 *         BTR-Services Belgium.
 * Numberer class for the Belgian-Dutch language region.
 *
 *  @see <a href="http://woordenlijst.org/leidraad/6/9/#r6n">http://woordenlijst.org/leidraad/6/9/#r6n</a>
 *  @see <a href="http://taaladvies.net/taal/advies/popup.php?id=88">http://taaladvies.net/taal/advies/popup.php?id=88</a>
 *  @see <a href="http://www.vlaanderen.be/servlet/Satellite?c=Page&cid=1120536021990&pagename=taaltelefoon%2FPage%2FHomePageMIN">http://www.vlaanderen.be/servlet/Satellite?c=Page&cid=1120536021990&pagename=taaltelefoon%2FPage%2FHomePageMIN</a>
 */

public class Numberer_nlBE extends AbstractNumberer {

	private static final long serialVersionUID = 1L;

	private static String[] dutchOrdinalUnits = {
        "", "eenste", "tweede", "derde", "vierde", "vijfde", "zesde", "zevende", "achtste", "negende",
        "tiende", "elfde ", "twaalfde", "dertiende" , "veertiende", "vijftiende", "zestiende",
        "zeventiende", "achtiende", "negentiende"};

    private static String[] dutchOrdinalTens = {
        "", "tiende", "twintigste", "dertigste", "veertigste", "vijftigste",
        "zestigste", "zeventigste", "tachtigste", "negentigste"};

    private static String[] dutchUnits = {
        "", "een", "twee", "drie", "vier", "vijf", "zes", "zeven", "acht", "negen",
        "tien", "elf", "twaalf", "dertien", "veertien", "vijftien", "zestien",
        "zeventien", "achtien", "negentien"};

    private static String[] dutchTens = {
        "", "tien", "twintig", "dertig", "veertig", "vijftig",
        "zestig", "zeventig", "tachtig", "negentig"};

    /**
     * Show an ordinal number as dutch words in a requested case (for example, Twentyfirst)
     */

     public String toOrdinalWords(String ordinalParam, long number, int wordCase) {
         String s;
         if(number==1000000000){
        	 s="miljardste";
         }
         else if(number==1000000){
        	 s="miljoenste";
         }
         else if(number==1000){
        	 s="duizendste";
         }
         else if(number==100){
        	 s="honderste";
         }
         else if (number >= 1000000000) {

             long rem = number % 1000000000;
             s = toWords(number / 1000000000) + " miljard" +
                     (rem==0 ? "" : (rem < 100 ? " en " : " ") +
                     toOrdinalWords(ordinalParam, rem, wordCase));
         } else if (number >= 1000000) {

             long rem = number % 1000000;
             s = toWords(number / 1000000) + " miljoen" +
                     (rem==0 ? "" : (rem < 100 ? " en " : " ") +
                     toOrdinalWords(ordinalParam, rem, wordCase));
         } else if (number >= 1000) {

             long rem = number % 1000;
             s = (number/1000==1?"":toWords(number / 1000)) + "duizend" +" "+
                     toOrdinalWords(ordinalParam, rem, wordCase);
         } else if (number >= 100) {
             long rem = number % 100;
             s = (number/100==1?"":toWords(number / 100)) + "honderd" + toOrdinalWords(ordinalParam, rem, wordCase);
         } else {
             if (number < 20) {
                 s = dutchOrdinalUnits[(int)number];
             } else {
                 int rem = (int)(number % 10);
                 if (rem==0) {
                     s = dutchOrdinalTens[(int)number / 10];
                 } else {
                     s =  dutchUnits[rem]+(rem==2?"�n":"en")+dutchTens[(int)number / 10]+"ste";
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
             return toWords(number / 1000000000) + " miljard" +
                     (rem==0 ? "" : (rem < 100 ? "en" : " ") + toWords(rem));
         } else if (number >= 1000000) {
             long rem = number % 1000000;
             return toWords(number / 1000000) + " miljoen" +
                     (rem==0 ? "" : (rem < 100 ? "en" : " ") + toWords(rem));
         } else if (number >= 1000) {
             long rem = number % 1000;
             return (number/1000==1?"":toWords(number / 1000)) + "duizend" +
                     (rem==0 ? "" : (rem < 100 ? "en" : " ") + toWords(rem));
         } else if (number >= 100) {
             long rem = number % 100;
             return (number/100==1?"":toWords(number / 100)) + "honderd" + toWords(rem);
         } else {
             if (number < 20) return dutchUnits[(int)number];
             int rem = (int)(number % 10);
             return (rem==0 ? "" : dutchUnits[rem])+(rem==2?"�n":"en")+dutchTens[(int)number / 10] ;
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



    private static String[] dutchMonths = {
        "januari", "februari", "maart", "april", "mei", "juni",
        "juli", "augustus", "september", "oktober", "november", "december"
    };

    /**
     * Get a month name or abbreviation
     * @param month The month number (1=January, 12=December)
     * @param minWidth The minimum number of characters
     * @param maxWidth The maximum number of characters
     */

	//@Override
	public String monthName(int month, int minWidth, int maxWidth) {
		String name = dutchMonths[month-1];
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
        String name = dutchDays[day-1];
        if (maxWidth < 2) {
            maxWidth = 2;
        }
        if (name.length() > maxWidth) {
            name = dutchDayAbbreviations[day-1];
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

    private static String[] dutchDays = {
        "maandag", "dinsdag", "woensdag", "donderdag", "vrijdag", "zaterdag", "zondag"
    };

    private static String[] dutchDayAbbreviations = {
        "ma", "di", "woe", "do", "vrij", "zat", "zon"
    };

    private static int[] minUniqueDayLength = {
        1, 2, 1, 2, 1, 2, 2
    };

    /**
     * Get an am/pm indicator
     * @param minutes the minutes within the day
     * @param minWidth minimum width of output
     * @param maxWidth maximum width of output
     * @return the AM or PM indicator
     */

    public String halfDayName(int minutes, int minWidth, int maxWidth) {
        String s;
        if (minutes < 12*60) {
            switch (maxWidth) {
                case 1:
                    s = "v";
                    break;
                case 2:
                case 3:
                    s = "vm";
                    break;
                default:
                    s = "v.m.";
            }
        } else {
            switch (maxWidth) {
                case 1:
                    s = "n";
                    break;
                case 2:
                case 3:
                    s = "nm";
                    break;
                default:
                    s = "n.m.";
            }
        }
        return s;
    }

    /**
     * Get the name for an era (e.g. "BC" or "AD")
     *
     * @param year the proleptic gregorian year, using "0" for the year before 1AD
     */

    public String getEraName(int year) {
        return (year > 0 ? "n.C." : "v.C.");
    }



}
