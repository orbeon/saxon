package org.orbeon.saxon.number;


/**
 * Class Numberer_fr is a number formatter for french. This one will be
 * activated for language="fr"
 *
 * @author Luc Rochefort
 * @version 1.0
 *
 */

public class Numberer_fr extends AbstractNumberer {

	/**
	 * Automatically generated serialVersionUID number
	 */
	private static final long serialVersionUID = -222104830008011842L;

	private static String[] frenchUnits = { "", "Un", "Deux", "Trois", "Quatre", "Cinq", "Six", "Sept", "Huit", "Neuf", "Dix", "Onze", "Douze", "Treize", "Quatorze", "Quinze", "Seize", "Dix-sept", "Dix-huit", "Dix-neuf" };

	private static String[] frenchTens = { "", "Dix", "Vingt", "Trente", "Quarante", "Cinquante", "Soixante", "Soixante", "Quatre-vingt", "Quatre-vingt" };

	private static String[] frenchOrdinalUnits = { "", "Premier", "Deuxi�me", "Troisi�me", "Quatri�me", "Cinqui�me", "Sixi�me", "Septi�me", "Huiti�me", "Neuvi�me", "Dixi�me", "Onzi�me", "Douzi�me", "Treizi�me", "Quatorzi�me", "Quinzi�me", "Seizi�me", "Dix-septi�me", "Dix-huiti�me", "Dix-neuvi�me" };

	private static String[] frenchOrdinalTens = { "", "Dixi�me", "Vingti�me", "Trenti�me", "Quaranti�me", "Cinquanti�me", "Soixanti�me", "Soixante", "Quatre-vingti�me", "Quatre-vingt" };

	private static String[] frenchDays = { "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche" };

	private static String[] frenchMonths = { "Janvier", "F�vrier", "Mars", "Avril", "Mai", "Juin", "Juillet", "Ao�t", "Septembre", "Octobre", "Novembre", "D�cembre" };

