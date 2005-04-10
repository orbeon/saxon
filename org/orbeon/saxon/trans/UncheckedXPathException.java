package net.sf.saxon.trans;

/**
 * When tree construction is deferred, innocuous methods such as NodeInfo#getLocalName() may
 * trigger a dynamic error. Rather than make all such methods on NodeInfo throw a checked XPathException,
 * we instead throw an UncheckedXPathException, which is a simple wrapper for an XPathException.
 * Appropriate places in the code must check for this condition and translate it back into an
 * XPathException.
 */

public class UncheckedXPathException extends RuntimeException {

    private XPathException cause;

    public UncheckedXPathException(XPathException cause) {
        this.cause = cause;
    }

    public XPathException getXPathException() {
        return cause;
    }
}
