package org.orbeon.saxon.instruct;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.event.*;
import org.orbeon.saxon.expr.ContextItemExpression;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.ExpressionVisitor;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.NodeKindTest;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.value.Whitespace;

/**
* Handler for xsl:copy elements in stylesheet.
*/

public class Copy extends ElementCreator {

    private boolean copyNamespaces;
    private ItemType contextItemType; // the type of the result

    /**
     * Create a shallow copy instruction
     * @param copyNamespaces true if namespace nodes are to be copied when copying an element
     * @param inheritNamespaces true if child elements are to inherit the namespace nodes of their parent
     * @param schemaType the Schema type against which the content is to be validated
     * @param validation the schema validation mode
     */

    public Copy(boolean copyNamespaces,
                boolean inheritNamespaces,
                SchemaType schemaType,
                int validation) {
        this.copyNamespaces = copyNamespaces;
        this.inheritNamespaces = inheritNamespaces;
        setSchemaType(schemaType);
        this.validation = validation;
        preservingTypes = schemaType == null && validation == Validation.PRESERVE;
        if (copyNamespaces) {
            setLazyConstruction(false);
            // can't do lazy construction at present in cases where namespaces need to be copied from the
            // source document.
        }
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression). The default implementation does nothing.
     *
     * @return the simplified expression
     * @throws org.orbeon.saxon.trans.XPathException
     *          if an error is discovered during expression rewriting
     * @param visitor an expression visitor
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        preservingTypes |= !visitor.getConfiguration().isSchemaAware(Configuration.XML_SCHEMA);
        return super.simplify(visitor);
    }


    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        if (contextItemType == null) {
            // See spec bug 7624, test case copy903err
            XPathException err = new XPathException("Context item for xsl:copy is undefined", "XTTE0945");
            err.setLocator(this);
            throw err;
        }
        if (contextItemType instanceof NodeTest) {
            switch (contextItemType.getPrimitiveType()) {
                // For elements and attributes, assume the type annotation will change
                case Type.ELEMENT:
                    this.contextItemType = NodeKindTest.ELEMENT;
                    break;
                case Type.ATTRIBUTE:
                    this.contextItemType = NodeKindTest.ATTRIBUTE;
                    break;
                case Type.DOCUMENT:
                    this.contextItemType = NodeKindTest.DOCUMENT;
                    break;
                default:
                    this.contextItemType = contextItemType;
            }
        } else {
            this.contextItemType = contextItemType;
        }
        return super.typeCheck(visitor, contextItemType);
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        Copy copy = new Copy(copyNamespaces, inheritNamespaces, getSchemaType(), validation);
        copy.setContentExpression(content.copy());
        return copy;
    }

    /**
    * Get the name of this instruction for diagnostic and tracing purposes
    */

    public int getInstructionNameCode() {
        return StandardNames.XSL_COPY;
    }

    /**
     * Get the item type of the result of this instruction.
     * @return The context item type.
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        if (contextItemType == null) {
            return AnyItemType.getInstance();
        } else {
            return contextItemType;
        }
    }


    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Expression exp = super.optimize(visitor, contextItemType);
        if (exp == this) {
            if (contextItemType.isAtomicType()) {
                return new ContextItemExpression();
            }
        }
        return exp;
    }

    /**
     * Callback from ElementCreator when constructing an element
     * @param context XPath dynamic evaluation context
     * @return the namecode of the element to be constructed
     * @throws XPathException
     */

    public int getNameCode(XPathContext context)
            throws XPathException {
        return ((NodeInfo)context.getContextItem()).getNameCode();
    }

    /**
     * Get the base URI of a copied element node (the base URI is retained in the new copy)
     * @param context XPath dynamic evaluation context
     * @return the base URI
     */

    public String getNewBaseURI(XPathContext context) {
        return ((NodeInfo)context.getContextItem()).getBaseURI();
    }

    /**
     * Callback to output namespace nodes for the new element.
     * @param context The execution context
     * @param receiver the Receiver where the namespace nodes are to be written
     * @throws XPathException
     */

    protected void outputNamespaceNodes(XPathContext context, Receiver receiver)
    throws XPathException {
        if (copyNamespaces) {
            NodeInfo element = (NodeInfo)context.getContextItem();
            NamespaceCodeIterator.sendNamespaces(element, receiver);
        }
    }

    /**
     * Callback to get a list of the intrinsic namespaces that need to be generated for the element.
     * The result is an array of namespace codes, the codes either occupy the whole array or are
     * terminated by a -1 entry. A result of null is equivalent to a zero-length array.
     */

    public int[] getActiveNamespaces() throws XPathException {
        if (copyNamespaces) {
            // we should have disabled lazy construction, so this shouldn't be called.
            throw new UnsupportedOperationException();
        } else {
            return null;
        }
    }

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        Controller controller = context.getController();
        XPathContext c2 = context;
