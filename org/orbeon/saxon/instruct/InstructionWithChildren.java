package net.sf.saxon.instruct;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.xpath.DynamicError;
import net.sf.saxon.xpath.XPathException;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.io.PrintStream;

/**
* Abstract superclass for all instructions in the compiled stylesheet that have child instructions.
* This does not include instructions such as xsl:perform-sort that have child instructions, but whose
* child instructions are compiled directly into an expression (or a Block).
*/

public abstract class InstructionWithChildren extends Instruction {

    // The instructions that are children of this instruction
    protected Expression[] children;

   /**
    * Constructor
    */

    public InstructionWithChildren() {}

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

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression).
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
            for (int c = 0; c < children.length; c++) {
                children[c] = children[c].promote(offer);
            }
        }
    }

    /**
      * Get the immediate sub-expressions of this expression.
      * @return an iterator containing the sub-expressions of this expression
      */

   public Iterator iterateSubExpressions() {
        if (children == null) {
            return Collections.EMPTY_LIST.iterator();
        } else {
            return Arrays.asList(children).iterator();
        }
    }

    /**
    * Process the children of this instruction, including any tail calls
    * @param context The dynamic context for the transformation
     * @throws XPathException if a dynamic error occurs
    */

    protected void processChildren(XPathContext context) throws XPathException {
        if (children==null) {
            return;
        }
        for (int i=0; i<children.length; i++) {
            try {
                children[i].process(context);
            } catch (XPathException err) {
                if (err instanceof DynamicError) {
                    if (err.getLocator() == null) {
                        err.setLocator(ExpressionTool.getLocator(children[i]));
                    }
                    if (((DynamicError)err).getXPathContext() == null) {
                        ((DynamicError)err).setXPathContext(context);
                    }
                    throw err;
                } else {
                    // terminate execution
                    throw dynamicError(ExpressionTool.getLocator(children[i]), err, context);
                }
            }
        }
    }

    /**
    * Process the children of this instruction, returning any tail call made by
    * the last child instruction
    * @param context The dynamic context of the transformation, giving access to the current node,
    * the current variables, etc.
    * @return null if the instruction has completed execution; or a TailCall indicating
    * a function call or template call that is delegated to the caller, to be made after the stack has
    * been unwound so as to save stack space.
    */

    protected TailCall processChildrenLeavingTail(XPathContext context) throws XPathException {

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
