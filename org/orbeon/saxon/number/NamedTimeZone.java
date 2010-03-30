package org.orbeon.saxon.number;

import org.orbeon.saxon.value.DateTimeValue;
import org.orbeon.saxon.om.FastStringBuffer;

import java.util.*;

/**
 * This class attempts to identify a timezone name, given the date (including the time zone offset)
 * and the country. The process is heuristic: sometimes there is more than one timezone that matches
 * this information, sometimes there is none, but the class simply does its best. This is in support
 * of the XSLT format-date() function.
 */
public class NamedTimeZone {

    static HashMap idForCountry = new HashMap(50);

    /**
     * Register a timezone in use in a particular country. Note that some countries use multiple
     * time zones
     * @param country the two-character code for the country
     * @param zoneId the Olsen timezone name for the timezone
     */

    static void tz(String country, String zoneId) {
        List list = (List)idForCountry.get(country);
        if (list == null) {
            list = new ArrayList(4);
        }
        list.add(zoneId);
        idForCountry.put(country, list);
    }

    static {

        // The table starts with countries that use multiple timezones, then proceeds in alphabetical order
        
        tz("us", "America/New_York");
        tz("us", "America/Chicago");
        tz("us", "America/Denver");
        tz("us", "America/Los_Angeles");
        tz("us", "America/Anchorage");
        tz("us", "America/Halifax");

        tz("ca", "Canada/Pacific");
        tz("ca", "Canada/Mountain");
        tz("ca", "Canada/Central");
        tz("ca", "Canada/Eastern");
        tz("ca", "Canada/Atlantic");

        tz("au", "Australia/Sydney");
        tz("au", "Australia/Darwin");
        tz("au", "Australia/Perth");

        tz("ru", "Europe/Moscow");
        tz("ru", "Europe/Samara");
        tz("ru", "Asia/Yekaterinburg");
        tz("ru", "Asia/Novosibirsk");
        tz("ru", "Asia/Krasnoyarsk");
        tz("ru", "Asia/Irkutsk");
        tz("ru", "Asia/Chita");
        tz("ru", "Asia/Vladivostok");

        tz("an", "Europe/Andorra");
        tz("ae", "Asia/Abu_Dhabi");
        tz("af", "Asia/Kabul");
        tz("al", "Europe/Tirana");
        tz("am", "Asia/Yerevan");
        tz("ao", "Africa/Luanda");
        tz("ar", "America/Buenos_Aires");
        tz("as", "Pacific/Samoa");
        tz("at", "Europe/Vienna");
        tz("aw", "America/Aruba");
        tz("az", "Asia/Baku");

        tz("ba", "Europe/Sarajevo");
        tz("bb", "America/Barbados");
        tz("bd", "Asia/Dhaka");
        tz("be", "Europe/Brussels");
        tz("bf", "Africa/Ouagadougou");
        tz("bg", "Europe/Sofia");
        tz("bh", "Asia/Bahrain");
        tz("bi", "Africa/Bujumbura");
        tz("bm", "Atlantic/Bermuda");
        tz("bn", "Asia/Brunei");
        tz("bo", "America/La_Paz");
        tz("br", "America/Sao_Paulo");
        tz("bs", "America/Nassau");
        tz("bw", "Gaborone");
        tz("by", "Europe/Minsk");
        tz("bz", "America/Belize");

        tz("cd", "Africa/Kinshasa");
        tz("ch", "Europe/Zurich");
        tz("ci", "Africa/Abidjan");
        tz("cl", "America/Santiago");
        tz("cn", "Asia/Shanghai");
        tz("co", "America/Bogota");
        tz("cr", "America/Costa_Rica");
        tz("cu", "America/Cuba");
        tz("cv", "Atlantic/Cape_Verde");
        tz("cy", "Asia/Nicosia");
        tz("cz", "Europe/Prague");

        tz("de", "Europe/Berlin");
        tz("dj", "Africa/Djibouti");
        tz("dk", "Europe/Copenhagen");
        tz("do", "America/Santo_Domingo");
        tz("dz", "Africa/Algiers");

        tz("ec", "America/Quito");
        tz("ee", "Europe/Tallinn");
        tz("eg", "Africa/Cairo");
        tz("er", "Africa/Asmara");
        tz("es", "Europe/Madrid");

        tz("fi", "Europe/Helsinki");
        tz("fj", "Pacific/Fiji");
        tz("fk", "America/Stanley");
        tz("fr", "Europe/Paris");
        
        tz("ga", "Africa/Libreville");
        tz("gb", "Europe/London");
        tz("gd", "America/Grenada");
        tz("ge", "Asia/Tbilisi");
        tz("gh", "Africa/Accra");
        tz("gm", "Africa/Banjul");
        tz("gn", "Africa/Conakry");
        tz("gr", "Europe/Athens");
        tz("gy", "America/Guyana");
        
        tz("hk", "Asia/Hong_Kong");
        tz("hn", "America/Tegucigalpa");
        tz("hr", "Europe/Zagreb");
        tz("ht", "America/Port-au-Prince");
        tz("hu", "Europe/Budapest");

        tz("id", "Asia/Jakarta");
        tz("ie", "Europe/Dublin");
        tz("il", "Asia/Tel_Aviv");
        tz("in", "Asia/Calcutta");
        tz("iq", "Asia/Baghdad");
        tz("ir", "Asia/Tehran");
        tz("is", "Atlantic/Reykjavik");
        tz("it", "Europe/Rome");

        tz("jm", "America/Jamaica");
        tz("jo", "Asia/Amman");
        tz("jp", "Asia/Tokyo");

        tz("ke", "Africa/Nairobi");
        tz("kg", "Asia/Bishkek");
        tz("kh", "Asia/Phnom_Penh");
        tz("kp", "Asia/Pyongyang");
        tz("kr", "Asia/Seoul");
        tz("kw", "Asia/Kuwait");

        tz("lb", "Asia/Beirut");
        tz("li", "Europe/Liechtenstein");
        tz("lk", "Asia/Colombo");
        tz("lr", "Africa/Monrovia");
        tz("ls", "Africa/Maseru");
        tz("lt", "Europe/Vilnius");
        tz("lu", "Europe/Luxembourg");
        tz("lv", "Europe/Riga");
        tz("ly", "Africa/Tripoli");

        tz("ma", "Africa/Rabat");
        tz("mc", "Europe/Monaco");
        tz("md", "Europe/Chisinau");
        tz("mg", "Indian/Antananarivo");
        tz("mk", "Europe/Skopje");
        tz("ml", "Africa/Bamako");
        tz("mm", "Asia/Rangoon");
        tz("mn", "Asia/Ulaanbaatar");
        tz("mo", "Asia/Macao");
        tz("mq", "America/Martinique");
        tz("mt", "Europe/Malta");
        tz("mu", "Indian/Mauritius");
        tz("mv", "Indian/Maldives");
        tz("mw", "Africa/Lilongwe");
        tz("mx", "America/Mexico_City");
        tz("my", "Asia/Kuala_Lumpur");

        tz("na", "Africa/Windhoek");
        tz("ne", "Africa/Niamey");
        tz("ng", "Africa/Lagos");
        tz("ni", "America/Managua");
        tz("nl", "Europe/Amsterdam");
        tz("no", "Europe/Oslo");
        tz("np", "Asia/Kathmandu");
        tz("nz", "Pacific/Aukland");

        tz("om", "Asia/Muscat");

        tz("pa", "America/Panama");
        tz("pe", "America/Lima");
        tz("pg", "Pacific/Port_Moresby");
        tz("ph", "Asia/Manila");
        tz("pk", "Asia/Karachi");
        tz("pl", "Europe/Warsaw");
        tz("pr", "America/Puerto_Rico");
        tz("pt", "Europe/Lisbon");
        tz("py", "America/Asuncion");

        tz("qa", "Asia/Qatar");

        tz("ro", "Europe/Bucharest");
        tz("rs", "Europe/Belgrade");

        tz("rw", "Africa/Kigali");

        tz("sa", "Asia/Riyadh");
        tz("sd", "Africa/Khartoum");
        tz("se", "Europe/Stockholm");
        tz("sg", "Asia/Singapore");
        tz("si", "Europe/Ljubljana");
        tz("sk", "Europe/Bratislava");
        tz("sl", "Africa/Freetown");
        tz("so", "Africa/Mogadishu");
        tz("sr", "America/Paramaribo");
        tz("sv", "America/El_Salvador");
        tz("sy", "Asia/Damascus");
        tz("sz", "Africa/Mbabane");
       
        tz("td", "Africa/Ndjamena");
        tz("tg", "Africa/Lome");
        tz("th", "Asia/Bangkok");
        tz("tj", "Asia/Dushanbe");
        tz("tm", "Asia/Ashgabat");
        tz("tn", "Africa/Tunis");
        tz("to", "Pacific/Tongatapu");
        tz("tr", "Asia/Istanbul");
        tz("tw", "Asia/Taipei");
        tz("tz", "Africa/Dar_es_Salaam");

        tz("ua", "Europe/Kiev");
        tz("ug", "Africa/Kampala");
        tz("uk", "Europe/London");
        tz("uy", "America/Montevideo");
        tz("uz", "Asia/Tashkent");

        tz("ve", "America/Caracas");
        tz("vn", "Asia/Hanoi");

        tz("za", "Africa/Johannesburg");
        tz("zm", "Africa/Lusaka");
        tz("zw", "Africa/Harare");


    }

