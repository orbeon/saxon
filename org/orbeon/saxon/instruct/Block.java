package org.orbeon.saxon.instruct;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.EmptyIterator;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.pattern.NoNodeTest;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.value.Cardinality;
import org.orbeon.saxon.value.EmptySequence;
import org.orbeon.saxon.value.SequenceExtent;

import java.io.PrintStream;
import java.util.*;


/**
* Implements an imaginary xsl:block instruction which simply evaluates
* its contents. Used for top-level templates, xsl:otherwise, etc.
*/

public class Block extends Instruction {

    // TODO: allow the last expression in a Block to be a tail-call of a function, at least in push mode

    private Expression[] children;

    public Block() {
    }

    public static Expression makeBlock(Expression e1, Expression e2) {
        if (e1 == null || e1 instanceof EmptySequence) {
            return e2;
        }
        if (e2 == null || e2 instanceof EmptySequence) {
            return e1;
        }
        if (e1 instanceof Block || e2 instanceof Block) {
            Iterator it1 = (e1 instanceof Block ? e1.iterateSubExpressions() : new MonoIterator(e1));
            Iterator it2 = (e2 instanceof Block ? e2.iterateSubExpressions() : new MonoIterator(e2));
            List list = new ArrayList(10);
            while (it1.hasNext()) {
                list.add(it1.next());
            }
            while (it2.hasNext()) {
                list.add(it2.next());
            }
            Expression[] exps = new Expression[list.size()];
            exps = (Expression[])list.toArray(exps);
            Block b = new Block();
            b.setChildren(exps);
            return b;
        } else {
            Expression[] exps = {e1, e2};
            Block b = new Block();
            b.setChildren(exps);
            return b;
        }
    }

    /**
    * Set the children of this instruction
    * @param children The instructions that are children of this instruction
    */

    public void setChildren(Expression[] children) {
        if (children==null || children.length==0) {
            this.children = null;
        } else {
            this.children = children;
            for (int c=0; c<children.length; c++) {
                adoptChildExpression(children[c]);
            }
        }
    }

    /**
    * Get the children of this instruction
    * @return the children of this instruction, as an array of Instruction objects. May return either
     * a zero-length array or null if there are no children
    */

    public Expression[] getChildren() {
        return children;
    }

    public Iterator iterateSubExpressions() {
        if (children == null) {
            return Collections.EMPTY_LIST.iterator();
        } else {
            return Arrays.asList(children).iterator();
        }
    }

    /**
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        for (int c=0; c<children.length; c++) {
            if (children[c] == original) {
                children[c] = replacement;
                found = true;
            };
        }
                return found;
    }


    /**
    * Determine the data type of the items returned by this expression
    * @return the data type
     * @param th
     */

    public final ItemType getItemType(TypeHierarchy th) {
        if (children==null || children.length==0) {
            return NoNodeTest.getInstance();
        }
        ItemType t1 = children[0].getItemType(th);
        for (int i=1; i<children.length; i++) {
            t1 = Type.getCommonSuperType(t1, children[i].getItemType(th), th);
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
            if (c1 == StaticProperty.ALLOWS_ZERO_OR_MORE) {
                break;
            }
        }
        return c1;
    }

    /**
     * Determine whether this instruction creates new nodes.
     * This implementation returns true if any child instruction
     * returns true.
     */

    public final boolean createsNewNodes() {
        if (children==null) {
            return false;
        };
        for (int i=0; i<children.length; i++) {
            int props = children[i].getSpecialProperties();
            if ((props & StaticProperty.NON_CREATIVE) == 0) {
                return true;
            }
        }
        return false;
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
        boolean allAtomic = true;
        boolean nested = false;
        if (children != null) {
            for (int c=0; c<children.length; c++) {
                children[c] = children[c].simplify(env);
                if (!(children[c] instanceof Item)) {
                    allAtomic = false;
                }
                if (children[c] instanceof Block) {
                    nested = true;
                } else if (children[c] instanceof EmptySequence) {
                    nested = true;
                }
            }
            if (children.length == 1) {
                return getChildren()[0];
            }
            if (children.length == 0) {
                return EmptySequence.getInstance();
            }
            if (nested) {
                List list = new ArrayList(children.length*2);
                flatten(list);
                children = new Expression[list.size()];
                for (int i=0; i<children.length; i++) {
                    children[i] = (Expression)list.get(i);
                    adoptChildExpression(children[i]);
                }
            }
            if (allAtomic) {
                Item[] values = new Item[children.length];
                for (int c=0; c<children.length; c++) {
                    values[c] = (Item)children[c];
                }
                return new SequenceExtent(values);
            }
        } else {
            return EmptySequence.getInstance();
        }

        return this;
    }

