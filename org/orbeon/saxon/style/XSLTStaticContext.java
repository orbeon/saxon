package net.sf.saxon.style;

import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.trans.XPathException;

/**
 * Extends the standard XPath static context with information that is available for
 * XPath expressions invoked from XSLT
 */

public interface XSLTStaticContext extends StaticContext {

   /**
    * Determine if an extension element is available
    * @throws net.sf.saxon.trans.XPathException if the name is invalid or the prefix is not declared
    */

    public boolean isElementAvailable(String qname) throws XPathException;
}
