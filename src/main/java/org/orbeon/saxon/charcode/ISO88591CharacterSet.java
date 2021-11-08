package org.orbeon.saxon.charcode;

/**
* This class defines properties of the ISO-8859-1 character set
*/

public class ISO88591CharacterSet implements CharacterSet {

    private static ISO88591CharacterSet theInstance = new ISO88591CharacterSet();

    private ISO88591CharacterSet() {}

    public static ISO88591CharacterSet getInstance() {
        return theInstance;
    }

    public final boolean inCharset(int c) {
        return c <= 0xff;
    }

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
// The Initial Developer of the Original Code is
// Aleksei Makarov [makarov@iitam.omsk.net.ru]
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
