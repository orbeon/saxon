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
iso-8859-8 encoding.  The character mapping was obtained by extracting the
Unicode values from an iconv character table (iso88=ucs2) available on HP-UX 11.23.

The class was tested by transforming a document with ISO-8859-8
set as the output encoding, converting ISO-8859-8 output to utf-8 using iconv,
and then comparing converted content to the same transformed document with utf-8 set
as the output encoding.

*/


public class ISO88598CharacterSet implements CharacterSet {

    private static ISO88598CharacterSet THE_INSTANCE = new ISO88598CharacterSet();

    public static ISO88598CharacterSet getInstance() {
        return THE_INSTANCE;
    }

	private static boolean c[] = null;

	static {
		c = new boolean[1520];

//		for (int i=0; i<=25; ++i) { c[i] = true; }
//		for (int i=27; i<=160; ++i) { c[i] = true; }
        Arrays.fill(c, 0, 191, true);
        c[26] = false;
//		for (int i=162; i<=169; ++i) { c[i] = true; }
//		for (int i=171; i<=174; ++i) { c[i] = true; }
//		for (int i=176; i<=185; ++i) { c[i] = true; }
//		for (int i=187; i<=190; ++i) { c[i] = true; }
        c[161] = false;
        c[170] = false;
        c[175] = false;
        c[186] = false;
		c[215] = true;
		c[247] = true;
//		for (int i=1488; i<=1514; ++i) { c[i] = true; }
        Arrays.fill(c, 1488, 1515, true);
		//c[8215] = true;
		//c[8254] = true;
	}

	public ISO88598CharacterSet() {
	}

	// Determine if it is a valid character
	public final boolean inCharset(int ch) {
		return (ch < 1520 && c[ch]) || ch==8215 || ch==8254;
	}

	public final String getEncodingName() {
		// Canonical Name for java.io and java.lang API
		return "ISO8859_8";
	}
}


