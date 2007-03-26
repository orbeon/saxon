package org.orbeon.saxon.functions;
import org.orbeon.saxon.Err;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.sort.GenericAtomicComparer;
import org.orbeon.saxon.sort.CodepointCollator;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.StaticError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.StringValue;
import org.orbeon.saxon.value.Value;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Comparator;

/**
* Abstract superclass for all functions that take an optional collation argument
*/

// Supports string comparison using a collation

public abstract class CollatingFunction extends SystemFunction {

    // The collation, if known statically
    protected transient Comparator collation = null;  // transient because RuleBasedCollator isn't serializable
    private URI expressionBaseURI = null;

    public void checkArguments(StaticContext env) throws XPathException {
        if (collation == null) {
            saveBaseURI(env, false);
            preEvaluateCollation(env);
        }
        super.checkArguments(env);
    }

    private void saveBaseURI(StaticContext env, boolean fail) throws StaticError {
        if (expressionBaseURI == null) {
            String base = null;
            try {
                base = env.getBaseURI();
                if (base == null) {
                    base = getCurrentDirectory();
                }
                if (base != null) {
                    expressionBaseURI = new URI(base);
                }
            } catch (URISyntaxException e) {
                // perhaps escaping special characters will fix the problem
                if (base != null) {
                    String esc = EscapeURI.iriToUri(base).toString();
                    try {
                        expressionBaseURI = new URI(esc);
                    } catch (URISyntaxException e2) {
                        // don't fail unless the base URI is actually needed (it usually isn't)
                        expressionBaseURI = null;
                    }
                }

                if (expressionBaseURI == null && fail) {
                    StaticError err = new StaticError("The base URI " + Err.wrap(env.getBaseURI(), Err.URI) +
                            " is not a valid URI");
                    err.setLocator(this);
                    throw err;
                }
            }
        }
    }

    private String getCurrentDirectory() {
        String dir;
        try {
            dir = System.getProperty("user.dir");
        } catch (Exception geterr) {
            // this doesn't work when running an applet
            return null;
        }
        if (!(dir.endsWith("/"))) {
            dir = dir + '/';
        }

        try {
            URL currentDirectoryURL = new File(dir).toURL();
            return currentDirectoryURL.toString();
        } catch (MalformedURLException err2) {
            return null;
        }
    }

    /**
     * Pre-evaluate the collation argument if its value is known statically
     */

     private void preEvaluateCollation(StaticContext env) throws XPathException {
        if (getNumberOfArguments() == getDetails().maxArguments) {
            if (argument[getNumberOfArguments()-1] instanceof Value) {
                // Collation is supplied as a constant
                String collationName = ((Value)argument[getNumberOfArguments()-1]).getStringValue();
                URI collationURI;
                try {
                    collationURI = new URI(collationName);
                    if (!collationURI.isAbsolute()) {
                        saveBaseURI(env, true);
                        if (expressionBaseURI == null) {
                            StaticError err = new StaticError(
                                    "The collation name is a relative URI, but the base URI is unknown");
                            err.setErrorCode("XPST0001");
                            err.setLocator(this);
                            throw err;
                        }
                        URI base = expressionBaseURI;
                        collationURI = base.resolve(collationURI);
                        collationName = collationURI.toString();
                    }
                } catch (URISyntaxException e) {
                    StaticError err = new StaticError("Collation name '" + collationName + "' is not a valid URI");
                    err.setErrorCode("FOCH0002");
                    err.setLocator(this);
                    throw err;
                }
                collation = env.getCollation(collationName);
                if (collation == null) {
                    StaticError err = new StaticError("Unknown collation " + Err.wrap(collationName, Err.URI));
                    err.setErrorCode("FOCH0002");
                    err.setLocator(this);
                    throw err;
                }
            } else {
                // collation isn't known until run-time
            }
        } else {
            // Use the default collation
            String uri = env.getDefaultCollationName();
            collation = env.getCollation(uri);
        }
    }

    /**
    * Get a GenericAtomicComparer that can be used to compare values
    * @param arg the position of the argument (starting at 0) containing the collation name.
    * If this argument was not supplied, the default collation is used
    * @param context The dynamic evaluation context.
    */

    protected GenericAtomicComparer getAtomicComparer(int arg, XPathContext context) throws XPathException {
        return new GenericAtomicComparer(getCollator(arg, context), context);
    }

    /**
    * Get a collator suitable for comparing strings. Returns the collator specified in the
    * given function argument if present, otherwise returns the default collator.
     * @param arg The argument position (counting from zero) that holds the collation
     * URI if present
     * @param context The dynamic context
     * @return a Comparator, which will either be a java.text.Collator, or a CodepointCollator
    */

    protected Comparator getCollator(int arg, XPathContext context) throws XPathException {

        if (collation != null) {
            // the collation was determined statically
            return collation;
        } else {
            int numargs = argument.length;
            if (numargs > arg) {
                AtomicValue av = (AtomicValue)argument[arg].evaluateItem(context);
                StringValue collationValue = (StringValue)av.getPrimitiveValue();
                String collationName = collationValue.getStringValue();
                URI collationURI;
                try {
                    collationURI = new URI(collationName);
                    if (!collationURI.isAbsolute()) {
                        if (expressionBaseURI == null) {
                            DynamicError err = new DynamicError(
                                    "Cannot resolve relative collation URI '" + collationName +
                                    "': unknown or invalid base URI");
                            err.setErrorCode("FOCH0002");
                            err.setXPathContext(context);
                            err.setLocator(this);
                            throw err;
                        }
                        collationURI = expressionBaseURI.resolve(collationURI);
                        collationName = collationURI.toString();
                    }
                } catch (URISyntaxException e) {
                    DynamicError err = new DynamicError("Collation name '" + collationName + "' is not a valid URI");
                    err.setErrorCode("FOCH0002");
                    err.setXPathContext(context);
                    err.setLocator(this);
                    throw err;
                }
                return context.getCollation(collationName);
            } else {
                Comparator collator = context.getDefaultCollation();
                return (collator==null ? CodepointCollator.getInstance() : collator);
            }
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
