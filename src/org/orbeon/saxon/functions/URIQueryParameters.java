package org.orbeon.saxon.functions;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.om.Validation;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.Whitespace;
import org.xml.sax.XMLReader;

import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FilenameFilter;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * A set of query parameters on a URI passed to the collection() or document() function
 */

public class URIQueryParameters {

    FilenameFilter filter = null;
    Boolean recurse = null;
    Integer validation = null;
    int strip = Whitespace.UNSPECIFIED;
    Integer onError = null;
    XMLReader parser = null;
    Boolean xinclude = null;

    public static final int ON_ERROR_FAIL = 1;
    public static final int ON_ERROR_WARNING = 2;
    public static final int ON_ERROR_IGNORE = 3;

    /**
     * Create an object representing the query part of a URI
     * @param query the part of the URI after the "?" symbol
     * @param config the Saxon configuration
     */    

    public URIQueryParameters(String query, Configuration config) {
        if (query != null) {
            StringTokenizer t = new StringTokenizer(query, ";&");
            while (t.hasMoreTokens()) {
                String tok = t.nextToken();
                int eq = tok.indexOf('=');
                if (eq > 0 && eq < (tok.length()-1)) {
                    String keyword = tok.substring(0, eq);
                    String value = tok.substring(eq+1);

                    if (keyword.equals("select")) {
                        FastStringBuffer sb = new FastStringBuffer(value.length() + 6);
                        sb.append('^');
                        for (int i=0; i<value.length(); i++) {
                            char c = value.charAt(i);
                            if (c == '.') {
                                // replace "." with "\."
                                sb.append("\\.");
                            } else if (c == '*') {
                                // replace "*" with ".*"
                                sb.append(".*");
                            } else {
                                sb.append(c);
                            }
                        }
                        sb.append('$');
                        String s = sb.toString();
                        Pattern pattern = Pattern.compile(s);
                        filter = new RegexFilter(pattern);
                    } else if (keyword.equals("recurse")) {
                        recurse = Boolean.valueOf("yes".equals(value));
                    } else if (keyword.equals("validation")) {
                        int v = Validation.getCode(value);
                        if (v != Validation.INVALID) {
                            validation = new Integer(v);
                        }
                    } else if (keyword.equals("strip-space")) {
                        if (value.equals("yes")) {
                            strip = Whitespace.ALL;
                        } else if (value.equals("ignorable")) {
                            strip = Whitespace.IGNORABLE;
                        } else if (value.equals("no")) {
                            strip = Whitespace.NONE;
                        }
                    } else if (keyword.equals("xinclude")) {
                        if (value.equals("yes")) {
                            xinclude = Boolean.TRUE;
                        } else if (value.equals("no")) {
                            xinclude = Boolean.FALSE;
                        }
                    } else if (keyword.equals("on-error")) {
                        if (value.equals("warning")) {
                            onError = new Integer(ON_ERROR_WARNING);
                        } else if (value.equals("ignore")) {
                            onError = new Integer(ON_ERROR_IGNORE);
                        } else if (value.equals("fail")) {
                            onError = new Integer(ON_ERROR_FAIL);
                        }
                    } else if (keyword.equals("parser")) {
                        try {
                            if (config == null) {
                                config = new Configuration();
                            }
                            parser = (XMLReader)config.getInstance(value, null);
                        } catch (XPathException err) {
                            try {
                                config.getErrorListener().warning(err);
                            } catch (TransformerException e) {
                                //
                            }
                        }
                    }
                }
            }
        }

    }

    /**
     * Get the value of the strip-space=yes|no parameter. Returns one of the values
     * {@link Whitespace#ALL}, {@link Whitespace#IGNORABLE}, {@link Whitespace#NONE},
     * {@link Whitespace#UNSPECIFIED}
     */

    public int getStripSpace() {
        return strip;
    }

    /**
     * Get the value of the validation=strict|lax|preserve|strip parameter, or null if unspecified
     */

    public Integer getValidationMode() {
        return validation;
    }

    /**
     * Get the file name filter (select=pattern), or null if unspecified
     */

    public FilenameFilter getFilenameFilter() {
        return filter;
    }

    /**
     * Get the value of the recurse=yes|no parameter, or null if unspecified
     */

    public Boolean getRecurse() {
        return recurse;
    }

    /**
     * Get the value of the on-error=fail|warning|ignore parameter, or null if unspecified
     */

    public Integer getOnError() {
        return onError;
    }

    /**
     * Get the value of xinclude=yes|no, or null if unspecified
     */

    public Boolean getXInclude() {
        return xinclude;
    }

    /**
     * Get the selected XML parser, or null if unspecified
     */

    public XMLReader getXMLReader() {
        return parser;
    }

    public static class RegexFilter implements FilenameFilter {

        private Pattern pattern;

        public RegexFilter(Pattern regex) {
            this.pattern = regex;
        }

        /**
         * Tests if a specified file should be included in a file list.
         *
         * @param dir  the directory in which the file was found.
         * @param name the name of the file.
         * @return <code>true</code> if and only if the name should be
         *         included in the file list; <code>false</code> otherwise.
         */

        public boolean accept(File dir, String name) {
            return new File(dir, name).isDirectory() || pattern.matcher(name).matches();
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

