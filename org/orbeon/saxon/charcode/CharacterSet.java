package net.sf.saxon.charcode;


/**
* This interface defines properties of a pluggable character set, that is, a user-supplied
* character set. This is selected in xsl:output using encoding="class-name", where
* class-name is the full name of an implementation of PluggableCharacterSet
*/

public interface CharacterSet {

    /**
    * Determine if a character is present in the character set
    */

    public boolean inCharset(int ch);

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
