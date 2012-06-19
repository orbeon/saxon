package org.orbeon.saxon.trans;

import org.orbeon.saxon.expr.XPathContext;

import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;

/**
* Subclass of XPathException used for dynamic errors
 * @deprecated since 9.0 - use the superclass XPathException instead
*/

public class DynamicError extends XPathException {

    transient XPathContext context;
    // declared transient because a compiled stylesheet might contain a "deferred action" dynamic error
    // and the EarlyEvaluationContext links back to the source stylesheet.

    public DynamicError(String message) {
        super(message);
    }

    public DynamicError(Throwable err) {
        super(err);
    }

    public DynamicError(String message, Throwable err) {
        super(message, err);
    }

    public DynamicError(String message, SourceLocator loc) {
        super(message, loc);
    }

    public DynamicError(String message, SourceLocator loc, Throwable err) {
        super(message, loc, err);
    }

    public DynamicError(String message, String errorCode) {
        super(message);
        setErrorCode(errorCode);
    }

    public DynamicError(String message, String errorCode, XPathContext context) {
        super(message);
        setErrorCode(errorCode);
        setXPathContext(context);
    }

    public void setXPathContext(XPathContext context) {
        this.context = context;
    }

    public XPathContext getXPathContext() {
        return context;
    }

    public static DynamicError makeDynamicError(TransformerException err) {
        if (err instanceof DynamicError) {
            return (DynamicError)err;
        } else if (err instanceof XPathException) {
            DynamicError de = new DynamicError(err);
            de.setErrorCode(((XPathException)err).getErrorCodeLocalPart());
            de.setLocator(err.getLocator());
            return de;
        } else {
            return new DynamicError(err);
        }
    }

    /**
     * Set the location and/or context of a message, only if they are
     * not already set
     * @param locator the current location (or null)
     * @param context the current context (or null)
     */

    public void maybeSetLocation(SourceLocator locator, XPathContext context) {
        if ((getLocator() == null || getLocator().getLineNumber() == -1) && locator != null) {
            setLocator(locator);
        }
        if (getXPathContext() == null && context != null) {
            setXPathContext(context);
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//