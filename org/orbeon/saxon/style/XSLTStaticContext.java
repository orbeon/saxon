package org.orbeon.saxon.style;

import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.xpath.XPathException;

/**
 * Extends the standard XPath static context with information that is available for
 * XPath expressions invoked from XSLT
 */

public interface XSLTStaticContext extends StaticContext {

   /**
    * Determine if an extension element is available
    * @throws org.orbeon.saxon.xpath.XPathException if the name is invalid or the prefix is not declared
    */

    public boolean isElementAvailable(String qname) throws XPathException;
}
