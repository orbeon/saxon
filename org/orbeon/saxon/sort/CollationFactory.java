package net.sf.saxon.sort;
import net.sf.saxon.Loader;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.StringTokenizer;

/**
* CollationFactory allows a Collation to be created given a set of parameters,
 * or a URI containing those parameters.
*/

public class CollationFactory {



    public static Collator makeUsingProperties(
            String langAtt,
            String strengthAtt,
            String decompositionAtt )
    {

        Collator collator = null;
        // Start with the lang attribute

        if (langAtt!=null) {
            collator = Collator.getInstance(getLocale(langAtt));
        } else {
            collator = Collator.getInstance();  // use default locale
        }

        if (strengthAtt!=null && collator instanceof Collator) {
            if (strengthAtt.equals("primary")) {
                collator.setStrength(Collator.PRIMARY);
            } else if (strengthAtt.equals("secondary")) {
                collator.setStrength(Collator.SECONDARY);
            } else if (strengthAtt.equals("tertiary")) {
                collator.setStrength(Collator.TERTIARY);
            } else if (strengthAtt.equals("identical")) {
                collator.setStrength(Collator.IDENTICAL);
            } else {
                //throw new XPathException.Dynamic("Collation strength must be primary, secondary, tertiary, or identical");
            }
        }

        if (decompositionAtt!=null && collator instanceof Collator) {
            if (decompositionAtt.equals("none")) {
                collator.setDecomposition(Collator.NO_DECOMPOSITION);
            } else if (decompositionAtt.equals("standard")) {
                collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
            } else if (decompositionAtt.equals("full")) {
                collator.setDecomposition(Collator.FULL_DECOMPOSITION);
            } else {
                //throw new XPathException.Dynamic("Collation decomposition must be none, standard, or full");
            }
        }
        return collator;
    }

    /**
    * Get a locale given a language code in XML format
    */

    private static Locale getLocale(String lang) {
        int hyphen = lang.indexOf("-");
        String language, country;
        if (hyphen < 1) {
            language = lang;
            country = "";
        } else {
            language = lang.substring(1, hyphen);
            country = lang.substring(hyphen+1);
        }
        return new Locale(language, country);
    }

    /**
    * Load a named collator class and check it is OK.
    */

    public static Comparator makeComparator (String className) throws XPathException
    {
        Object obj = Loader.getInstance(className);

        if (obj instanceof Comparator ) {
            return (Comparator)obj;
        } else {
            throw new DynamicError("Failed to load collation class " + className +
                        ": it is not an instance of java.util.Comparator");
        }

    }

    /**
     * Create a collator from a parameterized URI
     * @return null if the collation URI is not suitable
     */

    public static Comparator makeCollationFromURI(String uri) throws XPathException {
        if (uri.equals("http://saxon.sf.net/collation")) {
            return makeUsingProperties(null, null, null);
        } else if (uri.startsWith("http://saxon.sf.net/collation?")) {
            URI uuri;
            try {
                uuri = new URI(uri);
            } catch (URISyntaxException err) {
                return null;
            }
            String query = uuri.getQuery();
            String lang = null;
            String strength = null;
            String decomposition = null;
            String classname = null;
            StringTokenizer queryTokenizer = new StringTokenizer(query, ";&");
            while (queryTokenizer.hasMoreElements()) {
                String param = queryTokenizer.nextToken();
                int eq = param.indexOf('=');
                if (eq > 0 && eq < param.length()-1) {
                    String kw = param.substring(0, eq);
                    String val = param.substring(eq + 1);
                    if (kw.equals("lang")) {
                        lang = val;
                    } else if (kw.equals("strength")) {
                        strength = val;
                    } else if (kw.equals("decomposition")) {
                        decomposition = val;
                    } else if (kw.equals("class")) {
                        classname = val;
                    }
                }
            }
            if (classname != null) {
                return makeComparator(classname);
            }
            return makeUsingProperties(lang, strength, decomposition);
        } else {
            return null;
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
