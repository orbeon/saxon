package net.sf.saxon.charcode;

/**
* This class defines properties of the KO18R Cyrillic character set
*/

public class KOI8RCharacterSet implements CharacterSet {

    private static KOI8RCharacterSet theInstance = new KOI8RCharacterSet();

    private KOI8RCharacterSet() {}

    public static KOI8RCharacterSet getInstance() {
        return theInstance;
    }

    public final boolean inCharset(int c) {
        return ( c <= 0x7f ) || ( (0x0410 <= c) && (c <= 0x044f) ) ||
                    c == 0x0451 || c == 0x0401;
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
