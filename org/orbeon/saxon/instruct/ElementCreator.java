package org.orbeon.saxon.instruct;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.event.*;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.pattern.NodeKindTest;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.type.ValidationException;

import java.util.Iterator;


/**
 * An instruction that creates an element node. There are two subtypes, FixedElement
 * for use where the name is known statically, and Element where it is computed
 * dynamically. To allow use in both XSLT and XQuery, the class acts both as an
 * Instruction and as an Expression.
*/

public abstract class ElementCreator extends Instruction {

    protected Expression content;
    protected AttributeSet[] useAttributeSets;
    protected SchemaType schemaType;
    protected int validation;

    /**
     * The inheritNamespaces flag indicates that the namespace nodes on the element created by this instruction
     * are to be inherited (copied) on the children of this element. That is, if this flag is false, the child
     * elements must carry a namespace undeclaration for all the namespaces on the parent, unless they are
     * redeclared in some way.
     */

    protected boolean inheritNamespaces = true;

    public ElementCreator() { }

    /**
     * Set the expression that constructs the content of the element
     */

    public void setContent(Expression content) {
        this.content = content;
        adoptChildExpression(content);
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression). The default implementation does nothing.
     * @return the simplified expression
     * @throws org.orbeon.saxon.trans.XPathException
     *          if an error is discovered during expression rewriting
     */

    public Expression simplify(StaticContext env) throws XPathException {
        content = content.simplify(env);
        return this;
    }

