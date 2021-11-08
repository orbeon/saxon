package org.orbeon.saxon.om;


/**
  * ProcInstParser is used to parse pseudo-attributes within Processing Instructions
  * @author Michael H. Kay
  * @version 10 July 2000
  */
  

public class ProcInstParser {

    /**
     * Class is never instantiated
     */

    private ProcInstParser() {
    }

    /**
    * Get a pseudo-attribute. This is useful only if the processing instruction data part
    * uses pseudo-attribute syntax, which it does not have to. This syntax is as described
    * in the W3C Recommendation "Associating Style Sheets with XML Documents". 
    * @return the value of the pseudo-attribute if present, or null if not
    */

    public static String getPseudoAttribute(String content, String name) {

        int pos = 0;
        while (pos <= content.length()-4) {     // minimum length of x=""
            int nextQuote = -1;
            for (int q=pos; q<content.length(); q++) {
                if (content.charAt(q)=='"' || content.charAt(q)=='\'') {
                    nextQuote = q;
                    break;
                }
            }
            if (nextQuote < 0) return null;

            int closingQuote = content.indexOf(content.charAt(nextQuote), nextQuote+1);
            if (closingQuote<0) return null;
            int nextName = content.indexOf(name, pos);
            if (nextName < 0) return null;
            if (nextName < nextQuote) {
                // check only spaces and equal signs between the name and the quote
                boolean found = true;
                for (int s = nextName + name.length(); s < nextQuote; s++) {
                    char c = content.charAt(s);
                    if (!Character.isWhitespace(c) && c!='=') {
                        found=false;
                        break;
                    }
                }
                if (found) {
                    String val = content.substring(nextQuote+1, closingQuote);
                    return unescape(val);
                }
            }
            pos = closingQuote + 1;
        }
        return null;
    }

    /**
    * Interpret character references and built-in entity references
    */

    private static String unescape(String value) {
        if (value.indexOf('&')<0) return value;
        FastStringBuffer sb = new FastStringBuffer(value.length());
        for (int i=0; i<value.length(); i++) {
            char c = value.charAt(i);
            if (c=='&') {
                if (i+2 < value.length() && value.charAt(i+1)=='#') {
                    if (value.charAt(i+2)=='x') {
                        int x = i+3;
                        int charval = 0;
                        while (x<value.length() && value.charAt(x)!=';') {
                            int digit = "0123456789abcdef".indexOf(value.charAt(x));
                            if (digit<0) {
                                digit = "0123456789ABCDEF".indexOf(value.charAt(x));
                            }
                            if (digit<0) {
                                return null;
                            }
                            charval = charval * 16 + digit;
                            x++;
                        }
                        char hexchar = (char)charval;
                        sb.append(hexchar);
                        i=x;
                    } else {
                        int x = i+2;
                        int charval = 0;
                        while (x<value.length() && value.charAt(x)!=';') {
                            int digit = "0123456789".indexOf(value.charAt(x));
                            if (digit<0) {
                                return null;
                            }
                            charval = charval * 10 + digit;
                            x++;
                        }
                        char decchar = (char)charval;
                        sb.append(decchar);
                        i=x;
                    }
                } else if (value.substring(i+1).startsWith("lt;")) {
                    sb.append('<');
                    i+=3;
                } else if (value.substring(i+1).startsWith("gt;")) {
                    sb.append('>');
                    i+=3;
                } else if (value.substring(i+1).startsWith("amp;")) {
                    sb.append('&');
                    i+=4;                   
                } else if (value.substring(i+1).startsWith("quot;")) {
                    sb.append('"');
                    i+=5;                                     
                } else if (value.substring(i+1).startsWith("apos;")) {
                    sb.append('\'');
                    i+=5;
                } else {
                    return null;
                }

            } else {
                sb.append(c);
            }
        }
        return sb.toString();
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
