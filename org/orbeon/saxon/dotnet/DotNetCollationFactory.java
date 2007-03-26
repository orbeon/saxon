package org.orbeon.saxon.dotnet;
import cli.System.Globalization.CompareInfo;
import cli.System.Globalization.CompareOptions;
import cli.System.Globalization.CultureInfo;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.sort.AlphanumericComparer;
import org.orbeon.saxon.sort.LowercaseFirstComparer;
import org.orbeon.saxon.sort.UppercaseFirstComparer;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;

import java.text.Collator;
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.Comparator;
import java.util.Properties;

/**
 * A DotNetCollationFactory allows a Collation to be created given
 * a set of properties that the collation should have. This class uses the services
 * of the .NET platform; there is a corresponding class that uses the Java collation facilities.
*/

public abstract class DotNetCollationFactory {

    /**
     * The class is a never instantiated
     */
    private DotNetCollationFactory() {
    }

    /**
     * Make a collator with given properties
     * @param config the Configuration
     * @param props the desired properties of the collation
     * @return a collation with these properties
     */

    public static Comparator makeCollation(Configuration config, Properties props) throws XPathException {

        Comparator collator = null;

        // If a specific collation class is requested, this overrides everything else. Note that
        // the class is loaded as a Java class, not as a .NET class.

        String classAtt = props.getProperty("class");
        if (classAtt != null) {
            Object comparator = config.getInstance(classAtt, null);
            if (comparator instanceof Comparator) {
                collator = (Comparator)comparator;
            } else {
                throw new DynamicError("Requested collation class " + classAtt + " is not a Comparator");
            }
        }

        // If rules are specified, create a RuleBasedCollator. In this case we use the Java platform facilities
        // available from the GNU Classpath library.

        String rulesAtt = props.getProperty("rules");
        if (rulesAtt!=null && collator==null) {
            try {
                collator = new RuleBasedCollator(rulesAtt);
            } catch (ParseException e) {
                throw new DynamicError("Invalid collation rules: " + e.getMessage());
            }
        }

        // Start with the lang attribute

        CompareInfo info;
        int options = 0;

        String langAtt = props.getProperty("lang");
        if (langAtt!=null) {
            info = CultureInfo.CreateSpecificCulture(langAtt).get_CompareInfo();
        } else {
            info = CultureInfo.get_CurrentCulture().get_CompareInfo();  // use default locale
        }

        // See if there is a strength attribute
        String strengthAtt = props.getProperty("strength");
        if (strengthAtt!=null && options==0) {
            if (strengthAtt.equals("primary")) {
                 options = CompareOptions.IgnoreCase | CompareOptions.IgnoreNonSpace | CompareOptions.IgnoreWidth;
            } else if (strengthAtt.equals("secondary")) {
                 options = CompareOptions.IgnoreCase | CompareOptions.IgnoreWidth;
            } else if (strengthAtt.equals("tertiary")) {
                 options = CompareOptions.IgnoreWidth;
            } else if (strengthAtt.equals("identical")) {
                 options = 0;
            } else {
                throw new DynamicError("strength must be primary, secondary, tertiary, or identical");
            }
        }

        // Look for the properties ignore-case, ignore-modifiers, ignore-width

        String ignore = props.getProperty("ignore-case");
        if (ignore != null) {
            if (ignore.equals("yes")) {
                options |= CompareOptions.IgnoreCase;
            } else if (ignore.equals("no")) {
                // no-op
            } else {
                throw new DynamicError("ignore-case must be yes or no");
            }
        }

        ignore = props.getProperty("ignore-modifiers");
        if (ignore != null) {
            if (ignore.equals("yes")) {
                options |= CompareOptions.IgnoreNonSpace;
            } else if (ignore.equals("no")) {
                // no-op
            } else {
                throw new DynamicError("ignore-modifiers must be yes or no");
            }
        }

        ignore = props.getProperty("ignore-symbols");
        if (ignore != null) {
            if (ignore.equals("yes")) {
                options |= CompareOptions.IgnoreSymbols;
            } else if (ignore.equals("no")) {
                // no-op
            } else {
                throw new DynamicError("ignore-symbols must be yes or no");
            }
        }

        ignore = props.getProperty("ignore-width");
        if (ignore != null) {
            if (ignore.equals("yes")) {
                options |= CompareOptions.IgnoreWidth;
            } else if (ignore.equals("no")) {
                // no-op
            } else {
                throw new DynamicError("ignore-width must be yes or no");
            }
        }

        if (collator == null) {
            collator = new DotNetComparator(info, CompareOptions.wrap(options));
        }

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
            // force the base collation to ignore case
            options |= CompareOptions.IgnoreCase;
            collator = new DotNetComparator(info, CompareOptions.wrap(options));
            if (caseOrder.equals("lower-first")) {
                collator = new LowercaseFirstComparer(collator);
            } else if (caseOrder.equals("upper-first")) {
                collator = new UppercaseFirstComparer(collator);
                //System.err.println("Creating UCFirstComparer");
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
