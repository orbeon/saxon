package net.sf.saxon.instruct;
import net.sf.saxon.Controller;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.event.NoOpenStartTagException;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.SequenceReceiver;
import net.sf.saxon.event.TreeReceiver;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.ExpressionTool;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.xpath.DynamicError;
import net.sf.saxon.xpath.XPathException;

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
            element.outputNamespaceNodes(receiver, true);
        }
    }

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        Controller controller = context.getController();
        XPathContext c2 = context.newMinorContext();
        c2.setOrigin(this);
        SequenceReceiver out = c2.getReceiver();
        Item item = context.getContextItem();
        if (!(item instanceof NodeInfo)) {
            out.append(item, locationId);
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
                e.setErrorCode(err.getErrorCode());
                context.getController().recoverableError(e);
            }
            break;

        case Type.TEXT:
            out.characters(source.getStringValue(), locationId, 0);
            break;

        case Type.PROCESSING_INSTRUCTION:
            out.processingInstruction(source.getDisplayName(), source.getStringValue(), locationId, 0);
            break;

        case Type.COMMENT:
            out.comment(source.getStringValue(), locationId, 0);
            break;

        case Type.NAMESPACE:
            try {
                source.copy(out, NodeInfo.NO_NAMESPACES, false, locationId);
            } catch (NoOpenStartTagException err) {
                DynamicError e = new DynamicError(err.getMessage());
                e.setXPathContext(context);
                e.setErrorCode(err.getErrorCode());
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
                sr.setConfiguration(controller.getConfiguration());
                c2.setReceiver(sr);
                val = sr;
            }
            val.setConfiguration(controller.getConfiguration());
            val.startDocument(0);
            processChildren(c2);
            val.endDocument();
            break;

        default:
            throw new IllegalArgumentException("Unknown node kind " + source.getNodeKind());

        }
        return null;
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
