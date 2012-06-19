package org.orbeon.saxon.sort;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.trans.XPathException;

import javax.xml.transform.TransformerException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * StandardCollationURIResolver allows a Collation to be created given
 * a URI starting with "http://saxon.sf.net/collation" followed by a set of query parameters.
*/

public class StandardCollationURIResolver implements CollationURIResolver {

    private static final StandardCollationURIResolver theInstance = new StandardCollationURIResolver();

    /**
     * The class is a singleton
     */
    private StandardCollationURIResolver() {
    }

    /**
     * Return the singleton instance of this class
     */

    public static final StandardCollationURIResolver getInstance() {
        return theInstance;
    }


    /**
     * Create a collator from a parameterized URI
     * @return null if the collation URI is not recognized. If the collation URI is recognized but contains
     * errors, the method returns null after sending a warning to the ErrorListener.
     */

    public StringCollator resolve(String uri, String base, Configuration config) {
        try {
            if (uri.equals("http://saxon.sf.net/collation")) {
                return Configuration.getPlatform().makeCollation(config, new Properties(), uri);
            } else if (uri.startsWith("http://saxon.sf.net/collation?")) {
                URI uuri;
                try {
                    uuri = new URI(uri);
                } catch (URISyntaxException err) {
                    throw new XPathException(err);
                }
                Properties props = new Properties();
                String query = uuri.getRawQuery();
                StringTokenizer queryTokenizer = new StringTokenizer(query, ";&");
                while (queryTokenizer.hasMoreElements()) {
                    String param = queryTokenizer.nextToken();
                    int eq = param.indexOf('=');
                    if (eq > 0 && eq < param.length()-1) {
                        String kw = param.substring(0, eq);
                        String val = decode(param.substring(eq + 1));
                        props.setProperty(kw, val);
                    }
                }
                return Configuration.getPlatform().makeCollation(config, props, uri);
            } else {
                return null;
            }
        } catch (XPathException e) {
            try {
                config.getErrorListener().warning(e);
            } catch (TransformerException e1) {
                //
            }
            return null;
        }
    }

    public static String decode(String s) {
        // Evaluates all escapes in s, applying UTF-8 decoding if needed.  Assumes
        // that escapes are well-formed syntactically, i.e., of the form %XX.  If a
        // sequence of escaped octets is not valid UTF-8 then the erroneous octets
        // are replaced with '\uFFFD'.
        // Exception: any "%" found between "[]" is left alone. It is an IPv6 literal
        //            with a scope_id
        //

        if (s == null) {
            return s;
        }
        int n = s.length();
        if (n == 0) {
            return s;
        }
        if (s.indexOf('%') < 0) {
            return s;
        }

        FastStringBuffer sb = new FastStringBuffer(n);
        ByteBuffer bb = ByteBuffer.allocate(n);
        Charset utf8 = Charset.forName("UTF-8");

        // This is not horribly efficient, but it will do for now
        char c = s.charAt(0);
        boolean betweenBrackets = false;

        for (int i = 0; i < n;) {
            assert c == s.charAt(i);    // Loop invariant
            if (c == '[') {
                betweenBrackets = true;
            } else if (betweenBrackets && c == ']') {
                betweenBrackets = false;
            }
            if (c != '%' || betweenBrackets) {
                sb.append(c);
                if (++i >= n) {
                    break;
                }
                c = s.charAt(i);
                continue;
            }
            bb.clear();
            for (; ;) {
                assert (n - i >= 2);
                bb.put(hex(s.charAt(++i), s.charAt(++i)));
                if (++i >= n) {
                    break;
                }
                c = s.charAt(i);
                if (c != '%') {
                    break;
                }
            }
            bb.flip();
            sb.append(utf8.decode(bb));
        }

        return sb.toString();
    }

    private static byte hex(char high, char low) {
        return (byte)((hexToDec(high)<<4) | hexToDec(low));
    }

    private static int hexToDec(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        } else if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        } else if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        } else {
            return 0;
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
// The Initial Developer of the Original Code is Michael Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
