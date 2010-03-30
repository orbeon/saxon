package org.orbeon.saxon.event;

import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.om.FastStringBuffer;

import java.io.IOException;

/**
 * The XQueryEmitter is designed to serialize an XQuery that was originally embedded in an
 * XML document. It is a variant of the XMLEmitter, and differs in that the operators <, >, <=, >=, <<, and <<
 * are output without escaping. They are recognized by virtue of the fact that they appear in text or attribute
 * content between curly braces but not in quotes.
 */

public class XQueryEmitter extends XMLEmitter {

    /**
     * Write contents of array to current writer, after escaping special characters.
     * This method converts the XML special characters (such as < and &) into their
     * predefined entities.
     *
     * @param chars       The character sequence containing the string
     * @param inAttribute Set to true if the text is in an attribute value
     */

    protected void writeEscape(final CharSequence chars, final boolean inAttribute) throws IOException, XPathException {
        boolean inBraces = false;
        FastStringBuffer buff = new FastStringBuffer(chars.length());
        for (int i=0; i<chars.length(); i++) {
            char c = chars.charAt(i);
            if (!inBraces && c=='{' && chars.charAt(i+1)!='{') {
                inBraces = true;
                buff.append((char)0);   // switch disable-output-escaping on
            } else if (inBraces && c=='}') {
                inBraces = false;
                buff.append((char)0);   // switch disable-output-escaping off
            } else if (inBraces && c=='"') {
                buff.append((char)0);
                i++;
                do {
                    buff.append(c);
                    c = chars.charAt(i++);
                } while (c != '"');
                buff.append((char)0);
                i--;
            } else if (inBraces && c=='\'') {
                buff.append((char)0);
                i++;
                do {
                    buff.append(c);
                    c = chars.charAt(i++);
                } while (c != '\'');
                buff.append((char)0);
                i--;
            }
            buff.append(c);
        }
        super.writeEscape(buff, inAttribute); 
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