    /**
     * Try to identify a timezone name corresponding to a given date (including time zone)
     * and a given country. Note that this takes account of Java's calendar of daylight savings time
     * changes in different countries. The returned value is the convenional short timezone name, for example
     * PDT for Pacific Daylight Time
     * @param date the dateTimeValue, including timezone
     * @param country the two-letter ISO country code
     * @return the short timezone name if a timezone with the given time displacement is in use in the country
     * in question (on the appropriate date, if known). Otherwise, the formatted (numeric) timezone offset. If
     * the dateTimeValue supplied has no timezone, return a zero-length string.
     */

    public static String getTimeZoneNameForDate(DateTimeValue date, String country) {
        if (!date.hasTimezone()) {
            return "";
        }
        if (country == null) {
            return formatTimeZoneOffset(date);
        }
        List possibleIds = (List)idForCountry.get(country.toLowerCase());
        String exampleId;
        if (possibleIds == null) {
            return formatTimeZoneOffset(date);
        } else {
            exampleId = (String)possibleIds.get(0);
        }
        TimeZone exampleZone = TimeZone.getTimeZone(exampleId);
        Date javaDate = null;
        try {
            javaDate = date.getCalendar().getTime();
        } catch (IllegalArgumentException e) {
            // this happens with timezones that are allowed in XPath but not in Java, especially on JDK 1.4
            return formatTimeZoneOffset(date);
        }
        boolean inSummerTime = exampleZone.inDaylightTime(javaDate);
        int tzMinutes = date.getTimezoneInMinutes();
        for (int i=0; i<possibleIds.size(); i++) {
            TimeZone possibleTimeZone = TimeZone.getTimeZone((String)possibleIds.get(i));
            int offset = possibleTimeZone.getOffset(javaDate.getTime());
            if (offset == tzMinutes*60000) {
                return possibleTimeZone.getDisplayName(inSummerTime, TimeZone.SHORT);
            }
        }
        return formatTimeZoneOffset(date);
    }

