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
*/

/**
* Description: This class implements the CharacterSet to support ISO-8859-5 (Latin/Cyrillic)
* encoding.  The character mapping was obtained by extracting the
* Unicode values from an iconv character table (iso85=ucs2) available on HP-UX 11.23.
* <p/>
* The class was tested by transforming a document with ISO-8859-5
* set as the output encoding, converting Shif_JIS output to utf-8 using iconv,
* and then comparing converted content to the same transformed document with utf-8 set
* as the output encoding.
* <p/>
* Checked by MHK against http://www.unicode.org/Public/MAPPINGS/ISO8859/8859-5.TXT
*
*/

public class ISO88595CharacterSet implements CharacterSet {

    private static ISO88595CharacterSet THE_INSTANCE = new ISO88595CharacterSet();

    public static ISO88595CharacterSet getInstance() {
        return THE_INSTANCE;
    }

	private static boolean c[] = null;

	static {
		c = new boolean[1120];
        Arrays.fill(c, 0, 161, true);
        c[26] = false;
		c[167] = true;    // xA7 section sign
		c[173] = true;    // xAD soft hyphen
        Arrays.fill(c, 1025, 1120, true);
        c[1037] = false;     // x040D
        c[1104] = false;     // x0450
        c[1117] = false;     // x045D
        //c[8470] = true;
	}

	public ISO88595CharacterSet() {
	}

	// Determine if it is a valid character
	public final boolean inCharset(int ch) {
		return (ch < 1120 && c[ch]) || (ch==8470);
	}

	public final String getEncodingName() {
		// Canonical Name for java.io and java.lang API
		return "ISO8859_5";
	}

}


