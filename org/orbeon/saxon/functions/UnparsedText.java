package net.sf.saxon.functions;
import net.sf.saxon.Err;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.FastStringBuffer;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.StringValue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;


public class UnparsedText extends SystemFunction implements XSLTFunction {

    String expressionBaseURI = null;

    public void checkArguments(StaticContext env) throws XPathException {
        if (expressionBaseURI == null) {
            super.checkArguments(env);
            expressionBaseURI = env.getBaseURI();
        }
    }


    /**
    * preEvaluate: this method suppresses compile-time evaluation by doing nothing
    */

    public Expression preEvaluate(StaticContext env) {
        return this;
        // in principle we could pre-evaluate any call of unparsed-text() with
        // constant arguments. But we don't, because the file contents might
        // change before the stylesheet executes.
    }


    /**
    * evaluateItem() handles evaluation of the function:
    * it returns a String
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        StringValue hrefVal = (StringValue)argument[0].evaluateItem(context);
        if (hrefVal == null) {
            return null;
        }
        String href = hrefVal.getStringValue();

        String encoding = argument[1].evaluateItem(context).getStringValue();

        return new StringValue(
                    readFile(href, expressionBaseURI, encoding)
                   );
    }

    /**
    * Supporting routine to load one external file given a URI (href) and a baseURI
    */

    private CharSequence readFile(String href, String baseURI, String encoding)
    throws XPathException {

        // Resolve relative URI

        URL absoluteURL;
        if (baseURI==null) {    // no base URI available
            try {
                // the href might be an absolute URL
                absoluteURL = new URL(href);
            } catch (MalformedURLException err) {
                // it isn't
                DynamicError e = new DynamicError("Cannot resolve absolute URI", err);
                e.setErrorCode("XT1170");
                throw e;
            }
        } else {
            try {
                absoluteURL = new URL(new URL(baseURI), href);
            } catch (MalformedURLException err) {
                DynamicError e =  new DynamicError("Cannot resolve relative URI", err);
                e.setErrorCode("XT1170");
                throw e;
            }
        }
        try {
            InputStream is = absoluteURL.openStream();
            BufferedReader reader = new BufferedReader(
                                        new InputStreamReader(is, encoding));

            FastStringBuffer sb = new FastStringBuffer(2048);
            char[] buffer = new char[2048];
            int actual;
            while (true) {
                actual = reader.read(buffer, 0, 2048);
                if (actual<0) break;
                sb.append(buffer, 0, actual);
            }
            return sb.condense();
        } catch (java.io.UnsupportedEncodingException encErr) {
            DynamicError e =  new DynamicError("Unknown encoding " + Err.wrap(encoding), encErr);
            e.setErrorCode("XT1190");
            throw e;
        } catch (java.io.IOException ioErr) {
            DynamicError e =  new DynamicError("Failed to read input file", ioErr);
            e.setErrorCode("XT1170");
            throw e;
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