    /**
     * Format a timezone in numeric form for example +03:00 (or Z for +00:00)
     * @param timeValue the value whose timezone is to be formatted
     * @return the formatted timezone
     */

    public static String formatTimeZoneOffset(DateTimeValue timeValue) {
        FastStringBuffer sb = new FastStringBuffer(10);
        DateTimeValue.appendTimezone(timeValue.getTimezoneInMinutes(), sb);
        return sb.toString();
    }

    /**
     * Try to identify a timezone name corresponding to a given date (including time zone)
     * and a given country. Note that this takes account of Java's calendar of daylight savings time
     * changes in different countries. The returned value is the Olsen time zone name, for example
     * "Pacific/Los_Angeles", followed by an asterisk (*) if the time is in daylight savings time in that
     * timezone.
     * @param date the dateTimeValue, including timezone
     * @param country the country, as a two-letter code
     * @return the Olsen timezone name if a timezone with the given time displacement is in use in the country
     * in question (on the appropriate date, if known). In this case an asterisk is appended to the result if the
     * date/time is in daylight savings time. Otherwise, the formatted (numeric) timezone offset. If
     * the dateTimeValue supplied has no timezone, return a zero-length string.
     */

    public static String getOlsenTimeZoneName(DateTimeValue date, String country) {
        if (!date.hasTimezone()) {
            return "";
        }
        List possibleIds = (List)idForCountry.get(country.toLowerCase());
        String exampleId;
        if (possibleIds == null) {
            return formatTimeZoneOffset(date);
        } else {
            exampleId = (String)possibleIds.get(0);
        }
        TimeZone exampleZone = TimeZone.getTimeZone(exampleId);
        Date javaDate = date.getCalendar().getTime();
        boolean inSummerTime = exampleZone.inDaylightTime(javaDate);
        int tzMinutes = date.getTimezoneInMinutes();
        for (int i=0; i<possibleIds.size(); i++) {
            String olsen = (String)possibleIds.get(i);
            TimeZone possibleTimeZone = TimeZone.getTimeZone(olsen);
            int offset = possibleTimeZone.getOffset(javaDate.getTime());
            if (offset == tzMinutes*60000) {
                return inSummerTime ? olsen + "*" : olsen;
            }
        }
        return formatTimeZoneOffset(date);
    }

    /**
     * Determine whether a given date/time is in summer time (daylight savings time)
     * in a given region. This relies on the Java database of changes to daylight savings time.
     * Since summer time changes are set by civil authorities the information is not necessarily
     * reliable when applied to dates in the future.
     * @param date the date/time in question
     * @param region either the two-letter ISO country code, or an Olsen timezone name such as
     * "America/New_York" or "Europe/Lisbon". If the country code denotes a country spanning several
     * timezones, such as the US, then one of them is chosen arbitrarily.
     * @return true if the date/time is known to be in summer time in the relevant country;
     * false if it is known not to be in summer time; null if there is no timezone or if no
     * information is available for the specified region. 
     */

    public static Boolean inSummerTime(DateTimeValue date, String region) {
        String olsenName;
        if (region.length() == 2) {
            List possibleIds = (List)idForCountry.get(region.toLowerCase());
            if (possibleIds == null) {
                return null;
            } else {
                olsenName = (String)possibleIds.get(0);
            }
        } else {
            olsenName = region;
        }
        TimeZone zone = TimeZone.getTimeZone(olsenName);
        return Boolean.valueOf(zone.inDaylightTime(date.getCalendar().getTime()));
    }


    /**
     * Main method for testing
     * @param args first argument: a dateTime value
     *             second argument: a country code
     */

//    public static void main(String[] args) {
//        System.err.println(NamedTimeZone.getTimeZoneNameForDate((DateTimeValue)DateTimeValue.makeDateTimeValue(args[0]), args[1]));
//    }

