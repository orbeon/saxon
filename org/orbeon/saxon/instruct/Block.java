package org.orbeon.saxon.instruct;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.EmptyIterator;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.pattern.NoNodeTest;
import org.orbeon.saxon.type.AnyItemType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.Cardinality;
import org.orbeon.saxon.value.EmptySequence;
import org.orbeon.saxon.value.IntegerValue;
import org.orbeon.saxon.xpath.DynamicError;
import org.orbeon.saxon.xpath.XPathException;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;


/**
* Implements an imaginary xsl:block instruction which simply evaluates
* its contents. Used for top-level templates, xsl:otherwise, etc.
*/

public class Block extends Instruction implements MappingFunction {

    private Expression[] children;

    public Block() {
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
        if (children != null) {
            for (int c=0; c<children.length; c++) {
                children[c] = children[c].simplify(env);
            }
            if (children.length == 1) {
                return getChildren()[0];
            }
            if (children.length == 0) {
                return EmptySequence.getInstance();
            }
        } else {
            return EmptySequence.getInstance();
        }

        return this;
    }
    /**
     * Perform static analysis of an expression and its subexpressions.
     *
     * <p>This checks statically that the operands of the expression have
     * the correct type; if necessary it generates code to do run-time type checking or type
     * conversion. A static type error is reported only if execution cannot possibly succeed, that
     * is, if a run-time type error is inevitable. The call may return a modified form of the expression.</p>
     *
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable. However, the types of such functions and
     * variables will only be accurately known if they have been explicitly declared.</p>
     *
     * @param env the static context of the expression
     * @exception XPathException if an error is discovered during this phase
     *     (typically a type error)
     * @return the original expression, rewritten to perform necessary
     *     run-time type checks, and to perform other type-related
     *     optimizations
     */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        if (children != null) {
            for (int c=0; c<children.length; c++) {
                children[c] = children[c].analyze(env, contextItemType);
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
                children[c] = children[c].promote(offer);
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
     */

    public void display(int level, NamePool pool, PrintStream out) {
        if (children != null) {
            displayChildren(children, level+1, pool, out);
        }
    }

    /**
     * Display the children of an instruction for diagostics
     */

    public static void displayChildren(Expression[] children, int level, NamePool pool, PrintStream out) {
        if (children != null) {
            for (int c=0; c<children.length; c++) {
                children[c].display(level+1, pool, out);
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
                if (children[i] instanceof Instruction) {
                    tc = ((Instruction)children[i]).processLeavingTail(context);
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
