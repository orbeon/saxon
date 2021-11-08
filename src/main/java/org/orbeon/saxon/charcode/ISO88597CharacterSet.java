package org.orbeon.saxon.charcode;

import java.util.Arrays;

/*

Copyright (C) 2006 Hewlett-Packard Development Company, L.P.

The contents of this file are subject to the Mozilla Public License Version 1.1
(the "License"); you may not use this file except in compliance with the
License. You may obtain a copy of the License at http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
the specific language governing rights and limitations under the License.

The Original Code is: all this file
The Initial Developer of the Original Code is Lauren Ward. All Rights Reserved.
Contributor(s): Integrated into Saxon by Michael Kay

*************************
Author:
  Lauren Ward
Date:
  February 01, 2006
Address:
  Hewlett-Packard Company
  3404 East Harmony Road
  Fort Collins, CO 80528-9599
Revision:
  1.0 - Initial creation

Description: This class implements the PluggableCharacterSet to support
iso-8859-7 encoding.  The character mapping was obtained by extracting the
Unicode values from an iconv character table (iso87=ucs2) available on HP-UX 11.23.

The class was tested by transforming a document with ISO-8859-7
set as the output encoding, converting ISO-8859-7 output to utf-8 using iconv,
and then comparing converted content to the same transformed document with utf-8 set
as the output encoding.

*/


public class ISO88597CharacterSet implements CharacterSet {

    private static ISO88597CharacterSet THE_INSTANCE = new ISO88597CharacterSet();

    public static ISO88597CharacterSet getInstance() {
        return THE_INSTANCE;
    }

	private static boolean c[] = null;

	static {
		c = new boolean[1000];

//		for (int i=0; i<=25; ++i) { c[i] = true; }
//		for (int i=27; i<=160; ++i) { c[i] = true; }
        Arrays.fill(c, 0, 161, true);
        c[26] = false;
		c[163] = true;
//		for (int i=166; i<=169; ++i) { c[i] = true; }
//		for (int i=171; i<=173; ++i) { c[i] = true; }
//		for (int i=176; i<=179; ++i) { c[i] = true; }
        Arrays.fill(c, 166, 180, true);
        c[170] = false;
        c[174] = false;
        c[175] = false;
		c[183] = true;
		c[187] = true;
		c[189] = true;

		//for (int i=700; i<=701; ++i) { c[i] = true; }
        c[700] = true;
        c[701] = true;
		c[890] = true;
		c[894] = true;
//		for (int i=900; i<=902; ++i) { c[i] = true; }
//		for (int i=904; i<=906; ++i) { c[i] = true; }
//		c[908] = true;
//		for (int i=910; i<=929; ++i) { c[i] = true; }
//		for (int i=931; i<=974; ++i) { c[i] = true; }
        Arrays.fill(c, 900, 975, true);
        c[903] = false;
        c[907] = false;
        c[909] = false;
        c[930] = false;
//		c[8213] = true;
//		c[8364] = true;
//		c[8367] = true;

	}

	public ISO88597CharacterSet() {
	}

	//	Determine if it is a valid character
	public final boolean inCharset(int ch) {
		return (ch < 1000 && c[ch]) || (ch > 8212 && (ch==8213 || ch==8364 || ch==8367));
	}

	public final String getEncodingName() {
		// Canonical Name for java.io and java.lang API
		return "ISO8859_7";
	}
}