    /**
     * Main method to generate the list of timezone names known to Java
     * @param args not used
     */

//    public static void main(String[] args) {
//        String[] ids = TimeZone.getAvailableIDs(/* -5*60*60*1000 */);
//        for (int i=0; i<ids.length; i++) {
//            System.err.println(ids[i] + " - " + TimeZone.getTimeZone(ids[i]).getDisplayName(true, TimeZone.SHORT) +
//            " - " + TimeZone.getTimeZone(ids[i]).getDisplayName(false, TimeZone.SHORT));
//        }
//    }
}

/*
Etc/GMT+12 - GMT-12:00 - GMT-12:00
Etc/GMT+11 - GMT-11:00 - GMT-11:00
MIT - WST - WST
Pacific/Apia - WST - WST
Pacific/Midway - SST - SST
Pacific/Niue - NUT - NUT
Pacific/Pago_Pago - SST - SST
Pacific/Samoa - SST - SST
US/Samoa - SST - SST
America/Adak - HADT - HAST
America/Atka - HADT - HAST
Etc/GMT+10 - GMT-10:00 - GMT-10:00
HST - HST - HST
Pacific/Fakaofo - TKT - TKT
Pacific/Honolulu - HST - HST
Pacific/Johnston - HST - HST
Pacific/Rarotonga - CKT - CKT
Pacific/Tahiti - TAHT - TAHT
SystemV/HST10 - HST - HST
US/Aleutian - HADT - HAST
US/Hawaii - HST - HST
Pacific/Marquesas - MART - MART
AST - AKDT - AKST
America/Anchorage - AKDT - AKST
America/Juneau - AKDT - AKST
America/Nome - AKDT - AKST
America/Yakutat - AKDT - AKST
Etc/GMT+9 - GMT-09:00 - GMT-09:00
Pacific/Gambier - GAMT - GAMT
SystemV/YST9 - AKST - AKST
SystemV/YST9YDT - AKDT - AKST
US/Alaska - AKDT - AKST
America/Dawson - PDT - PST
America/Ensenada - PDT - PST
America/Los_Angeles - PDT - PST
America/Tijuana - PDT - PST
America/Vancouver - PDT - PST
America/Whitehorse - PDT - PST
Canada/Pacific - PDT - PST
Canada/Yukon - PDT - PST
Etc/GMT+8 - GMT-08:00 - GMT-08:00
Mexico/BajaNorte - PDT - PST
PST - PDT - PST
PST8PDT - PDT - PST
Pacific/Pitcairn - PST - PST
SystemV/PST8 - PST - PST
SystemV/PST8PDT - PDT - PST
US/Pacific - PDT - PST
US/Pacific-New - PDT - PST
America/Boise - MDT - MST
America/Cambridge_Bay - MDT - MST
America/Chihuahua - MDT - MST
America/Dawson_Creek - MST - MST
America/Denver - MDT - MST
America/Edmonton - MDT - MST
America/Hermosillo - MST - MST
America/Inuvik - MDT - MST
America/Mazatlan - MDT - MST
America/Phoenix - MST - MST
America/Shiprock - MDT - MST
America/Yellowknife - MDT - MST
Canada/Mountain - MDT - MST
Etc/GMT+7 - GMT-07:00 - GMT-07:00
MST - MST - MST
MST7MDT - MDT - MST
Mexico/BajaSur - MDT - MST
Navajo - MDT - MST
PNT - MST - MST
SystemV/MST7 - MST - MST
SystemV/MST7MDT - MDT - MST
US/Arizona - MST - MST
US/Mountain - MDT - MST
America/Belize - CST - CST
America/Cancun - CDT - CST
America/Chicago - CDT - CST
America/Costa_Rica - CST - CST
America/El_Salvador - CST - CST
America/Guatemala - CST - CST
America/Indiana/Knox - CDT - CST
America/Indiana/Petersburg - CDT - CST
America/Indiana/Vincennes - CDT - CST
America/Knox_IN - EDT - EST
America/Managua - CST - CST
America/Menominee - CDT - CST
America/Merida - CDT - CST
America/Mexico_City - CDT - CST
America/Monterrey - CDT - CST
America/North_Dakota/Center - CDT - CST
America/North_Dakota/New_Salem - CDT - CST
America/Rainy_River - CDT - CST
America/Rankin_Inlet - CDT - CST
America/Regina - CST - CST
America/Swift_Current - CST - CST
America/Tegucigalpa - CST - CST
America/Winnipeg - CDT - CST
CST - CDT - CST
CST6CDT - CDT - CST
Canada/Central - CDT - CST
Canada/East-Saskatchewan - CST - CST
Canada/Saskatchewan - CST - CST
Chile/EasterIsland - EASST - EAST
Etc/GMT+6 - GMT-06:00 - GMT-06:00
Mexico/General - CDT - CST
Pacific/Easter - EASST - EAST
Pacific/Galapagos - GALT - GALT
SystemV/CST6 - CST - CST
SystemV/CST6CDT - CDT - CST
US/Central - CDT - CST
US/Indiana-Starke - EDT - EST
America/Bogota - COT - COT
America/Cayman - EST - EST
America/Coral_Harbour - EST - EST
America/Detroit - EDT - EST
America/Eirunepe - ACT - ACT
America/Fort_Wayne - EDT - EST
America/Grand_Turk - EDT - EST
America/Guayaquil - ECT - ECT
America/Havana - CDT - CST
America/Indiana/Indianapolis - EDT - EST
America/Indiana/Marengo - EDT - EST
America/Indiana/Vevay - EDT - EST
America/Indianapolis - EDT - EST
America/Iqaluit - EDT - EST
America/Jamaica - EST - EST
America/Kentucky/Louisville - EDT - EST
America/Kentucky/Monticello - EDT - EST
America/Lima - PET - PET
America/Louisville - EDT - EST
America/Montreal - EDT - EST
America/Nassau - EDT - EST
America/New_York - EDT - EST
America/Nipigon - EDT - EST
America/Panama - EST - EST
America/Pangnirtung - EDT - EST
America/Port-au-Prince - EDT - EST
America/Porto_Acre - ACT - ACT
America/Rio_Branco - ACT - ACT
America/Thunder_Bay - EDT - EST
America/Toronto - EDT - EST
Brazil/Acre - ACT - ACT
Canada/Eastern - EDT - EST
Cuba - CDT - CST
EST - EST - EST
EST5EDT - EDT - EST
Etc/GMT+5 - GMT-05:00 - GMT-05:00
IET - EDT - EST
Jamaica - EST - EST
SystemV/EST5 - EST - EST
SystemV/EST5EDT - EDT - EST
US/East-Indiana - EDT - EST
US/Eastern - EDT - EST
US/Michigan - EDT - EST
America/Anguilla - AST - AST
America/Antigua - AST - AST
America/Aruba - AST - AST
America/Asuncion - PYST - PYT
America/Barbados - AST - AST
America/Boa_Vista - AMT - AMT
America/Campo_Grande - AMST - AMT
America/Caracas - VET - VET
America/Cuiaba - AMST - AMT
America/Curacao - AST - AST
America/Dominica - AST - AST
America/Glace_Bay - ADT - AST
America/Goose_Bay - ADT - AST
America/Grenada - AST - AST
America/Guadeloupe - AST - AST
America/Guyana - GYT - GYT
America/Halifax - ADT - AST
America/La_Paz - BOT - BOT
America/Manaus - AMT - AMT
America/Martinique - AST - AST
America/Moncton - ADT - AST
America/Montserrat - AST - AST
America/Port_of_Spain - AST - AST
America/Porto_Velho - AMT - AMT
America/Puerto_Rico - AST - AST
America/Santiago - CLST - CLT
America/Santo_Domingo - AST - AST
America/St_Kitts - AST - AST
America/St_Lucia - AST - AST
America/St_Thomas - AST - AST
America/St_Vincent - AST - AST
America/Thule - ADT - AST
America/Tortola - AST - AST
America/Virgin - AST - AST
Antarctica/Palmer - CLST - CLT
Atlantic/Bermuda - ADT - AST
Atlantic/Stanley - FKST - FKT
Brazil/West - AMT - AMT
Canada/Atlantic - ADT - AST
Chile/Continental - CLST - CLT
Etc/GMT+4 - GMT-04:00 - GMT-04:00
PRT - AST - AST
SystemV/AST4 - AST - AST
SystemV/AST4ADT - ADT - AST
America/St_Johns - NDT - NST
CNT - NDT - NST
Canada/Newfoundland - NDT - NST
AGT - ART - ART
America/Araguaina - BRT - BRT
America/Argentina/Buenos_Aires - ART - ART
America/Argentina/Catamarca - ART - ART
America/Argentina/ComodRivadavia - ART - ART
America/Argentina/Cordoba - ART - ART
America/Argentina/Jujuy - ART - ART
America/Argentina/La_Rioja - ART - ART
America/Argentina/Mendoza - ART - ART
America/Argentina/Rio_Gallegos - ART - ART
America/Argentina/San_Juan - ART - ART
America/Argentina/Tucuman - ART - ART
America/Argentina/Ushuaia - ART - ART
America/Bahia - BRT - BRT
America/Belem - BRT - BRT
America/Buenos_Aires - ART - ART
America/Catamarca - ART - ART
America/Cayenne - GFT - GFT
America/Cordoba - ART - ART
America/Fortaleza - BRT - BRT
America/Godthab - WGST - WGT
America/Jujuy - ART - ART
America/Maceio - BRT - BRT
America/Mendoza - ART - ART
America/Miquelon - PMDT - PMST
America/Montevideo - UYT - UYT
America/Paramaribo - SRT - SRT
America/Recife - BRT - BRT
America/Rosario - ART - ART
America/Sao_Paulo - BRST - BRT
Antarctica/Rothera - ROTT - ROTT
BET - BRST - BRT
Brazil/East - BRST - BRT
Etc/GMT+3 - GMT-03:00 - GMT-03:00
America/Noronha - FNT - FNT
Atlantic/South_Georgia - GST - GST
Brazil/DeNoronha - FNT - FNT
Etc/GMT+2 - GMT-02:00 - GMT-02:00
America/Scoresbysund - EGST - EGT
Atlantic/Azores - AZOST - AZOT
Atlantic/Cape_Verde - CVT - CVT
Etc/GMT+1 - GMT-01:00 - GMT-01:00
Africa/Abidjan - GMT - GMT
Africa/Accra - GMT - GMT
Africa/Bamako - GMT - GMT
Africa/Banjul - GMT - GMT
Africa/Bissau - GMT - GMT
Africa/Casablanca - WET - WET
Africa/Conakry - GMT - GMT
Africa/Dakar - GMT - GMT
Africa/El_Aaiun - WET - WET
Africa/Freetown - GMT - GMT
Africa/Lome - GMT - GMT
Africa/Monrovia - GMT - GMT
Africa/Nouakchott - GMT - GMT
Africa/Ouagadougou - GMT - GMT
Africa/Sao_Tome - GMT - GMT
Africa/Timbuktu - GMT - GMT
America/Danmarkshavn - GMT - GMT
Atlantic/Canary - WEST - WET
Atlantic/Faeroe - WEST - WET
Atlantic/Madeira - WEST - WET
Atlantic/Reykjavik - GMT - GMT
Atlantic/St_Helena - GMT - GMT
Eire - IST - GMT
Etc/GMT - GMT+00:00 - GMT+00:00
Etc/GMT+0 - GMT+00:00 - GMT+00:00
Etc/GMT-0 - GMT+00:00 - GMT+00:00
Etc/GMT0 - GMT+00:00 - GMT+00:00
Etc/Greenwich - GMT - GMT
Etc/UCT - UTC - UTC
Etc/UTC - UTC - UTC
Etc/Universal - UTC - UTC
Etc/Zulu - UTC - UTC
Europe/Belfast - BST - GMT
Europe/Dublin - IST - GMT
Europe/Lisbon - WEST - WET
Europe/London - BST - GMT
GB - BST - GMT
GB-Eire - BST - GMT
GMT - GMT - GMT
GMT0 - GMT+00:00 - GMT+00:00
Greenwich - GMT - GMT
Iceland - GMT - GMT
Portugal - WEST - WET
UCT - UTC - UTC
UTC - UTC - UTC
Universal - UTC - UTC
WET - WEST - WET
Zulu - UTC - UTC
Africa/Algiers - CET - CET
Africa/Bangui - WAT - WAT
Africa/Brazzaville - WAT - WAT
Africa/Ceuta - CEST - CET
Africa/Douala - WAT - WAT
Africa/Kinshasa - WAT - WAT
Africa/Lagos - WAT - WAT
Africa/Libreville - WAT - WAT
Africa/Luanda - WAT - WAT
Africa/Malabo - WAT - WAT
Africa/Ndjamena - WAT - WAT
Africa/Niamey - WAT - WAT
Africa/Porto-Novo - WAT - WAT
Africa/Tunis - CEST - CET
Africa/Windhoek - WAST - WAT
Arctic/Longyearbyen - CEST - CET
Atlantic/Jan_Mayen - CEST - CET
CET - CEST - CET
ECT - CEST - CET
Etc/GMT-1 - GMT+01:00 - GMT+01:00
Europe/Amsterdam - CEST - CET
Europe/Andorra - CEST - CET
Europe/Belgrade - CEST - CET
Europe/Berlin - CEST - CET
Europe/Bratislava - CEST - CET
Europe/Brussels - CEST - CET
Europe/Budapest - CEST - CET
Europe/Copenhagen - CEST - CET
Europe/Gibraltar - CEST - CET
Europe/Ljubljana - CEST - CET
Europe/Luxembourg - CEST - CET
Europe/Madrid - CEST - CET
Europe/Malta - CEST - CET
Europe/Monaco - CEST - CET
Europe/Oslo - CEST - CET
Europe/Paris - CEST - CET
Europe/Prague - CEST - CET
Europe/Rome - CEST - CET
Europe/San_Marino - CEST - CET
Europe/Sarajevo - CEST - CET
Europe/Skopje - CEST - CET
Europe/Stockholm - CEST - CET
Europe/Tirane - CEST - CET
Europe/Vaduz - CEST - CET
Europe/Vatican - CEST - CET
Europe/Vienna - CEST - CET
Europe/Warsaw - CEST - CET
Europe/Zagreb - CEST - CET
Europe/Zurich - CEST - CET
MET - MEST - MET
Poland - CEST - CET
ART - EEST - EET
Africa/Blantyre - CAT - CAT
Africa/Bujumbura - CAT - CAT
Africa/Cairo - EEST - EET
Africa/Gaborone - CAT - CAT
Africa/Harare - CAT - CAT
Africa/Johannesburg - SAST - SAST
Africa/Kigali - CAT - CAT
Africa/Lubumbashi - CAT - CAT
Africa/Lusaka - CAT - CAT
Africa/Maputo - CAT - CAT
Africa/Maseru - SAST - SAST
Africa/Mbabane - SAST - SAST
Africa/Tripoli - EET - EET
Asia/Amman - EEST - EET
Asia/Beirut - EEST - EET
Asia/Damascus - EEST - EET
Asia/Gaza - EEST - EET
Asia/Istanbul - EEST - EET
Asia/Jerusalem - IDT - IST
Asia/Nicosia - EEST - EET
Asia/Tel_Aviv - IDT - IST
CAT - CAT - CAT
EET - EEST - EET
Egypt - EEST - EET
Etc/GMT-2 - GMT+02:00 - GMT+02:00
Europe/Athens - EEST - EET
Europe/Bucharest - EEST - EET
Europe/Chisinau - EEST - EET
Europe/Helsinki - EEST - EET
Europe/Istanbul - EEST - EET
Europe/Kaliningrad - EEST - EET
Europe/Kiev - EEST - EET
Europe/Mariehamn - EEST - EET
Europe/Minsk - EEST - EET
Europe/Nicosia - EEST - EET
Europe/Riga - EEST - EET
Europe/Simferopol - EEST - EET
Europe/Sofia - EEST - EET
Europe/Tallinn - EEST - EET
Europe/Tiraspol - EEST - EET
Europe/Uzhgorod - EEST - EET
Europe/Vilnius - EEST - EET
Europe/Zaporozhye - EEST - EET
Israel - IDT - IST
Libya - EET - EET
Turkey - EEST - EET
Africa/Addis_Ababa - EAT - EAT
Africa/Asmera - EAT - EAT
Africa/Dar_es_Salaam - EAT - EAT
Africa/Djibouti - EAT - EAT
Africa/Kampala - EAT - EAT
Africa/Khartoum - EAT - EAT
Africa/Mogadishu - EAT - EAT
Africa/Nairobi - EAT - EAT
Antarctica/Syowa - SYOT - SYOT
Asia/Aden - AST - AST
Asia/Baghdad - ADT - AST
Asia/Bahrain - AST - AST
Asia/Kuwait - AST - AST
Asia/Qatar - AST - AST
Asia/Riyadh - AST - AST
EAT - EAT - EAT
Etc/GMT-3 - GMT+03:00 - GMT+03:00
Europe/Moscow - MSD - MSK
Indian/Antananarivo - EAT - EAT
Indian/Comoro - EAT - EAT
Indian/Mayotte - EAT - EAT
W-SU - MSD - MSK
Asia/Riyadh87 - GMT+03:07 - GMT+03:07
Asia/Riyadh88 - GMT+03:07 - GMT+03:07
Asia/Riyadh89 - GMT+03:07 - GMT+03:07
Mideast/Riyadh87 - GMT+03:07 - GMT+03:07
Mideast/Riyadh88 - GMT+03:07 - GMT+03:07
Mideast/Riyadh89 - GMT+03:07 - GMT+03:07
Asia/Tehran - IRST - IRST
Iran - IRST - IRST
Asia/Baku - AZST - AZT
Asia/Dubai - GST - GST
Asia/Muscat - GST - GST
Asia/Tbilisi - GET - GET
Asia/Yerevan - AMST - AMT
Etc/GMT-4 - GMT+04:00 - GMT+04:00
Europe/Samara - SAMST - SAMT
Indian/Mahe - SCT - SCT
Indian/Mauritius - MUT - MUT
Indian/Reunion - RET - RET
NET - AMST - AMT
Asia/Kabul - AFT - AFT
Asia/Aqtau - AQTT - AQTT
Asia/Aqtobe - AQTT - AQTT
Asia/Ashgabat - TMT - TMT
Asia/Ashkhabad - TMT - TMT
Asia/Dushanbe - TJT - TJT
Asia/Karachi - PKT - PKT
Asia/Oral - ORAT - ORAT
Asia/Samarkand - UZT - UZT
Asia/Tashkent - UZT - UZT
Asia/Yekaterinburg - YEKST - YEKT
Etc/GMT-5 - GMT+05:00 - GMT+05:00
Indian/Kerguelen - TFT - TFT
Indian/Maldives - MVT - MVT
PLT - PKT - PKT
Asia/Calcutta - IST - IST
Asia/Colombo - LKT - LKT
IST - IST - IST
Asia/Katmandu - NPT - NPT
Antarctica/Mawson - MAWT - MAWT
Antarctica/Vostok - VOST - VOST
Asia/Almaty - ALMT - ALMT
Asia/Bishkek - KGT - KGT
Asia/Dacca - BDT - BDT
Asia/Dhaka - BDT - BDT
Asia/Novosibirsk - NOVST - NOVT
Asia/Omsk - OMSST - OMST
Asia/Qyzylorda - QYZT - QYZT
Asia/Thimbu - BTT - BTT
Asia/Thimphu - BTT - BTT
BST - BDT - BDT
Etc/GMT-6 - GMT+06:00 - GMT+06:00
Indian/Chagos - IOT - IOT
Asia/Rangoon - MMT - MMT
Indian/Cocos - CCT - CCT
Antarctica/Davis - DAVT - DAVT
Asia/Bangkok - ICT - ICT
Asia/Hovd - HOVST - HOVT
Asia/Jakarta - WIT - WIT
Asia/Krasnoyarsk - KRAST - KRAT
Asia/Phnom_Penh - ICT - ICT
Asia/Pontianak - WIT - WIT
Asia/Saigon - ICT - ICT
Asia/Vientiane - ICT - ICT
Etc/GMT-7 - GMT+07:00 - GMT+07:00
Indian/Christmas - CXT - CXT
VST - ICT - ICT
Antarctica/Casey - WST - WST
Asia/Brunei - BNT - BNT
Asia/Chongqing - CST - CST
Asia/Chungking - CST - CST
Asia/Harbin - CST - CST
Asia/Hong_Kong - HKT - HKT
Asia/Irkutsk - IRKST - IRKT
Asia/Kashgar - CST - CST
Asia/Kuala_Lumpur - MYT - MYT
Asia/Kuching - MYT - MYT
Asia/Macao - CST - CST
Asia/Macau - CST - CST
Asia/Makassar - CIT - CIT
Asia/Manila - PHT - PHT
Asia/Shanghai - CST - CST
Asia/Singapore - SGT - SGT
Asia/Taipei - CST - CST
Asia/Ujung_Pandang - CIT - CIT
Asia/Ulaanbaatar - ULAST - ULAT
Asia/Ulan_Bator - ULAST - ULAT
Asia/Urumqi - CST - CST
Australia/Perth - WST - WST
Australia/West - WST - WST
CTT - CST - CST
Etc/GMT-8 - GMT+08:00 - GMT+08:00
Hongkong - HKT - HKT
PRC - CST - CST
Singapore - SGT - SGT
Asia/Choibalsan - CHOST - CHOT
Asia/Dili - TPT - TPT
Asia/Jayapura - EIT - EIT
Asia/Pyongyang - KST - KST
Asia/Seoul - KST - KST
Asia/Tokyo - JST - JST
Asia/Yakutsk - YAKST - YAKT
Etc/GMT-9 - GMT+09:00 - GMT+09:00
JST - JST - JST
Japan - JST - JST
Pacific/Palau - PWT - PWT
ROK - KST - KST
ACT - CST - CST
Australia/Adelaide - CST - CST
Australia/Broken_Hill - CST - CST
Australia/Darwin - CST - CST
Australia/North - CST - CST
Australia/South - CST - CST
Australia/Yancowinna - CST - CST
AET - EST - EST
Antarctica/DumontDUrville - DDUT - DDUT
Asia/Sakhalin - SAKST - SAKT
Asia/Vladivostok - VLAST - VLAT
Australia/ACT - EST - EST
Australia/Brisbane - EST - EST
Australia/Canberra - EST - EST
Australia/Currie - EST - EST
Australia/Hobart - EST - EST
Australia/Lindeman - EST - EST
Australia/Melbourne - EST - EST
Australia/NSW - EST - EST
Australia/Queensland - EST - EST
Australia/Sydney - EST - EST
Australia/Tasmania - EST - EST
Australia/Victoria - EST - EST
Etc/GMT-10 - GMT+10:00 - GMT+10:00
Pacific/Guam - ChST - ChST
Pacific/Port_Moresby - PGT - PGT
Pacific/Saipan - ChST - ChST
Pacific/Truk - TRUT - TRUT
Pacific/Yap - YAPT - YAPT
Australia/LHI - LHST - LHST
Australia/Lord_Howe - LHST - LHST
Asia/Magadan - MAGST - MAGT
Etc/GMT-11 - GMT+11:00 - GMT+11:00
Pacific/Efate - VUT - VUT
Pacific/Guadalcanal - SBT - SBT
Pacific/Kosrae - KOST - KOST
Pacific/Noumea - NCT - NCT
Pacific/Ponape - PONT - PONT
SST - SBT - SBT
Pacific/Norfolk - NFT - NFT
Antarctica/McMurdo - NZDT - NZST
Antarctica/South_Pole - NZDT - NZST
Asia/Anadyr - ANAST - ANAT
Asia/Kamchatka - PETST - PETT
Etc/GMT-12 - GMT+12:00 - GMT+12:00
Kwajalein - MHT - MHT
NST - NZDT - NZST
NZ - NZDT - NZST
Pacific/Auckland - NZDT - NZST
Pacific/Fiji - FJT - FJT
Pacific/Funafuti - TVT - TVT
Pacific/Kwajalein - MHT - MHT
Pacific/Majuro - MHT - MHT
Pacific/Nauru - NRT - NRT
Pacific/Tarawa - GILT - GILT
Pacific/Wake - WAKT - WAKT
Pacific/Wallis - WFT - WFT
NZ-CHAT - CHADT - CHAST
Pacific/Chatham - CHADT - CHAST
Etc/GMT-13 - GMT+13:00 - GMT+13:00
Pacific/Enderbury - PHOT - PHOT
Pacific/Tongatapu - TOT - TOT
Etc/GMT-14 - GMT+14:00 - GMT+14:00
Pacific/Kiritimati - LINT - LINT

*/


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

