package net.sf.saxon.xpath;

import net.sf.saxon.expr.XPathContext;

import javax.xml.transform.SourceLocator;

/**
* Subclass of XPathException used for dynamic errors
*/

public class DynamicError extends XPathException {

    XPathContext context;
    boolean isTypeError = false;

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

}
