package net.sf.saxon.style;
import net.sf.saxon.Configuration;
import net.sf.saxon.Loader;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.om.AttributeCollection;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;

import javax.xml.transform.TransformerConfigurationException;
import java.text.Collator;
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.Comparator;
import java.util.Locale;

/**
* A saxon:collation element in the style sheet: this is a top-level
* element that defines details of a named collation. The attributes of the
* element provide different ways of instantiating an instance of java.util.Comparator
*/

public class SaxonCollation extends StyleElement {

    private String collationName;

    private Comparator collator;      // it's best to supply a java.text.Collator,
                                      // but we can cope with any java.util.Comparator
    private boolean isDefault = false;

    public void prepareAttributes() throws TransformerConfigurationException {

		AttributeCollection atts = getAttributeList();

		String nameAtt = null;              // collation name for use in expressions
		String classAtt = null;             // Java class name of Collator
		String strengthAtt = null;          // primary, secondary, tertiary, or identical
		String decompositionAtt = null;     // canonical, full, or none
		String langAtt = null;              // identifies a locale: country+language
		String rulesAtt = null;             // collation rules as used in RuleBasedCollator
		String defaultAtt = null;

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f==StandardNames.NAME) {
        		nameAtt = atts.getValue(a).trim();
            } else if (f==StandardNames.CLASS) {
                classAtt = atts.getValue(a).trim();
            } else if (f==StandardNames.STRENGTH) {
                strengthAtt = atts.getValue(a).trim();
            } else if (f==StandardNames.DECOMPOSITION) {
                decompositionAtt = atts.getValue(a).trim();
            } else if (f==StandardNames.LANG) {
                langAtt = atts.getValue(a).trim();
            } else if (f==StandardNames.RULES) {
                rulesAtt = atts.getValue(a).trim();
            } else if (f==StandardNames.DEFAULT) {
                defaultAtt = atts.getValue(a).trim();
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (nameAtt!=null) {
            collationName = nameAtt.trim();
        }

        if (classAtt!=null) {
            if (rulesAtt != null || langAtt != null || strengthAtt != null || decompositionAtt != null) {
                compileError("The class attribute cannot be combined with rules, lang, strength, or decomposition");
            }
            try {
                collator = makeCollator(classAtt);
                return;
            } catch (XPathException err) {
                collator = Collator.getInstance();  // so that error paths work
                throw new TransformerConfigurationException(err);
            }
        }

        if (rulesAtt!=null) {
            if (langAtt != null || strengthAtt != null || decompositionAtt != null) {
                compileError("The rules attribute cannot be combined with lang, strength, or decomposition");
            }
            try {
                collator = new RuleBasedCollator(rulesAtt);
            } catch (ParseException e) {
                collator = Collator.getInstance();  // so that error paths work
                compileError("Invalid collation rules: " + e.getMessage());
            }
        }

        // Start with the lang attribute

        if (langAtt!=null) {
            collator = Collator.getInstance(Configuration.getLocale(langAtt));
        } else if (collator == null) {
            collator = Collator.getInstance();  // use default locale
        }

        if (strengthAtt!=null && collator instanceof Collator) {
            if (strengthAtt.equals("primary")) {
                ((Collator)collator).setStrength(Collator.PRIMARY);
            } else if (strengthAtt.equals("secondary")) {
                ((Collator)collator).setStrength(Collator.SECONDARY);
            } else if (strengthAtt.equals("tertiary")) {
                ((Collator)collator).setStrength(Collator.TERTIARY);
            } else if (strengthAtt.equals("identical")) {
                ((Collator)collator).setStrength(Collator.IDENTICAL);
            } else {
                compileError("strength must be primary, secondary, tertiary, or identical");
            }
        }

        if (decompositionAtt!=null && collator instanceof Collator) {
            if (decompositionAtt.equals("none")) {
                ((Collator)collator).setDecomposition(Collator.NO_DECOMPOSITION);
            } else if (decompositionAtt.equals("standard")) {
                ((Collator)collator).setDecomposition(Collator.CANONICAL_DECOMPOSITION);
            } else if (decompositionAtt.equals("full")) {
                ((Collator)collator).setDecomposition(Collator.FULL_DECOMPOSITION);
            } else {
                compileError("decomposition must be none, standard, or full");
            }
        }

        if (defaultAtt != null) {
            if (defaultAtt.equals("yes")) {
                isDefault = true;
            } else if (defaultAtt.equals("no")) {
                // ignore it
            } else {
                compileError("default attribute must be yes or no");
            }
        }

        // register the collation early, so it's available when optimizing XPath expressions
        getPrincipalStylesheet().setCollation(collationName, collator, isDefault);
    }

    public void validate() throws TransformerConfigurationException {
        checkTopLevel(null);
        checkEmpty();
    }

    public Expression compile(Executable exec) throws TransformerConfigurationException {
        getPrincipalStylesheet().setCollation(collationName, collator, isDefault);
        return null;
    }

    public String getCollationName() {
        return collationName;
    }

    public boolean isDefaultCollation() {
        return isDefault;
    }

    public Comparator getCollator() {
        return collator;
    }

    /**
    * Load a named collator class and check it is OK.
    */

    public static Comparator makeCollator (String className) throws XPathException
    {
        Object handler = Loader.getInstance(className);

        if (handler instanceof Comparator ) {
            return (Comparator )handler;
        } else {
            throw new DynamicError("Failed to load collation class " + className +
                        ": it is not an instance of java.util.Comparator");
        }

    }

    /**
    * Utility method to print details of the locales for which a collator
    * is available. (The results depend on the Java VM)
    */

    public static void main(String[] args) {
        System.err.println("The following locales have collations available:");
        Locale[] loc = Collator.getAvailableLocales();
        for (int i=0; i<loc.length; i++) {
            Locale l=loc[i];
            System.err.println("Locale:" +
              ("".equals(l.getCountry()) ? "" : " country='" + l.getCountry() + "' (" + l.getDisplayCountry() + ")" ) +
              ("".equals(l.getLanguage()) ? "" : " language='" + l.getLanguage() + "' (" + l.getDisplayLanguage() + ")" ) +
              ("".equals(l.getVariant()) ? "" : " variant='" + l.getVariant() + "' (" + l.getDisplayVariant() + ")" ));
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
// The Initial Developer of the Original Code is Michael H. Kay of International Computers Limited (mhkay@iclway.co.uk).
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
