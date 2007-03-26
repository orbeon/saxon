package org.orbeon.saxon.sort;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;

import java.text.Collator;
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.Comparator;
import java.util.Locale;
import java.util.Properties;

/**
 * A JavaCollationFactory allows a Collation to be created given
 * a set of properties that the collation should have. This class creates a collator using
 * the facilities of the Java platform; there is a corresponding class that uses the .NET
 * platform.
*/

public abstract class JavaCollationFactory {

    /**
     * The class is a never instantiated
     */
    private JavaCollationFactory() {
    }

    /**
     * Make a collator with given properties
     * @param config the Configuration
     * @param props the desired properties of the collation
     * @return a collation with these properties
     */

    public static Comparator makeCollation(Configuration config, Properties props) throws XPathException {

        Comparator collator = null;

        // If a specific collation class is requested, this overrides everything else

        String classAtt = props.getProperty("class");
        if (classAtt != null) {
            Object comparator = config.getInstance(classAtt, null);
            if (comparator instanceof Comparator) {
                collator = (Comparator)comparator;
            } else {
                throw new DynamicError("Requested collation class " + classAtt + " is not a Comparator");
            }
        }

        // If rules are specified, create a RuleBasedCollator

        String rulesAtt = props.getProperty("rules");
        if (rulesAtt!=null && collator==null) {
            try {
                collator = new RuleBasedCollator(rulesAtt);
            } catch (ParseException e) {
                throw new DynamicError("Invalid collation rules: " + e.getMessage());
            }
        }

        // Start with the lang attribute

        if (collator == null) {
            String langAtt = props.getProperty("lang");
            if (langAtt!=null) {
                collator = Collator.getInstance(getLocale(langAtt));
            } else {
                collator = Collator.getInstance();  // use default locale
            }
        }

        // See if there is a strength attribute
        String strengthAtt = props.getProperty("strength");
        if (strengthAtt!=null) {
            if (strengthAtt.equals("primary") && collator instanceof Collator) {
                ((Collator)collator).setStrength(Collator.PRIMARY);
            } else if (strengthAtt.equals("secondary")) {
                ((Collator)collator).setStrength(Collator.SECONDARY);
            } else if (strengthAtt.equals("tertiary")) {
                ((Collator)collator).setStrength(Collator.TERTIARY);
            } else if (strengthAtt.equals("identical")) {
                ((Collator)collator).setStrength(Collator.IDENTICAL);
            } else {
                throw new DynamicError("strength must be primary, secondary, tertiary, or identical");
            }
        }

        // Look for the properties ignore-case, ignore-modifiers, ignore-width

        String ignore = props.getProperty("ignore-width");
        if (ignore != null) {
            if (ignore.equals("yes") && strengthAtt == null && collator instanceof Collator) {
                ((Collator)collator).setStrength(Collator.TERTIARY);
            } else if (ignore.equals("no")) {
                // no-op
            } else {
                throw new DynamicError("ignore-width must be yes or no");
            }
        }

        ignore = props.getProperty("ignore-case");
        if (ignore != null && strengthAtt == null && collator instanceof Collator) {
            if (ignore.equals("yes")) {
                ((Collator)collator).setStrength(Collator.SECONDARY);
            } else if (ignore.equals("no")) {
                // no-op
            } else {
                throw new DynamicError("ignore-case must be yes or no");
            }
        }

        ignore = props.getProperty("ignore-modifiers");
        if (ignore != null) {
            if (ignore.equals("yes") && strengthAtt == null && collator instanceof Collator) {
                ((Collator)collator).setStrength(Collator.PRIMARY);
            } else if (ignore.equals("no")) {
                // no-op
            } else {
                throw new DynamicError("ignore-modifiers must be yes or no");
            }
        }

        // The ignore-symbols property is ignored

        // See if there is a decomposition attribute
        String decompositionAtt = props.getProperty("decomposition");
        if (decompositionAtt!=null && collator instanceof Collator) {
            if (decompositionAtt.equals("none")) {
                ((Collator)collator).setDecomposition(Collator.NO_DECOMPOSITION);
            } else if (decompositionAtt.equals("standard")) {
                ((Collator)collator).setDecomposition(Collator.CANONICAL_DECOMPOSITION);
            } else if (decompositionAtt.equals("full")) {
                ((Collator)collator).setDecomposition(Collator.FULL_DECOMPOSITION);
            } else {
                throw new DynamicError("decomposition must be non, standard, or full");
            }
        }

        // See if there is a case-order property
        String caseOrder = props.getProperty("case-order");
        if (caseOrder != null && !"#default".equals(caseOrder)) {
            if (collator instanceof Collator) {
                // force base collator to ignore case differences
                ((Collator)collator).setStrength(Collator.SECONDARY);
            }
            if (caseOrder.equals("lower-first")) {
                collator = new LowercaseFirstComparer(collator);
            } else if (caseOrder.equals("upper-first")) {
                collator = new UppercaseFirstComparer(collator);
            } else {
                throw new DynamicError("case-order must be lower-first, upper-first, or #default");
            }
        };

        // See if there is an alphanumeric property
        String alphanumeric = props.getProperty("alphanumeric");
        if (alphanumeric != null && !"no".equals(alphanumeric)) {
            if (alphanumeric.equals("yes")) {
                collator = new AlphanumericComparer(collator);
            } else {
                throw new DynamicError("alphanumeric must be yes or no");
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
