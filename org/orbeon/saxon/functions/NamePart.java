package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.QNameValue;
import org.orbeon.saxon.value.StringValue;
import org.orbeon.saxon.value.AnyURIValue;
import org.orbeon.saxon.type.Type;

/**
* This class supports the name(), local-name(), and namespace-uri() functions
* from XPath 1.0, and also the XSLT generate-id() function
*/

public class NamePart extends SystemFunction {

    public static final int NAME = 0;
    public static final int LOCAL_NAME = 1;
    public static final int NAMESPACE_URI = 2;
    public static final int GENERATE_ID = 3;
    public static final int DOCUMENT_URI = 4;
    public static final int NODE_NAME = 6;

    /**
    * Simplify and validate.
    */

     public Expression simplify(StaticContext env) throws XPathException {
        useContextItemAsDefault();
        return simplifyArguments(env);
    }

    /**
     * Determine the special properties of this expression. The generate-id()
     * function is a special case: it is considered creative if its operand
     * is creative, so that generate-id(f()) is not taken out of a loop
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        if (operation == GENERATE_ID) {
            return p & ~StaticProperty.NON_CREATIVE;
        } else {
            return p;
        }
    }

    /**
    * Evaluate the function in a string context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        NodeInfo node = (NodeInfo)argument[0].evaluateItem(c);
        if (node==null) {
            // Effect of supplying an empty sequence as the argument differs depending on the function
            if (operation == NODE_NAME || operation == DOCUMENT_URI ) {
                return null;
            } else if (operation == NAMESPACE_URI) {
                return AnyURIValue.EMPTY_URI;
            } else {
                return StringValue.EMPTY_STRING;
            }
        }

        String s;
        switch (operation) {
            case NAME:
                s = node.getDisplayName();
                break;
            case LOCAL_NAME:
                s = node.getLocalPart();
                break;
            case NAMESPACE_URI:
                String uri = node.getURI();
                s = (uri==null ? "" : uri);
                        // null should no longer be returned, but the spec has changed, so it's
                        // better to be defensive
                return new AnyURIValue(s);

            case GENERATE_ID:
                FastStringBuffer buffer = new FastStringBuffer(16);
                node.generateId(buffer);
                return new StringValue(buffer);

            case DOCUMENT_URI:
                // If the node is in the document pool, get the URI under which it is registered.
                // Otherwise, return its systemId. 
                if (node.getNodeKind() == Type.DOCUMENT) {
                    DocumentPool pool = c.getController().getDocumentPool();
                    String docURI = pool.getDocumentURI(node);
                    if (docURI == null) {
                        docURI = node.getSystemId();
                    }
                    if (docURI == null) {
                        return null;
                    } else if ("".equals(docURI)) {
                        return null;
                    } else {
                        return StringValue.makeStringValue(docURI);
                    }
                } else {
                    return null;
                }
            case NODE_NAME:
                int nc = node.getNameCode();
                if (nc == -1) {
                    return null;
                }
                return new QNameValue(node.getNamePool(), nc);
            default:
                throw new UnsupportedOperationException("Unknown name operation");
        }
        return new StringValue(s);
    }

    /**
     * Test whether an expression is a call on the generate-id() function
     * @param exp the expression to be tested
     * @return true if exp is a call on generate-id(), else false
     */

    public static boolean isGenerateIdFunction(Expression exp) {
        return ((exp instanceof NamePart) && ((NamePart)exp).operation == GENERATE_ID);
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
