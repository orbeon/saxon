package net.sf.saxon.pattern;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.PromotionOffer;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.KeyManager;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.AtomicValue;

import java.util.Iterator;

/**
* A KeyPattern is a pattern of the form key(keyname, keyvalue)
*/

public final class KeyPattern extends Pattern {

    private int keyfingerprint;          // the fingerprint of the key name
    private Expression keyexp;           // the value of the key

    /**
    * Constructor
    * @param namecode the name of the key
    * @param key the value of the key: either a StringValue or a VariableReference
    */

    public KeyPattern(int namecode, Expression key) {
        keyfingerprint = namecode & 0xfffff;
        keyexp = key;
    }

    /**
    * Type-check the pattern. This is needed for patterns that contain
    * variable references or function calls.
    * @return the optimised Pattern
    */

    public Pattern analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        keyexp = keyexp.analyze(env, contextItemType);
        return this;
    }

    /**
     * Get the dependencies of the pattern. The only possible dependency for a pattern is
     * on local variables. This is analyzed in those patterns where local variables may appear.
     */

    public int getDependencies() {
        return keyexp.getDependencies();
    }

    /**
     * Iterate over the subexpressions within this pattern
     */

    public Iterator iterateSubExpressions() {
        return keyexp.iterateSubExpressions();
    }

    /**
     * Offer promotion for subexpressions within this pattern. The offer will be accepted if the subexpression
     * is not dependent on the factors (e.g. the context item) identified in the PromotionOffer.
     * By default the offer is not accepted - this is appropriate in the case of simple expressions
     * such as constant values and variable references where promotion would give no performance
     * advantage. This method is always called at compile time.
     * <p/>
     * <p>Unlike the corresponding method on {@link net.sf.saxon.expr.Expression}, this method does not return anything:
     * it can make internal changes to the pattern, but cannot return a different pattern. Only certain
     * kinds of promotion are applicable within a pattern: specifically, promotions affecting local
     * variable references within the pattern.
     *
     * @param offer details of the offer, for example the offer to move
     *              expressions that don't depend on the context to an outer level in
     *              the containing expression
     * @throws net.sf.saxon.trans.XPathException
     *          if any error is detected
     */

    public void promote(PromotionOffer offer) throws XPathException {
        keyexp = keyexp.promote(offer);
    }

   /**
    * Determine whether this Pattern matches the given Node.
    * @param e The NodeInfo representing the Element or other node to be tested against the Pattern
    * @return true if the node matches the Pattern, false otherwise
    */

    public boolean matches(NodeInfo e, XPathContext context) throws XPathException {
        DocumentInfo doc = e.getDocumentRoot();
        if (doc==null) {
            return false;
        }
        KeyManager km = context.getController().getKeyManager();
        SequenceIterator iter = keyexp.iterate(context);
        while (true) {
            Item it = iter.next();
            if (it == null) {
                return false;
            }
            SequenceIterator nodes = km.selectByKey(keyfingerprint, doc, (AtomicValue)it, context);
            while (true) {
                NodeInfo n = (NodeInfo)nodes.next();
                if (n == null) {
                    break;
                }
                if (n.isSameNodeInfo(e)) {
                    return true;
                }
            }
        }
    }

    /**
    * Get a NodeTest that all the nodes matching this pattern must satisfy
    */

    public NodeTest getNodeTest() {
        return AnyNodeTest.getInstance();
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
