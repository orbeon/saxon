package net.sf.saxon.instruct;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.EmptyIterator;
import net.sf.saxon.om.Item;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.IntegerValue;
import net.sf.saxon.value.Cardinality;
import net.sf.saxon.xpath.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.pattern.NoNodeTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.io.PrintStream;


/**
* Implements an imaginary xsl:block instruction which simply evaluates
* its contents. Used for top-level templates, xsl:otherwise, etc.
*/

public class Block extends InstructionWithChildren implements MappingFunction {

    public Block() {
    }

    /**
    * Determine the data type of the items returned by this expression
    * @return the data type
    */

    public final ItemType getItemType() {
        if (children==null || children.length==0) {
            return NoNodeTest.getInstance();
        }
        ItemType t1 = children[0].getItemType();
        for (int i=1; i<children.length; i++) {
            t1 = Type.getCommonSuperType(t1, children[i].getItemType());
            if (t1 instanceof AnyItemType) {
                return t1;  // no point going any further
            }
        }
        return t1;
    }

    /**
     * Determine the cardinality of the expression
     */

    public final int getCardinality() {
        if (children==null || children.length==0) {
            return StaticProperty.EMPTY;
        }
        int c1 = children[0].getCardinality();
        for (int i=1; i<children.length; i++) {
            c1 = Cardinality.add(c1, children[i].getCardinality());
        }
        return c1;
    }
    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression). The default implementation does nothing.
     *
     * @exception XPathException if an error is discovered during expression
     *     rewriting
     * @return the simplified expression
     */

    public Expression simplify(StaticContext env) throws XPathException {
        Expression exp = super.simplify(env);
        if (exp != this) {
            return exp;
        }
        if (children==null || getChildren().length == 0) {
            return EmptySequence.getInstance();
        }
        if (getChildren().length == 1) {
            return getChildren()[0];
        }
        return this;
    }

    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     * @param offer The type of rewrite being offered
     * @throws XPathException
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        if (children != null) {
            for (int c=0; c<children.length; c++) {
                children[c] = children[c].promote(offer);
            }
        }
    }

    /**
     * Diagnostic print of expression structure. The expression is written to the System.err
     * output stream
     *
     * @param level indentation level for this expression
     * @param out
     */

    public void display(int level, NamePool pool, PrintStream out) {
        if (children != null) {
            InstructionWithChildren.displayChildren(children, level+1, pool, out);
        }
    }

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        return processChildrenLeavingTail(context);
    }

    /**
     * Iterate over the results of all the child expressions
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        if (children==null || children.length == 0) {
            return EmptyIterator.getInstance();
        } else if (children.length == 1) {
            return children[0].iterate(context);
        } else {
            SequenceIterator base = new RangeExpression.RangeIterator(0, children.length-1);
            return new MappingIterator(base, this, null, context);
        }
    }

   /**
     * Map one item to a sequence.
     * @param item The item to be mapped.
     * If context is supplied, this must be the same as context.currentItem().
     * @param context The processing context. This is supplied only for mapping constructs that
     * set the context node, position, and size. Otherwise it is null.
     * @param info Arbitrary information supplied by the creator of the MappingIterator. It must be
     * read-only and immutable for the duration of the iteration.
     * @return either (a) a SequenceIterator over the sequence of items that the supplied input
     * item maps to, or (b) an Item if it maps to a single item, or (c) null if it maps to an empty
     * sequence.
     */

    public Object map(Item item, XPathContext context, Object info) throws XPathException {
        int i = (int)((IntegerValue)item).longValue();
        return children[i].iterate((XPathContext)info);
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
