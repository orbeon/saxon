package org.orbeon.saxon.instruct;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.event.*;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.pattern.NodeKindTest;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.xpath.XPathException;

import org.orbeon.saxon.xpath.XPathException;
import org.orbeon.saxon.xpath.DynamicError;


/**
 * An instruction that creates an element node. There are two subtypes, FixedElement
 * for use where the name is known statically, and Element where it is computed
 * dynamically. To allow use in both XSLT and XQuery, the class acts both as an
 * Instruction and as an Expression.
*/

public abstract class ElementCreator extends InstructionWithChildren {

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


    public ItemType getItemType() {
        return NodeKindTest.ELEMENT;
    }

    public int getCardinality() {
        return StaticProperty.EXACTLY_ONE;
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
     * @throws XPathException
     */

    protected abstract void outputNamespaceNodes(XPathContext context, Receiver receiver)
    throws XPathException;

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
                out.setConfiguration(controller.getConfiguration());
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
            processChildren(c2);

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
        processChildren(context);
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
            SequenceOutputter seq = new SequenceOutputter();
            seq.setConfiguration(controller.getConfiguration());

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
                tr.setConfiguration(controller.getConfiguration());
                tr.setDocumentLocator(getExecutable().getLocationMap());
                c2.setReceiver(tr);
                ini = tr;
            }


            ini.open();
            ini.startElement(nameCode, -1, locationId, 0);
            // ignore attribute sets for now

            // output the namespace nodes for the new element
            outputNamespaceNodes(c2, ini);

            processChildren(c2);

            ini.endElement();
            ini.close();
            //c2.resetOutputDestination(old);

            // the constructed element is the first and only item in the sequence
            return seq.getFirstItem();

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
