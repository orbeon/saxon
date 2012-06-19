package org.orbeon.saxon.charcode;
import org.orbeon.saxon.event.PipelineConfiguration;
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
     * @param details the serialization properties
     * @param pipe the PipelineConfiguration (used to get the current ClassLoader)
     * @return the constructed CharacterSet
    */

    public static CharacterSet getCharacterSet(Properties details, PipelineConfiguration pipe)
    throws XPathException {

        String encoding = details.getProperty(OutputKeys.ENCODING);
        if (encoding == null) {
            encoding = "UTF8";
        }
        if (encoding.equalsIgnoreCase("UTF-8")) {
            encoding = "UTF8";    // needed for Microsoft Java VM
        }

        CharacterSet charSet = makeCharacterSet(encoding, pipe);
        if (charSet==null) {
            XPathException err = new XPathException("Unknown encoding requested: " + encoding);
            err.setErrorCode("SESU0007");
            throw err;
        }
        return charSet;
    }

	private static CharacterSet makeCharacterSet(String encoding, PipelineConfiguration pipe)
    throws XPathException {
        String enc2 = encoding.replace('_', '-');
        switch(enc2.length()) {
            case 4:
                if (enc2.equalsIgnoreCase("UTF8")) {
                    return UnicodeCharacterSet.getInstance();
                } else if (enc2.equalsIgnoreCase("Big5")) {
                    return Big5CharacterSet.getInstance();
                } else if (enc2.equalsIgnoreCase("SJIS")) {
                    return ShiftJISCharacterSet.getInstance();
                }
                break;
            case 5:
                if (enc2.equalsIgnoreCase("ASCII")) {
                    return ASCIICharacterSet.getInstance();
                } else if (enc2.equalsIgnoreCase("UTF-8")) {
                    return UnicodeCharacterSet.getInstance();
                } if (enc2.equalsIgnoreCase("UTF16")) {
                    return UnicodeCharacterSet.getInstance();
                } else if (enc2.equalsIgnoreCase("cp852")) {
                    return CP852CharacterSet.getInstance();
                }
                break;
            case 6:
                if (enc2.equalsIgnoreCase("iso646")) {
                    return ASCIICharacterSet.getInstance();
                } else if (enc2.equalsIgnoreCase("UTF-16")) {
                    return UnicodeCharacterSet.getInstance();
                } else if (enc2.equalsIgnoreCase("EUC-CN")) {
                    return GB2312CharacterSet.getInstance();
                } else if (enc2.equalsIgnoreCase("GB2312")) {
                    return GB2312CharacterSet.getInstance();
                } else if (enc2.equalsIgnoreCase("EUC-JP")) {
                    return EucJPCharacterSet.getInstance();
                } else if (enc2.equalsIgnoreCase("EUC-KR")) {
                    return EucKRCharacterSet.getInstance();
                } else if (enc2.equalsIgnoreCase("KOI8-R")) {
                    return KOI8RCharacterSet.getInstance();
                } else if (enc2.equalsIgnoreCase("cp1251")) {
                    return CP1251CharacterSet.getInstance();
                } else if (enc2.equalsIgnoreCase("cp1250")) {
                    return CP1250CharacterSet.getInstance();
                } else if (enc2.equalsIgnoreCase("cp1252")) {
                    return CP1252CharacterSet.getInstance();
                }
                break;
            case 7:
                if (enc2.equalsIgnoreCase("iso-646")) {
                    return ASCIICharacterSet.getInstance();
                } else
                break;
            case 8:
                if (enc2.equalsIgnoreCase("US-ASCII")) {
                    return ASCIICharacterSet.getInstance();
                }
                break;
            case 9:
                if (enc2.equalsIgnoreCase("Shift-JIS")) {
                    return ShiftJISCharacterSet.getInstance();
                } else if (enc2.equalsIgnoreCase("ISO8859-1")) {
                    return ISO88591CharacterSet.getInstance();
                } else if (enc2.equalsIgnoreCase("ISO8859-2")) {
                    return ISO88592CharacterSet.getInstance();
                } else if (enc2.equalsIgnoreCase("ISO8859-5")) {
                    return ISO88595CharacterSet.getInstance();
                } else if (enc2.equalsIgnoreCase("ISO8859-7")) {
                    return ISO88597CharacterSet.getInstance();
                } else if (enc2.equalsIgnoreCase("ISO8859-8")) {
                    return ISO88598CharacterSet.getInstance();
                } else if (enc2.equalsIgnoreCase("ISO8859-9")) {
                    return ISO88599CharacterSet.getInstance();
                }
                break;
            case 10:
                if (enc2.equalsIgnoreCase("iso-8859-1")) {
                    return ISO88591CharacterSet.getInstance();
                }  else if (enc2.equalsIgnoreCase("iso-8859-2")) {
                    return ISO88592CharacterSet.getInstance();
                } else if (enc2.equalsIgnoreCase("iso-8859-5")) {
                    return ISO88595CharacterSet.getInstance();
                }  else if (enc2.equalsIgnoreCase("iso-8859-7")) {
                    return ISO88597CharacterSet.getInstance();
                } else if (enc2.equalsIgnoreCase("iso-8859-8")) {
                    return ISO88598CharacterSet.getInstance();
                } else if (enc2.equalsIgnoreCase("iso-8859-9")) {
                    return ISO88599CharacterSet.getInstance();
                }
            case 11:
                if (enc2.equalsIgnoreCase("windows-852")) {
                    return CP852CharacterSet.getInstance();
                }
                break;
            case 12:
                if (enc2.equalsIgnoreCase("windows-1251")) {
                    return CP1251CharacterSet.getInstance();
                } else if (enc2.equalsIgnoreCase("windows-1250")) {
                    return CP1250CharacterSet.getInstance();
                } else if (enc2.equalsIgnoreCase("windows-1252")) {
                    return CP1252CharacterSet.getInstance();
                }
                break;
            default:
                break;
        }

        // Allow an alias for the character set to be specified as a system property
        String csname = System.getProperty("encoding." + enc2);
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
                throw new XPathException("Invalid encoding name: " + encoding);
            } catch (UnsupportedCharsetException err) {
                return null;
            }
        } else {
            try {
                Object obj = pipe.getConfiguration().getInstance(csname, pipe.getController().getClassLoader());
                if (obj instanceof PluggableCharacterSet) {
                    return (PluggableCharacterSet)obj;
                }
            } catch (Exception err) {
                throw new XPathException("Failed to load " + csname);
            }
        }
    	return null;
	}

    /**
     * Main program is a utility to give a list of the character sets supported
     * by the Java VM
     * @param args command line arguments
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
