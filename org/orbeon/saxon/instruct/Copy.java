package org.orbeon.saxon.instruct;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.event.*;
import org.orbeon.saxon.expr.ExpressionTool;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.Validation;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.value.Whitespace;

import java.io.PrintStream;

/**
* Handler for xsl:copy elements in stylesheet.
*/

public class Copy extends ElementCreator {

    private boolean copyNamespaces;
    private ItemType contextItemType;    // TODO set this while type-checking

    public Copy(boolean copyNamespaces,
                boolean inheritNamespaces,
                SchemaType schemaType,
                int validation) {
        this.copyNamespaces = copyNamespaces;
        this.inheritNamespaces = inheritNamespaces;
        setSchemaType(schemaType);
        this.validation = validation;
        this.validating = schemaType != null || validation != Validation.PRESERVE;
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
     */

    public Expression simplify(StaticContext env) throws XPathException {
        validating &= env.getConfiguration().isSchemaAware(Configuration.XML_SCHEMA);
        return super.simplify(env);
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
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        if (contextItemType == null) {
            return AnyItemType.getInstance();
        } else {
            return contextItemType;
        }
    }

    /**
     * Callback from ElementCreator when constructing an element
     * @param context
     * @return the namecode of the element to be constructed
     * @throws XPathException
     */

    public int getNameCode(XPathContext context)
            throws XPathException {
        return ((NodeInfo)context.getContextItem()).getNameCode();
    }

    /**
     * Get the base URI of a copied element node (the base URI is retained in the new copy)
     * @param context
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
            element.sendNamespaceDeclarations(receiver, true);
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
        XPathContext c2 = context.newMinorContext();
        c2.setOrigin(this);
        SequenceReceiver out = c2.getReceiver();
        Item item = context.getContextItem();
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
                DynamicError e = new DynamicError(err.getMessage());
                e.setXPathContext(context);
                e.setErrorCode(err.getErrorCodeLocalPart());
                throw dynamicError(this, e, context);
            }
            break;

        case Type.DOCUMENT:
            if (validating) {
                Receiver val = controller.getConfiguration().
                        getDocumentValidator(out,
                                             source.getBaseURI(),
                                validation, Whitespace.NONE, getSchemaType());
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
        final PipelineConfiguration pipe = controller.makePipelineConfiguration();
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
     * Diagnostic print of expression structure. The expression is written to the System.err
     * output stream
     *
     * @param level indentation level for this expression
     @param out
     @param config
     */

    public void display(int level, PrintStream out, Configuration config) {
        out.println(ExpressionTool.indent(level) + "copy");
        content.display(level+1, out, config);
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
