package org.orbeon.saxon.functions;

import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.sort.CodepointCollator;
import org.orbeon.saxon.sort.GenericAtomicComparer;
import org.orbeon.saxon.sort.StringCollator;
import org.orbeon.saxon.trans.Err;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.StringValue;
import org.orbeon.saxon.value.Value;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Abstract superclass for all functions that take an optional collation argument
 */

// Supports string comparison using a collation

public abstract class CollatingFunction extends SystemFunction {

    // The collation, if known statically
    protected StringCollator stringCollator = null;
    private URI expressionBaseURI = null;

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        if (stringCollator == null) {
            StaticContext env = visitor.getStaticContext();
            saveBaseURI(env, false);
            preEvaluateCollation(env);
        }
        super.checkArguments(visitor);
    }

    private void saveBaseURI(StaticContext env, boolean fail) throws XPathException {
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

                String esc = EscapeURI.iriToUri(base).toString();
                try {
                    expressionBaseURI = new URI(esc);
                } catch (URISyntaxException e2) {
                    // don't fail unless the base URI is actually needed (it usually isn't)
                    expressionBaseURI = null;
                }

                if (expressionBaseURI == null && fail) {
                    XPathException err = new XPathException("The base URI " + Err.wrap(env.getBaseURI(), Err.URI) +
                            " is not a valid URI");
                    err.setLocator(this);
                    throw err;
                }
            }
        }
    }

    /**
     * Get the saved static base URI
     * @return the static base URI
     */

    public URI getExpressionBaseURI() {
        return expressionBaseURI;
    }

    /**
     * Get the collation if known statically, as a StringCollator object
     * @return a StringCollator. Return null if the collation is not known statically.
     */

    public StringCollator getStringCollator() {
        return stringCollator;
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

        URI currentDirectoryURL = new File(dir).toURI();
        return currentDirectoryURL.toString();
    }

    /**
     * Pre-evaluate the collation argument if its value is known statically
     * @param env the static XPath evaluation context
     */

    private void preEvaluateCollation(StaticContext env) throws XPathException {
        if (getNumberOfArguments() == getDetails().maxArguments) {
            final Expression collationExp = argument[getNumberOfArguments() - 1];
            final Value collationVal = (collationExp instanceof Literal ? ((Literal)collationExp).getValue() : null);
            if (collationVal instanceof AtomicValue) {
                // Collation is supplied as a constant
                String collationName = collationVal.getStringValue();
                URI collationURI;
                try {
                    collationURI = new URI(collationName);
                    if (!collationURI.isAbsolute()) {
                        saveBaseURI(env, true);
                        if (expressionBaseURI == null) {
                            XPathException err = new XPathException("The collation name is a relative URI, but the base URI is unknown");
                            err.setErrorCode("XPST0001");
                            err.setIsStaticError(true);
                            err.setLocator(this);
                            throw err;
                        }
                        URI base = expressionBaseURI;
                        collationURI = base.resolve(collationURI);
                        collationName = collationURI.toString();
                    }
                } catch (URISyntaxException e) {
                    XPathException err = new XPathException("Collation name '" + collationName + "' is not a valid URI");
                    err.setErrorCode("FOCH0002");
                    err.setIsStaticError(true);
                    err.setLocator(this);
                    throw err;
                }
                StringCollator comp = env.getCollation(collationName);
                if (comp == null) {
                    XPathException err = new XPathException("Unknown collation " + Err.wrap(collationName, Err.URI));
                    err.setErrorCode("FOCH0002");
                    err.setIsStaticError(true);
                    err.setLocator(this);
                    throw err;
                } else {
                    stringCollator = comp;
                }
            } else {
                // collation isn't known until run-time
            }
        } else {
            // Use the default collation
            String uri = env.getDefaultCollationName();
            stringCollator = env.getCollation(uri);
        }
    }


    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        CollatingFunction d = (CollatingFunction)super.copy();
        d.expressionBaseURI = expressionBaseURI;
        d.stringCollator = stringCollator;
        return d;
    }

    /**
     * Get a GenericAtomicComparer that can be used to compare values. This method is called
     * at run time by subclasses to evaluate the parameter containing the collation name.
     * <p/>
     * <p>The difference between this method and {@link #getCollator} is that a
     * GenericAtomicComparer is capable of comparing values of any atomic type, not only
     * strings. It is therefore called by functions such as compare, deep-equal, index-of, and
     * min() and max() where the operands may include a mixture of strings and other types.</p>
     *
     * @param arg     the position of the argument (starting at 0) containing the collation name.
     *                If this argument was not supplied, the default collation is used
     * @param context The dynamic evaluation context.
     * @return a GenericAtomicComparer that can be used to compare atomic values.
     */

    protected GenericAtomicComparer getAtomicComparer(int arg, XPathContext context) throws XPathException {
        // TODO:PERF avoid creating a new object on each call when the collation is specified dynamically
        return new GenericAtomicComparer(getCollator(arg, context), context);
    }

    /**
     * Get a collator suitable for comparing strings. Returns the collator specified in the
     * given function argument if present, otherwise returns the default collator. This method is
     * called by subclasses at run time. It is used (in contrast to {@link #getAtomicComparer})
     * when it is known that the values to be compared are always strings.
     *
     * @param arg     The argument position (counting from zero) that holds the collation
     *                URI if present
     * @param context The dynamic context
     * @return a StringCollator
     */

    protected StringCollator getCollator(int arg, XPathContext context) throws XPathException {

        if (stringCollator != null) {
            // the collation was determined statically
            return stringCollator;
        } else {
            int numargs = argument.length;
            if (numargs > arg) {
                AtomicValue av = (AtomicValue) argument[arg].evaluateItem(context);
                StringValue collationValue = (StringValue) av;
                String collationName = collationValue.getStringValue();
                URI collationURI;
                try {
                    collationURI = new URI(collationName);
                    if (!collationURI.isAbsolute()) {
                        if (expressionBaseURI == null) {
                            XPathException err = new XPathException("Cannot resolve relative collation URI '" + collationName +
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
                    XPathException err = new XPathException("Collation name '" + collationName + "' is not a valid URI");
                    err.setErrorCode("FOCH0002");
                    err.setXPathContext(context);
                    err.setLocator(this);
                    throw err;
                }
                return context.getCollation(collationName);
            } else {
                StringCollator collator = context.getDefaultCollation();
                return (collator == null ? CodepointCollator.getInstance() : collator);
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
