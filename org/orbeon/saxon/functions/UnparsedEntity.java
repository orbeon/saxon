package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.DocumentInfo;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.value.StringValue;

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
     * Type-check the expression. This also calls preEvaluate() to evaluate the function
     * if all the arguments are constant; functions that do not require this behavior
     * can override the preEvaluate method.
     */

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        try {
            return super.typeCheck(env, contextItemType);
        } catch (XPathException err) {
            if ("XPDY0002".equals(err.getErrorCodeLocalPart())) {
                if (operation == URI) {
                    DynamicError e = new DynamicError("Cannot call the unparsed-entity-uri()" +
                            " function when there is no context node");
                    e.setErrorCode("XTDE1370");
                    throw e;
                } else {
                    DynamicError e = new DynamicError("Cannot call the unparsed-entity-public-id()" +
                            " function when there is no context node");
                    e.setErrorCode("XTDE1380");
                    throw e;
                }
            }
            throw err;
        }
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
        NodeInfo doc = null;
        try {
            doc = (NodeInfo)argument[1].evaluateItem(context);
        } catch (XPathException err) {
            if ("XPDY0002".equals(err.getErrorCodeLocalPart())) {
                if (operation == URI) {
                    DynamicError e = new DynamicError("Cannot call the unparsed-entity-uri()" +
                            " function when there is no context node");
                    e.setErrorCode("XTDE1370");
                    throw e;
                } else {
                    DynamicError e = new DynamicError("Cannot call the unparsed-entity-public-id()" +
                            " function when there is no context node");
                    e.setErrorCode("XTDE1380");
                    throw e;
                }
            } else if ("XPDY0050".equals(err.getErrorCodeLocalPart())) {
                if (operation == URI) {
                    DynamicError e = new DynamicError("Can only call the unparsed-entity-uri()" +
                            " function when the context node is in a tree rooted at a document node");
                    e.setErrorCode("XTDE1370");
                    throw e;
                } else {
                    DynamicError e = new DynamicError("Can only call the unparsed-entity-public-id()" +
                            " function when the context node is in a tree rooted at a document node");
                    e.setErrorCode("XTDE1380");
                    throw e;
                }
            }
        }
        if (doc.getNodeKind() != Type.DOCUMENT) {
            String code = (operation==URI ? "XTDE1370" : "XTDE1380");
            dynamicError("In function " + getDisplayName(context.getNamePool()) +
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