//        XPathContext c2 = context.newMinorContext();
//        c2.setOrigin(this);
        SequenceReceiver out = c2.getReceiver();
        Item item = context.getContextItem();
        if (item == null) {
            // See spec bug 7624, test case copy903err
            XPathException err = new XPathException("Context item for xsl:copy is undefined", "XTTE0945");
            err.setLocator(this);
            throw err;
        }
        if (!(item instanceof NodeInfo)) {
            out.append(item, locationId, NodeInfo.ALL_NAMESPACES);
            return null;
        }
        NodeInfo source = (NodeInfo)item;
        //out.getPipelineConfiguration().setBaseURI(source.getBaseURI());

        // Processing depends on the node kind.

        switch(source.getNodeKind()) {

        case Type.ELEMENT:
            // use the generic code for creating new elements
            return super.processLeavingTail(c2);

        case Type.ATTRIBUTE:
            try {
                CopyOf.copyAttribute(source, getSchemaType(), validation, this, c2, false);
            } catch (NoOpenStartTagException err) {
                err.setXPathContext(context);
                throw dynamicError(this, err, c2);
            }
            break;

        case Type.TEXT:
            out.characters(source.getStringValueCS(), locationId, 0);
            break;

        case Type.PROCESSING_INSTRUCTION:
            out.processingInstruction(source.getDisplayName(), source.getStringValueCS(), locationId, 0);
            break;

        case Type.COMMENT:
            out.comment(source.getStringValueCS(), locationId, 0);
            break;

        case Type.NAMESPACE:
            try {
                source.copy(out, NodeInfo.NO_NAMESPACES, false, locationId);
            } catch (NoOpenStartTagException err) {
                XPathException e = new XPathException(err.getMessage());
                e.setXPathContext(context);
                e.setErrorCode(err.getErrorCodeLocalPart());
                throw dynamicError(this, e, context);
            }
            break;

        case Type.DOCUMENT:
            if (!preservingTypes) {
                Receiver val = controller.getConfiguration().
                        getDocumentValidator(out, source.getBaseURI(),
                                validation, Whitespace.NONE, getSchemaType(), -1);
                if (val != out) {
                    SequenceReceiver sr = new TreeReceiver(val);
                    sr.setPipelineConfiguration(out.getPipelineConfiguration());
                    c2.setReceiver(sr);
                    out = sr;
                }
            }
            out.startDocument(0);
            content.process(c2);
            out.endDocument();
            break;

        default:
            throw new IllegalArgumentException("Unknown node kind " + source.getNodeKind());

        }
        return null;
    }

    /**
     * Evaluate as an expression. We rely on the fact that when these instructions
     * are generated by XQuery, there will always be a valueExpression to evaluate
     * the content
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Controller controller = context.getController();
        XPathContext c2 = context.newMinorContext();
        c2.setOrigin(this);
        SequenceOutputter seq = controller.allocateSequenceOutputter(1);
        PipelineConfiguration pipe = controller.makePipelineConfiguration();
        pipe.setHostLanguage(getHostLanguage());
        seq.setPipelineConfiguration(pipe);
        c2.setTemporaryReceiver(seq);
        process(c2);
        seq.close();
        Item item = seq.getFirstItem();
        seq.reset();
        return item;
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("copy");
        content.explain(out);
        out.endElement();
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
