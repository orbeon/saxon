package net.sf.saxon.instruct;
import net.sf.saxon.event.ReceiverOptions;
import net.sf.saxon.event.SequenceReceiver;
import net.sf.saxon.expr.PromotionOffer;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.xpath.XPathException;
import net.sf.saxon.style.StandardNames;

/**
* Handler for xsl:text elements (and simple text nodes) in the stylesheet. <BR>
*/

public class Text extends SimpleNodeConstructor {

    private int options;

    /**
    * Create an xsl:text instruction
    * @param disable set to true if output escaping is to be disabled
    */

    public Text(boolean disable) {
        this.options = (disable ? ReceiverOptions.DISABLE_ESCAPING : 0);
    }

    /**
    * Get the name of this instruction for diagnostic and tracing purposes
    */

    public int getInstructionNameCode() {
        return StandardNames.XSL_TEXT;
    }

    public ItemType getItemType() {
        return NodeKindTest.TEXT;
    }

    public int getCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
    * Process this instruction, that is, produce a processing-instruction node in the
    * result sequence.
    * @param context the dynamic context of this transformation
    * @throws XPathException if any non-recoverable dynamic error occurs
    * @return always returns null in this implementation
    */

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        CharSequence value = expandChildren(context);
        SequenceReceiver out = context.getReceiver();
        out.characters(value, locationId, options);
        return null;
    }

    protected void promoteInst(PromotionOffer offer) {
        throw new UnsupportedOperationException("Text instruction cannot be used as an expression");
    }

    public void typeCheck(StaticContext env, ItemType contextItemType) {}


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
