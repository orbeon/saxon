package org.orbeon.saxon.charcode;
import org.orbeon.saxon.Loader;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;

import javax.xml.transform.OutputKeys;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Iterator;
import java.util.Properties;

/**
* This class creates a CharacterSet object for a given named encoding.
*/


public class CharacterSetFactory {

    /**
     * Class is never instantiated
     */
    private CharacterSetFactory() {
    }

    /**
    * Make a CharacterSet appropriate to the encoding
    */

    public static CharacterSet getCharacterSet(Properties details)
    throws XPathException {

        String encoding = details.getProperty(OutputKeys.ENCODING);
        if (encoding==null) encoding = "UTF8";
        if (encoding.equalsIgnoreCase("UTF-8")) encoding = "UTF8";    // needed for Microsoft Java VM

        CharacterSet charSet = makeCharacterSet(encoding);
        if (charSet==null) {
        	charSet = ASCIICharacterSet.getInstance();
        }
        return charSet;
    }

	private static CharacterSet makeCharacterSet(String encoding)
    throws XPathException {
        if (encoding.equalsIgnoreCase("UTF8")) {
            return UnicodeCharacterSet.getInstance();
        } else if (encoding.equalsIgnoreCase("ASCII")) {
            return ASCIICharacterSet.getInstance();
        } else if (encoding.equalsIgnoreCase("US-ASCII")) {
            return ASCIICharacterSet.getInstance();
        } else if (encoding.equalsIgnoreCase("iso-646")) {
            return ASCIICharacterSet.getInstance();
        } else if (encoding.equalsIgnoreCase("iso646")) {
            return ASCIICharacterSet.getInstance();
        } else if (encoding.equalsIgnoreCase("iso-8859-1")) {
            return Latin1CharacterSet.getInstance();
        } else if (encoding.equalsIgnoreCase("ISO8859_1")) {
            return Latin1CharacterSet.getInstance();
        } else if (encoding.equalsIgnoreCase("iso-8859-2")) {
            return Latin2CharacterSet.getInstance();
        } else if (encoding.equalsIgnoreCase("ISO8859_2")) {
            return Latin2CharacterSet.getInstance();
        } else if (encoding.equalsIgnoreCase("UTF-8")) {
            return UnicodeCharacterSet.getInstance();
        } else if (encoding.equalsIgnoreCase("UTF-16")) {
            return UnicodeCharacterSet.getInstance();
        } else if (encoding.equalsIgnoreCase("UTF16")) {
            return UnicodeCharacterSet.getInstance();
        } else if (encoding.equalsIgnoreCase("KOI8-R")) {
            return KOI8RCharacterSet.getInstance();
        } else if (encoding.equalsIgnoreCase("cp1251")) {
            return CP1251CharacterSet.getInstance();
        } else if (encoding.equalsIgnoreCase("windows-1251")) {
            return CP1251CharacterSet.getInstance();
        } else if (encoding.equalsIgnoreCase("cp1250")) {
            return CP1250CharacterSet.getInstance();
        } else if (encoding.equalsIgnoreCase("windows-1250")) {
            return CP1250CharacterSet.getInstance();
        } else if (encoding.equalsIgnoreCase("cp1252")) {
            return CP1252CharacterSet.getInstance();
        } else if (encoding.equalsIgnoreCase("windows-1252")) {
            return CP1252CharacterSet.getInstance();
        } else if (encoding.equalsIgnoreCase("cp852")) {
            return CP852CharacterSet.getInstance();
        } else if (encoding.equalsIgnoreCase("windows-852")) {
            return CP852CharacterSet.getInstance();

        } else {
            // Allow an alias for the character set to be specified as a system property
            String csname = System.getProperty(OutputKeys.ENCODING + '.' + encoding);
            if (csname == null) {
                Charset charset;
                try {
                    charset = Charset.forName(encoding);
                    CharacterSet res = UnknownCharacterSet.makeCharSet(charset);

                    // Some JDK1.4 charsets are known to be buggy, for example SJIS.
                    // We'll see whether the charset claims to be able to encode some
                    // tricky characters; if it says it can, the chances are it's lying.

                    if (res.inCharset(0x1ff) &&
                            res.inCharset(0x300) &&
                            res.inCharset(0xa90) &&
                            res.inCharset(0x2200) &&
                            res.inCharset(0x3400)) {
                        res = BuggyCharacterSet.makeCharSet(charset);
                    }
                    return res;
                } catch (IllegalCharsetNameException err) {
                    throw new DynamicError("Invalid encoding name: " + encoding);
                } catch (UnsupportedCharsetException err) {
                    //System.err.println("Unknown encoding " + encoding + ": reverting to ASCII");
                    return ASCIICharacterSet.getInstance();
                }
            } else {
                try {
                    Object obj = Loader.getInstance(csname);
                    if (obj instanceof PluggableCharacterSet) {
                        return (PluggableCharacterSet)obj;
                    }
                } catch (Exception err) {
                    throw new DynamicError("Failed to load " + csname);
                }
            }
        }
    	return null;
	}

    /**
     * Main program is a utility to give a list of the character sets supported
     * by the Java VM
     */

    public static void main(String[] args) throws Exception {
        System.err.println("Available Character Sets in the java.nio package for this Java VM:");
        Iterator iter = Charset.availableCharsets().keySet().iterator();
        while (iter.hasNext()) {
            String s = (String) iter.next();
            System.err.println(s);
        }
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