    /**
     * Perform static analysis of an expression and its subexpressions.
     * <p/>
     * <p>This checks statically that the operands of the expression have
     * the correct type; if necessary it generates code to do run-time type checking or type
     * conversion. A static type error is reported only if execution cannot possibly succeed, that
     * is, if a run-time type error is inevitable. The call may return a modified form of the expression.</p>
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable. However, the types of such functions and
     * variables will only be accurately known if they have been explicitly declared.</p>
     *
     * @param env the static context of the expression
     * @return the original expression, rewritten to perform necessary
     *         run-time type checks, and to perform other type-related
     *         optimizations
     * @throws org.orbeon.saxon.trans.XPathException
     *          if an error is discovered during this phase
     *          (typically a type error)
     */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        content = content.analyze(env, contextItemType);
        return this;
    }

    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     * @param offer The type of rewrite being offered
     * @throws org.orbeon.saxon.trans.XPathException
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        content = content.promote(offer);
    }

    /**
      * Get the immediate sub-expressions of this expression.
      * @return an iterator containing the sub-expressions of this expression
      */

    public Iterator iterateSubExpressions() {
        return new MonoIterator(content);
    }


    /**
     * Determine whether this instruction creates new nodes.
     * This implementation returns true.
     */

    public final boolean createsNewNodes() {
        return true;
    }

    /**
     * Get the item type of the value returned by this instruction
     * @return the item type
     */

    public ItemType getItemType() {
        return NodeKindTest.ELEMENT;
    }

    public int getCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
     * Get the static properties of this expression (other than its type). The result is
     * bit-signficant. These properties are used for optimizations. In general, if
     * property bit is set, it is true, but if it is unset, the value is unknown.
     *
     * @return a set of flags indicating static properties of this expression
     */

    public int computeSpecialProperties() {
        return super.computeSpecialProperties() |
                StaticProperty.SINGLE_DOCUMENT_NODESET;
    }

    /**
     * Set the validation mode for the new element
     */

    public void setValidationMode(int mode) {
        validation = mode;
    }

    /**
     * Get the validation mode for the constructed element
     */

    public int getValidationMode() {
        return validation;
    }

    protected abstract int getNameCode(XPathContext context)
    throws XPathException;

    /**
     * Callback to output namespace nodes for the new element.
     * @param context The execution context
     * @param receiver the Receiver where the namespace nodes are to be written
     * @throws org.orbeon.saxon.trans.XPathException
     */

    protected abstract void outputNamespaceNodes(XPathContext context, Receiver receiver)
    throws XPathException;

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is prefered. For instructions this is the process() method.
     */

    public int getImplementationMethod() {
        return Expression.PROCESS_METHOD | Expression.EVALUATE_METHOD;
    }

    /**
     * Evaluate the instruction to produce a new element node
     * @param context
     * @return null (this instruction never returns a tail call)
     * @throws XPathException
     */
    public TailCall processLeavingTail(XPathContext context)
    throws XPathException {

        try {

            int nameCode = getNameCode(context);
            if (nameCode == -1) {
                // XSLT recovery action when the computed name is invalid
                skipElement(context);
                return null;
            }

            Controller controller = context.getController();
            XPathContext c2 = context;
            SequenceReceiver out = context.getReceiver();

            Receiver validator = controller.getConfiguration().getElementValidator(
                    out, nameCode, locationId,
                    schemaType, validation,
                    controller.getNamePool()
            );

            if (validator != out) {
                c2 = context.newMinorContext();
                c2.setOrigin(this);
                out = new TreeReceiver(validator);
                out.setPipelineConfiguration(controller.makePipelineConfiguration());
                c2.setReceiver(out);
            }
            int properties = (inheritNamespaces ? 0 : ReceiverOptions.DISINHERIT_NAMESPACES);
            out.startElement(nameCode, -1, locationId, properties);

            // output the required namespace nodes via a callback

            outputNamespaceNodes(c2, out);

            // apply the content of any attribute sets mentioned in use-attribute-sets
            if (useAttributeSets != null) {
                AttributeSet.expand(useAttributeSets, c2);
            }

            // process subordinate instructions to generate attributes and content
            content.process(c2);

            // output the element end tag (which will fail if validation fails)
            out.endElement();
            return null;

        } catch (DynamicError e) {
            if (e.getXPathContext() == null) {
                e.setXPathContext(context);
            }
            if (e.getLocator()==null) {
                e.setLocator(this);
            }
            throw e;
        }
    }

    /**
    * Recovery action when the element name is invalid. We need to tell the outputter
    * about this, so that it can ignore attributes in the content
    */

    private void skipElement(XPathContext context) throws XPathException {
        context.getReceiver().startElement(-1, -1, locationId, 0);
        // Sending a namecode of -1 to the Outputter is a special signal to ignore
        // this element and the attributes that follow it
        content.process(context);
        // Note, we don't bother with an endElement call
    }


   /**
     * Evaluate as an expression. We rely on the fact that when these instructions
     * are generated by XQuery, there will always be a valueExpression to evaluate
     * the content
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        try {
            Controller controller = context.getController();
            XPathContext c2 = context.newMinorContext();
            c2.setOrigin(this);
            SequenceReceiver old = c2.getReceiver();
            SequenceOutputter seq;
            if (old instanceof SequenceOutputter && !((SequenceOutputter)old).hasOpenNodes()) {
                // Reuse the current SequenceOutputter if possible. This enables the construction of
                // a single TinyTree to hold a sequence of parentless elements (a forest).
                seq = (SequenceOutputter)old;
            } else {
                seq = new SequenceOutputter(1);
            }
            seq.setPipelineConfiguration(controller.makePipelineConfiguration());

            int nameCode = getNameCode(c2);

            Receiver validator = controller.getConfiguration().getElementValidator(
                    seq, nameCode, locationId,
                    schemaType, validation,
                    controller.getNamePool()
            );

            SequenceReceiver ini = seq;
            if (validator == seq) {
                c2.setTemporaryReceiver(seq);
            } else {
                TreeReceiver tr = new TreeReceiver(validator);
                tr.setPipelineConfiguration(seq.getPipelineConfiguration());
                c2.setReceiver(tr);
                ini = tr;
            }


            ini.open();
            int properties = (inheritNamespaces ? 0 : ReceiverOptions.DISINHERIT_NAMESPACES);
            ini.startElement(nameCode, -1, locationId, properties);
            // ignore attribute sets for now

            // output the namespace nodes for the new element
            outputNamespaceNodes(c2, ini);

            content.process(c2);

            ini.endElement();
            ini.close();

            // the constructed element is the first and only item in the sequence
            return seq.popLastItem();

        } catch (XPathException err) {
            if (err instanceof ValidationException) {
                ((ValidationException)err).setSourceLocator(this);
                ((ValidationException)err).setSystemId(getSystemId());
            }
            if (err.getLocator() == null) {
                err.setLocator(this);
            }
            if (err instanceof DynamicError) {
                ((DynamicError)err).setXPathContext(context);
            }
            throw err;
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