	/*
	 * (non-Javadoc)
	 *
	 * @see org.orbeon.saxon.number.Numberer_en#ordinalSuffix(java.lang.String,
	 *      long)
	 */
	protected String ordinalSuffix(String ordinalParam, long number) {
		if (number != 1) {
			return "e";
		} else {
			return "er";
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.orbeon.saxon.number.Numberer_en#toWords(long)
	 */
	public String toWords(long number) {
		return toWords(number, true);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.orbeon.saxon.number.Numberer_en#toWords(long, int)
	 */
	public String toWords(long number, int wordCase) {
		String s = toWords(number);
		if (wordCase == UPPER_CASE) {
			return s.toUpperCase();
		} else if (wordCase == LOWER_CASE) {
			return s.toLowerCase();
		} else {
			return s;
		}
	}

	private String toWords(long number, boolean terminal) {
		if (number == 0) {
			return "Z�ro";
		} else if (number >= 1000000000000000000l) {
			long rem = number % 1000000000000000000l;
			long n = number / 1000000000000000000l;
			String s = (n == 1 ? "Un" : toWords(n, true));
			return s + " quintillion" + (n > 1 ? "s" : "") + (rem == 0 ? "" : " " + toWords(rem, LOWER_CASE, terminal));
		} else if (number >= 1000000000000000l) {
			long rem = number % 1000000000000000l;
			long n = number / 1000000000000000l;
			String s = (n == 1 ? "Un" : toWords(n, true));
			return s + " quatrillion" + (n > 1 ? "s" : "") + (rem == 0 ? "" : " " + toWords(rem, LOWER_CASE, terminal));
		} else if (number >= 1000000000000l) {
			long rem = number % 1000000000000l;
			long n = number / 1000000000000l;
			String s = (n == 1 ? "Un" : toWords(n, true));
			return s + " trillion" + (n > 1 ? "s" : "") + (rem == 0 ? "" : " " + toWords(rem, LOWER_CASE, terminal));
		} else if (number >= 1000000000) {
			long rem = number % 1000000000;
			long n = number / 1000000000;
			String s = (n == 1 ? "Un" : toWords(n, true));
			return s + " milliard" + (n > 1 ? "s" : "") + (rem == 0 ? "" : " " + toWords(rem, LOWER_CASE, terminal));
		} else if (number >= 1000000) {
			long rem = number % 1000000;
			long n = number / 1000000;
			String s = (n == 1 ? "Un" : toWords(n, true));
			return s + " million" + (n > 1 ? "s" : "") + (rem == 0 ? "" : " " + toWords(rem, LOWER_CASE, terminal));
		} else if (number >= 1000) {
			long rem = number % 1000;
			long n = number / 1000;
			String s = (n == 1 ? "" : toWords(n, false));
			return s + (n == 1 ? "Mille" : " mille") + (rem == 0 ? "" : " " + toWords(rem, LOWER_CASE, terminal));
		} else if (number >= 100) {
			long rem = number % 100;
			long n = number / 100;
			String s = (n == 1 ? "" : toWords(n, false));
			return s + (n == 1 ? "Cent" : " cent") + (rem == 0 && n > 1 && terminal ? "s" : ((rem != 0) ? " " + toWords(rem, LOWER_CASE, terminal) : ""));
		} else {
			if (number < 20)
				return frenchUnits[(int) number];
			int rem = (int) (number % 10);
			int tens = (int) number / 10;
			if (tens == 7 || tens == 9) {
				rem += 10;
			}
			String link = (rem == 1 || rem == 11) ? ((tens == 8 || tens == 9) ? "-" : " et ") : "-";

			return frenchTens[tens] + (rem == 0 ? ((tens == 8 && terminal) ? "s" : "") : link) + (tens == 0 ? frenchUnits[rem] : frenchUnits[rem].toLowerCase());
		}
	}

	private String toWords(long number, int wordCase, boolean terminal) {
		String s = toWords(number, terminal);
		if (wordCase == UPPER_CASE) {
			return s.toUpperCase();
		} else if (wordCase == LOWER_CASE) {
			return s.toLowerCase();
		} else {
			return s;
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.orbeon.saxon.number.Numberer_en#toOrdinalWords(java.lang.String,
	 *      long, int)
	 */
	public String toOrdinalWords(String ordinalParam, long number, int wordCase) {
		String ord;
		if (number < 20) {
			if (number == 0) {
				ord = "Z�roi�me";
			} else {
				ord = frenchOrdinalUnits[(int) number];
			}
		} else if (number < 100) {
			long mod10 = number % 10;
			long int10 = number / 10;
			if (int10 == 7 || int10 == 9) {
				int10 -= 1;
				mod10 += 10;
			}
			if (mod10 == 0) {
				ord = frenchOrdinalTens[(int) int10];
			} else {
				String link = (mod10 == 1 || mod10 == 11) ? ((int10 == 8) ? "-" : " et ") : "-";
				String prefix = toWords(int10 * 10);
				if (int10 == 8) {
					prefix = prefix.substring(0, prefix.length() - 1);
				}
				String result = prefix + link;
				ord = result + ((mod10 == 1) ? "uni�me" : toOrdinalWords("", mod10, LOWER_CASE));
			}
		} else {
			String suffix = "i�me";
			long mod100 = number % 100;
			long int100 = number / 100;
			if (int100 == 70 || int100 == 90) {
				int100 -= 10;
				mod100 += 100;
			}

			String prefix = toWords(int100 * 100, false);
			if (int100 % 10000 == 0) {
				prefix = prefix.replaceFirst("Un ", "");
			}

			/* strip prefix, if needed */
			if ((prefix.endsWith("mille") || prefix.endsWith("Mille")) && mod100 == 0) {
				prefix = prefix.substring(0, prefix.length() - 1);
			} else if (prefix.endsWith("illions") || prefix.endsWith("illiards")) {
				prefix = prefix.substring(0, prefix.length() - 1);
			}

			ord = prefix + ((mod100 == 0) ? suffix : " " + ((mod100 == 1) ? "uni�me" : toOrdinalWords("", mod100, LOWER_CASE)));
		}
		if (wordCase == UPPER_CASE) {
			return ord.toUpperCase();
		} else if (wordCase == LOWER_CASE) {
			return ord.toLowerCase();
		} else {
			return ord;
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.orbeon.saxon.number.Numberer#monthName(int, int, int)
	 */
	public String monthName(int month, int minWidth, int maxWidth) {
		String name = frenchMonths[month - 1];
		if (maxWidth < 3) {
			maxWidth = 3;
		}
		if (name.length() > maxWidth) {
			name = name.substring(0, maxWidth);
		}
		while (name.length() < minWidth) {
			name = name + " ";
		}
		return name;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.orbeon.saxon.number.Numberer#dayName(int, int, int)
	 */
	public String dayName(int day, int minWidth, int maxWidth) {
		String name = frenchDays[day - 1];
		if (maxWidth < 3) {
			maxWidth = 3;
		}
		if (name.length() > maxWidth) {
			name = name.substring(0, maxWidth);
		}
		while (name.length() < minWidth) {
			name = name + " ";
		}
		return name;
	}

}

//
// The contents of this file are subject to the Mozilla Public License Version
// 1.0 (the "License");
// you may not use this file except in compliance with the License. You may
// obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations
// under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by Luc Rochefort are Copyright (C) Luc Rochefort. All
// Rights Reserved.
//
// Contributor(s): 	Laurent Bourbeau, for the elaboration of JUnit tests
//					and Jean-Gr�goire Dj�nandji, for acceptance testing.
//