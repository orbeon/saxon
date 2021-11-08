package org.orbeon.saxon.pattern;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.DocumentInfo;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.SequenceType;
import org.orbeon.saxon.instruct.SlotManager;

import java.util.StringTokenizer;
import java.util.Iterator;

/**
* An IDPattern is a pattern of the form id("literal") or id($variable)
*/

public final class IDPattern extends Pattern {

    private Expression idExpression;

    /**
     * Create an id pattern.
     * @param id Either a StringValue or a VariableReference
     */
    public IDPattern(Expression id) {
        idExpression = id;
    }

    /**
    * Type-check the pattern.
    * Default implementation does nothing. This is only needed for patterns that contain
    * variable references or function calls.
    * @return the optimised Pattern
    */

    public Pattern analyze(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        idExpression = visitor.typeCheck(idExpression, contextItemType);
        RoleLocator role = new RoleLocator(RoleLocator.FUNCTION, "id", 1);
        idExpression = TypeChecker.staticTypeCheck(idExpression, SequenceType.ATOMIC_SEQUENCE, false, role, visitor);
        return this;
    }

    /**
     * Get the dependencies of the pattern. The only possible dependency for a pattern is
     * on local variables. This is analyzed in those patterns where local variables may appear.
     */

    public int getDependencies() {
        return idExpression.getDependencies();
    }

    /**
     * Iterate over the subexpressions within this pattern
     */

    public Iterator iterateSubExpressions() {
        return new MonoIterator(idExpression);
    }

    /**
     * Offer promotion for subexpressions within this pattern. The offer will be accepted if the subexpression
     * is not dependent on the factors (e.g. the context item) identified in the PromotionOffer.
     * By default the offer is not accepted - this is appropriate in the case of simple expressions
     * such as constant values and variable references where promotion would give no performance
     * advantage. This method is always called at compile time.
     * <p/>
     * <p>Unlike the corresponding method on {@link org.orbeon.saxon.expr.Expression}, this method does not return anything:
     * it can make internal changes to the pattern, but cannot return a different pattern. Only certain
     * kinds of promotion are applicable within a pattern: specifically, promotions affecting local
     * variable references within the pattern.
     *
     * @param offer details of the offer, for example the offer to move
     *              expressions that don't depend on the context to an outer level in
     *              the containing expression
     * @throws org.orbeon.saxon.trans.XPathException
     *          if any error is detected
     */

    public void promote(PromotionOffer offer) throws XPathException {
        idExpression = idExpression.promote(offer);
    }

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        if (idExpression == original) {
            idExpression = replacement;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Allocate slots to any variables used within the pattern
     * @param env         the static context in the XSLT stylesheet
     * @param slotManager the slot manager representing the stack frame for local variables
     * @param nextFree    the next slot that is free to be allocated @return the next slot that is free to be allocated
     */

     public int allocateSlots(StaticContext env, SlotManager slotManager, int nextFree) {
        return ExpressionTool.allocateSlots(idExpression, nextFree, slotManager);
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
            StringTokenizer tokenizer = new StringTokenizer(ids, " \t\n\r", false);
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

    /**
     * Determine whether this pattern is the same as another pattern
     * @param other the other object
     */

    public boolean equals(Object other) {
        return (other instanceof IDPattern) &&
                ((IDPattern)other).idExpression.equals(idExpression);
    }

    /**
     * Hashcode supporting equals()
     */

    public int hashCode() {
        return 0x73108728 ^ idExpression.hashCode();
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