    private void flatten(List list) {
        for (int i=0; i<children.length; i++) {
            if (children[i] instanceof Block) {
                ((Block)children[i]).flatten(list);
            } else if (children[i] instanceof EmptySequence) {
                // no-op
            } else {
                list.add(children[i]);
            }
        }
    }

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        boolean nested = false;
        if (children != null) {
            for (int c=0; c<children.length; c++) {
                children[c] = children[c].typeCheck(env, contextItemType);
                adoptChildExpression(children[c]);
                if (children[c] instanceof Block) {
                    nested = true;
                } else if (children[c] instanceof EmptySequence) {
                    nested = true;
                }
            }
        }
        if (nested) {
            List list = new ArrayList(children.length*2);
            flatten(list);
            children = new Expression[list.size()];
            for (int i=0; i<children.length; i++) {
                children[i] = (Expression)list.get(i);
                adoptChildExpression(children[i]);
            }
        }
        if (children.length == 0) {
            return EmptySequence.getInstance();
        } else if (children.length == 1) {
            ComputedExpression.setParentExpression(children[0], getParentExpression());
            return children[0];
        } else {
            return this;
        }
    }

    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        if (children != null) {
            for (int c=0; c<children.length; c++) {
                children[c] = children[c].optimize(opt, env, contextItemType);
                adoptChildExpression(children[c]);
            }
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
                children[c] = doPromotion(children[c], offer);
                if (offer.accepted) {
                    break;  // can only handle one promotion at a time
                }
            }
        }
    }

    /**
     * Check that any elements and attributes constructed or returned by this expression are acceptable
     * in the content model of a given complex type. It's always OK to say yes, since the check will be
     * repeated at run-time. The process of checking element and attribute constructors against the content
     * model of a complex type also registers the type of content expected of those constructors, so the
     * static validation can continue recursively.
     */

    public void checkPermittedContents(SchemaType parentType, StaticContext env, boolean whole) throws XPathException {
        if (children != null) {
            for (int c=0; c<children.length; c++) {
                children[c].checkPermittedContents(parentType, env, false);
            }
        }
    }

    /**
     * Diagnostic print of expression structure. The expression is written to the System.err
     * output stream
     *
     * @param level indentation level for this expression
     * @param out
     * @param config
     */

    public void display(int level, PrintStream out, Configuration config) {
        if (children != null) {
            displayChildren(children, level+1, config, out);
        }
    }

    /**
     * Display the children of an instruction for diagostics
     */

    public static void displayChildren(Expression[] children, int level, Configuration config, PrintStream out) {
        if (children != null) {
            for (int c=0; c<children.length; c++) {
                children[c].display(level+1, out, config);
            }
        }
    }


    public TailCall processLeavingTail(XPathContext context) throws XPathException {

        if (children==null) {
            return null;
        }

        TailCall tc = null;
        for (int i=0; i<children.length; i++) {
            try {
                if (children[i] instanceof TailCallReturner) {
                    tc = ((TailCallReturner)children[i]).processLeavingTail(context);
                } else {
                    children[i].process(context);
                    tc = null;
                }
            } catch (DynamicError e) {
                if (e.getXPathContext() == null) {
                    e.setXPathContext(context);
                }
                if (e.getLocator()==null) {
                    e.setLocator(ExpressionTool.getLocator(children[i]));
                }
                throw e;
            }
        }
    	return tc;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided. This implementation provides both iterate() and
     * process() methods natively.
     */

    public int getImplementationMethod() {
        return ITERATE_METHOD | PROCESS_METHOD;
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
            return new BlockIterator(context);
        }
    }

    /**
     * Iterate over the instructions in the Block, concatenating the result of each instruction
     * into a single combined sequence.
     */

    private class BlockIterator implements SequenceIterator {

        private int i = 0;
        private SequenceIterator child;
        private XPathContext context;
        private Item current;
        private int position = 0;

        public BlockIterator(XPathContext context) {
            this.context = context;
        }

        /**
         * Get the next item in the sequence. <BR>
         *
         * @return the next item, or null if there are no more items.
         * @throws org.orbeon.saxon.trans.XPathException
         *          if an error occurs retrieving the next item
         */

        public Item next() throws XPathException {
            if (position < 0) {
                return null;
            }
            while (true) {
                if (child == null) {
                    child = children[i++].iterate(context);
                }
                current = child.next();
                if (current != null) {
                    position++;
                    return current;
                }
                child = null;
                if (i >= children.length) {
                    current = null;
                    position = -1;
                    return null;
                }
            }
        }

        /**
         * Get the current value in the sequence (the one returned by the
         * most recent call on next()). This will be null before the first
         * call of next().
         *
         * @return the current item, the one most recently returned by a call on
         *         next(); or null, if next() has not been called, or if the end
         *         of the sequence has been reached.
         */

        public Item current() {
            return current;
        }

        /**
         * Get the current position. This will be zero before the first call
         * on next(), otherwise it will be the number of times that next() has
         * been called.
         *
         * @return the current position, the position of the item returned by the
         *         most recent call of next()
         */

        public int position() {
            return position;
        }

        /**
         * Get another SequenceIterator that iterates over the same items as the original,
         * but which is repositioned at the start of the sequence.
         *
         * @return a SequenceIterator that iterates over the same items,
         *         positioned before the first item
         * @throws org.orbeon.saxon.trans.XPathException
         *          if any error occurs
         */

        public SequenceIterator getAnother() throws XPathException {
            return new BlockIterator(context);
        }

        /**
         * Get properties of this iterator, as a bit-significant integer.
         *
         * @return the properties of this iterator. This will be some combination of
         *         properties such as {@link GROUNDED}, {@link LAST_POSITION_FINDER},
         *         and {@link LOOKAHEAD}. It is always
         *         acceptable to return the value zero, indicating that there are no known special properties.
         *         It is acceptable for the properties of the iterator to change depending on its state.
         */

        public int getProperties() {
            return 0;
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
