package net.sf.saxon.pattern;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.xpath.XPathException;

import java.util.StringTokenizer;

/**
* An IDPattern is a pattern of the form id(literal)
*/

public final class IDPattern extends Pattern {

    private Expression idExpression;

    public IDPattern(Expression id) {
        idExpression = id;
    }

    /**
    * Type-check the pattern.
    * Default implementation does nothing. This is only needed for patterns that contain
    * variable references or function calls.
    * @return the optimised Pattern
    */

    public Pattern typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        idExpression = idExpression.analyze(env, contextItemType);
        return this;
    }

    /**
    * Determine whether this Pattern matches the given Node
    * @param e The NodeInfo representing the Element or other node to be tested against the Pattern
    * @return true if the node matches the Pattern, false otherwise
    */

    public boolean matches(NodeInfo e, XPathContext context) throws XPathException {
        if (e.getNodeKind() != Type.ELEMENT) {
            return false;
        }
        DocumentInfo doc = e.getDocumentRoot();
        if (doc==null) {
            return false;
        }
        AtomicValue idValue = (AtomicValue)idExpression.evaluateItem(context);
        if (idValue == null) {
            return false;
        }
        String ids = idValue.getStringValue();
        if (ids.indexOf(' ') < 0 &&
                ids.indexOf(0x09) < 0 &&
                ids.indexOf(0x0a) < 0 &&
                ids.indexOf(0x0c) < 0) {
            NodeInfo element = doc.selectID(ids);
            if (element==null) return false;
            return (element.isSameNodeInfo(e));
        } else {
            StringTokenizer tokenizer = new StringTokenizer(ids);
            while (tokenizer.hasMoreElements()) {
                String id = (String)tokenizer.nextElement();
                NodeInfo element = doc.selectID(id);
                if (element != null && e.isSameNodeInfo(element)) {
                    return true;
                }
            }
            return false;
        }

    }

    /**
    * Determine the type of nodes to which this pattern applies.
    * @return Type.ELEMENT
    */

    public int getNodeKind() {
        return Type.ELEMENT;
    }

    /**
    * Get a NodeTest that all the nodes matching this pattern must satisfy
    */

    public NodeTest getNodeTest() {
        return NodeKindTest.ELEMENT;
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
