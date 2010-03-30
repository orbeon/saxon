package org.orbeon.saxon.functions;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.trans.Err;
import org.orbeon.saxon.Platform;
import org.orbeon.saxon.expr.ExpressionVisitor;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.AnyURIValue;
import org.orbeon.saxon.value.AtomicValue;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
* This class supports the resolve-uri() functions in XPath 2.0
*/

public class ResolveURI extends SystemFunction {

    String expressionBaseURI = null;

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        if (expressionBaseURI == null) {
            super.checkArguments(visitor);
            expressionBaseURI = visitor.getStaticContext().getBaseURI();
            if (expressionBaseURI == null && argument.length == 1) {
                XPathException de = new XPathException("Base URI in static context of resolve-uri() is unknown");
                de.setErrorCode("FONS0005");
                throw de;
            }
        }
    }

    /**
     * Get the static base URI of the expression
     */

    public String getStaticBaseURI() {
        return expressionBaseURI;
    }

    /**
     * Copy an expression. This makes a deep copy.
     * @return the copy of the original expression
     */

    public Expression copy() {
        ResolveURI d = (ResolveURI)super.copy();
        d.expressionBaseURI = expressionBaseURI;
        return d;
    }

    /**
    * Evaluate the function at run-time
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        AtomicValue arg0 = (AtomicValue)argument[0].evaluateItem(context);
        if (arg0 == null) {
            return null;
        }
        String relative = arg0.getStringValue();
        String base;
        if (argument.length == 2) {
            base = argument[1].evaluateAsString(context).toString();
        } else {
            base = expressionBaseURI;
            if (expressionBaseURI == null) {
                dynamicError("Base URI in static context of resolve-uri() is unknown", "FONS0005", context);
                return null;
            }
        }

        Platform platform = Configuration.getPlatform();
        try {
            URI resolved = platform.makeAbsolute(relative,  base);
            return new AnyURIValue(resolved.toString());
        } catch (URISyntaxException err) {
            dynamicError("Base URI " + Err.wrap(base) + " is invalid: " + err.getMessage(),
                    "FORG0002", context);
            return null;
        }
    }

    /**
    * If a system ID can't be parsed as a URL, try to expand it as a relative
    * URI using the current directory as the base URI.
    */

    public static String tryToExpand(String systemId) {
        if (systemId==null) {
            systemId = "";
        }
	    try {
	        new URL(systemId);
	        return systemId;   // all is well
	    } catch (MalformedURLException err) {
	        String dir;
	        try {
	            dir = System.getProperty("user.dir");
	        } catch (Exception geterr) {
	            // this doesn't work when running an applet
	            return systemId;
	        }
	        if (!(dir.endsWith("/") || systemId.startsWith("/"))) {
	            dir = dir + '/';
	        }

            URI currentDirectoryURI = new File(dir).toURI();
            URI baseURI = currentDirectoryURI.resolve(systemId);
            return baseURI.toString();

	    }
	}

    /**
     * Replace spaces by %20
     */

    public static String escapeSpaces(String s) {
        // It's not entirely clear why we have to escape spaces by hand, and not other special characters;
        // it's just that tests with a variety of filenames show that this approach seems to work.
        if (s == null) return s;
        int i = s.indexOf(' ');
        if (i < 0) {
            return s;
        }
        return (i == 0 ? "" : s.substring(0, i))
                + "%20"
                + (i == s.length()-1 ? "" : escapeSpaces(s.substring(i+1)));
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
