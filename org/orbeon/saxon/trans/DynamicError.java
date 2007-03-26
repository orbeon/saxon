package org.orbeon.saxon.trans;

import org.orbeon.saxon.expr.XPathContext;

import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;

/**
* Subclass of XPathException used for dynamic errors
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
}
