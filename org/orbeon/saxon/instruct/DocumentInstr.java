package net.sf.saxon.instruct;

import net.sf.saxon.Controller;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.SequenceReceiver;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.ExpressionTool;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.tinytree.TinyBuilder;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.value.TextFragmentValue;
import net.sf.saxon.xpath.XPathException;

import java.io.PrintStream;


/**
 * An instruction to create a document node. This doesn't correspond directly
 * to any XSLT instruction. It is used to support the document node constructor
 * expression in XQuery, and is used as a sub-instruction within an xsl:variable
 * that constructs a temporary tree.
 *
 * <p>Conceptually it represents an XSLT instruction xsl:document-node,
 * with no attributes, whose content is a complex content constructor for the
 * children of the document node.</p>
 */

public class DocumentInstr extends InstructionWithChildren {

    private static final int[] treeSizeParameters = {50, 10, 5, 200};
    // estimated size of a temporary tree: {nodes, attributes, namespaces, characters}

    boolean textOnly = false;
    String constantText = null;
    String baseURI = null;
    private int validationAction = Validation.PRESERVE;
    private SchemaType schemaType;

    public DocumentInstr(boolean textOnly,
                         String constantText,
                         String baseURI) {
        this.textOnly = textOnly;
        this.constantText = constantText;
        this.baseURI = baseURI;
    }

    /**
     * Set the validation action
     */

    public void setValidationAction(int action) {
        validationAction = action;
    }

    /**
     * Set the SchemaType of the document element
     */

    public void setSchemaType(SchemaType type) {
        schemaType = type;
    }

    public ItemType getItemType() {
        return NodeKindTest.DOCUMENT;
    }

    public int getCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        Item item = evaluateItem(context);
        if (item != null) {
            SequenceReceiver out = context.getReceiver();
            out.append(item, locationId);
        }
        return null;
    }

    /**
     * Evaluate as an expression.
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Controller controller = context.getController();
        DocumentInfo root = null;
        if (textOnly) {
            CharSequence textValue;
            if (constantText != null) {
                textValue = constantText;
            } else {
                textValue = new StringBuffer();
                Expression[] children = getChildren();
                for (int i=0; i<children.length; i++) {
                    SequenceIterator iter = children[i].iterate(context);
                    if (iter instanceof AtomizableIterator) {
                        ((AtomizableIterator)iter).setIsAtomizing(true);
                    }
                    while (true) {
                        Item item = iter.next();
                        if (item==null) break;
                        ((StringBuffer)textValue).append(item.getStringValue());
                    }
                }
            }
            root = new TextFragmentValue(textValue, baseURI);
            root.setConfiguration(controller.getConfiguration());
        } else {
            XPathContext c2 = context.newMinorContext();
            c2.setOrigin(this);

            // TODO: delayed evaluation of temporary trees, in the same way as
            // node-sets. This requires saving the controller, including values of local variables
            // and any assignable global variables (ouch).

            // TODO: use an Outputter that delayes the decision whether to build a
            // TextFragment or a TinyTree until the first element is encountered, to
            // avoid the overhead of using a TinyTree for text-only trees. This would
            // make the static analysis superfluous.

            TinyBuilder builder = new TinyBuilder();
            //System.err.println("Build doc " + builder);
            builder.setSizeParameters(treeSizeParameters);
            builder.setLineNumbering(controller.getConfiguration().isLineNumbering());

            Receiver receiver = builder;
            receiver.setSystemId(baseURI);
            receiver.setConfiguration(controller.getConfiguration());
            receiver.open();
            receiver.startDocument(0);
            c2.changeOutputDestination(null,
                    receiver,
                    false,
                    validationAction,
                    schemaType);
            processChildren(c2);
            //c2.resetOutputDestination(old);
            receiver.endDocument();
            receiver.close();
            //System.err.println("End build doc " + builder);

            root = builder.getCurrentDocument();
        }
        return root;
    }


    /**
     * Get the name of this instruction for diagnostic and tracing purposes
     * (the string "document-constructor")
     */

    public int getInstructionNameCode() {
        return StandardNames.XSL_DOCUMENT;
    }

    /**
     * Display this instruction as an expression, for diagnostics
     */

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "document-constructor");
        if (children.length == 0) {
            out.println(ExpressionTool.indent(level + 1) + "empty content");
        } else {
            InstructionWithChildren.displayChildren(children, level + 1, pool, out);
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
