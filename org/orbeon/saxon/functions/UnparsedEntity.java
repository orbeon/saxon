package net.sf.saxon.functions;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.StringValue;

/**
* Implements the unparsed-entity-uri() function defined in XSLT 1.0
* and the unparsed-entity-public-id() function defined in XSLT 2.0
*/


public class UnparsedEntity extends SystemFunction implements XSLTFunction {

    public static int URI = 0;
    public static int PUBLIC_ID = 1;

    /**
    * Simplify: add a second implicit argument, the context document
    */

     public Expression simplify(StaticContext env) throws XPathException {
        UnparsedEntity f = (UnparsedEntity)super.simplify(env);
        f.addContextDocumentArgument(1, (operation==URI ? "unparsed-entity-uri_9999_": "unparsed-entity-public-id_9999_"));
        return f;
    }

    /**
    * preEvaluate: this method suppresses compile-time evaluation by doing nothing
    */

    public Expression preEvaluate(StaticContext env) {
        return this;
    }

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        String arg0 = argument[0].evaluateItem(context).getStringValue();
        Item doc = argument[1].evaluateItem(context);
        if (!(doc instanceof DocumentInfo)) {
            String code = (operation==URI ? "XT1370" : "XT1380");
            dynamicError("In function " + getDisplayName(context.getController().getNamePool()) +
                            ", the context node must be in a tree whose root is a document node", code, context);
        }
        String[] ids = ((DocumentInfo)doc).getUnparsedEntity(arg0);
        if (ids==null) return StringValue.EMPTY_STRING;
        return new StringValue(ids[operation]);
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
