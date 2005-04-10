package net.sf.saxon.instruct;
import net.sf.saxon.Controller;
import net.sf.saxon.event.*;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.pull.UnconstructedElement;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.ValidationException;


/**
 * An instruction that creates an element node. There are two subtypes, FixedElement
 * for use where the name is known statically, and Element where it is computed
 * dynamically. To allow use in both XSLT and XQuery, the class acts both as an
 * Instruction and as an Expression.
*/

public abstract class ElementCreator extends ParentNodeConstructor {

    //protected SchemaType schemaType;
    //protected int validation;

    /**
     * The inheritNamespaces flag indicates that the namespace nodes on the element created by this instruction
     * are to be inherited (copied) on the children of this element. That is, if this flag is false, the child
     * elements must carry a namespace undeclaration for all the namespaces on the parent, unless they are
     * redeclared in some way.
     */

    protected boolean inheritNamespaces = true;

    public ElementCreator() { }

    /**
     * Get the item type of the value returned by this instruction
     * @return the item type
     */

    public ItemType getItemType() {
        return NodeKindTest.ELEMENT;
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

    public abstract int getNameCode(XPathContext context)
    throws XPathException;

    /**
     * Callback to output namespace nodes for the new element.
     * @param context The execution context
     * @param receiver the Receiver where the namespace nodes are to be written
     * @throws net.sf.saxon.trans.XPathException
     */

    protected abstract void outputNamespaceNodes(XPathContext context, Receiver receiver)
    throws XPathException;

    /**
     * Callback to get a list of the intrinsic namespaces that need to be generated for the element.
     * The result is an array of namespace codes, the codes either occupy the whole array or are
     * terminated by a -1 entry. A result of null is equivalent to a zero-length array.
     */

    public int[] getActiveNamespaces() throws XPathException {
        return null;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is prefered. For instructions this is the process() method.
     */

    public int getImplementationMethod() {
        return Expression.PROCESS_METHOD | Expression.EVALUATE_METHOD;
    }

    /**
     * Evaluate the instruction to produce a new element node. This method is typically used when there is
     * a parent element or document in a result tree, to which the new element is added.
     * @param context
     * @return null (this instruction never returns a tail call)
     * @throws XPathException
     */
    public TailCall processLeavingTail(XPathContext context)
    throws XPathException {

        try {

            int nameCode = getNameCode(context);
            if (nameCode == -1) {
                // XSLT 1.0 recovery action when the computed name is invalid
                // TODO: now unrecoverable in XSLT 2.0
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
        // TODO: this code is obsolete, the error is no longer recoverable
        content.process(context);
        // Note, we don't bother with an endElement call
    }


   /**
     * Evaluate the constructor, returning the constructed element node. If lazy construction
     * mode is in effect, then an UnconstructedParent object is returned instead.
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
       //
       if (isLazyConstruction()) {
           UnconstructedElement e = new UnconstructedElement(this, context);
           // The name code is evaluated eagerly. It's usually already known, and it's usually needed.
           // Evaluating it now removes problems with error handling.
           e.setNameCode(getNameCode(context));
           return e;
       } else {
           return constructElement(context);
       }
    }

    /**
     * Construct the element node as a free-standing (parentless) node in a tiny tree
     * @param context
     * @return the constructed element node
     * @throws XPathException
     */
    private NodeInfo constructElement(XPathContext context) throws XPathException {
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

            // output the namespace nodes for the new element
            outputNamespaceNodes(c2, ini);

            content.process(c2);

            ini.endElement();
            ini.close();

            // the constructed element is the first and only item in the sequence
            return (NodeInfo)seq.popLastItem();

        } catch (XPathException err) {
            if (err instanceof ValidationException) {
                ((ValidationException)err).setSourceLocator(this);
                ((ValidationException)err).setSystemId(getSystemId());
            }
            if (err.getLocator() == null) {
                err.setLocator(this);
            }
            if (err instanceof DynamicError && ((DynamicError)err).getXPathContext() == null) {
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
