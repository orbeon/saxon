package net.sf.saxon.instruct;
import net.sf.saxon.Controller;
import net.sf.saxon.event.*;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.ExpressionTool;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.Type;

import java.io.PrintStream;

/**
* Handler for xsl:copy elements in stylesheet.
*/

public class Copy extends ElementCreator {

    private boolean copyNamespaces;
    private ItemType contextItemType;

    public Copy( AttributeSet[] useAttributeSets,
                 boolean copyNamespaces,
                 boolean inheritNamespaces,
                 SchemaType schemaType,
                 int validation) {
        this.useAttributeSets = useAttributeSets;
        this.copyNamespaces = copyNamespaces;
        this.inheritNamespaces = inheritNamespaces;
        this.schemaType = schemaType;
        this.validation = validation;
    }

    /**
    * Get the name of this instruction for diagnostic and tracing purposes
    */

    public int getInstructionNameCode() {
        return StandardNames.XSL_COPY;
    }

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        this.contextItemType = contextItemType;
        return super.analyze(env, contextItemType);
    }

    /**
     * Get the item type of the result of this instruction.
     * @return The context item type.
     */

    public ItemType getItemType() {
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

    protected int getNameCode(XPathContext context)
            throws XPathException {
        return ((NodeInfo)context.getContextItem()).getNameCode();
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

        // Processing depends on the node kind.

        switch(source.getNodeKind()) {

        case Type.ELEMENT:
            // use the generic code for creating new elements
            return super.processLeavingTail(c2);

        case Type.ATTRIBUTE:
            try {
                CopyOf.copyAttribute(source, schemaType, validation, locationId, c2);
            } catch (NoOpenStartTagException err) {
                DynamicError e = new DynamicError(err.getMessage());
                e.setXPathContext(context);
                e.setErrorCode(err.getErrorCodeLocalPart());
                context.getController().recoverableError(e);
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
                context.getController().recoverableError(e);
            }
            break;

        case Type.DOCUMENT:
            Receiver val = controller.getConfiguration().
                    getDocumentValidator(out,
                                         source.getBaseURI(),
                                         controller.getNamePool(),
                                         validation);
            if (val != out) {
                SequenceReceiver sr = new TreeReceiver(val);
                sr.setPipelineConfiguration(out.getPipelineConfiguration());
                c2.setReceiver(sr);
                val = sr;
            }
            val.startDocument(0);
            content.process(c2);
            val.endDocument();
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
        SequenceOutputter seq = new SequenceOutputter(1);
        seq.setPipelineConfiguration(controller.makePipelineConfiguration());
        c2.setTemporaryReceiver(seq);
        process(c2);
        seq.close();
        return seq.getFirstItem();
    }

    /**
     * Diagnostic print of expression structure. The expression is written to the System.err
     * output stream
     *
     * @param level indentation level for this expression
     * @param out
     */

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "copy");
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
