package org.orbeon.saxon.instruct;

import org.orbeon.saxon.Controller;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.event.SequenceReceiver;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.NodeKindTest;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.tinytree.TinyBuilder;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.value.TextFragmentValue;
import org.orbeon.saxon.xpath.XPathException;

import java.io.PrintStream;
import java.util.Iterator;


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

public class DocumentInstr extends Instruction {

    private static final int[] treeSizeParameters = {50, 10, 5, 200};
    // estimated size of a temporary tree: {nodes, attributes, namespaces, characters}

    private Expression content;
    private boolean textOnly;
    private String constantText;
    private String baseURI;
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
     * Set the expression that constructs the content
     */

    public void setContent(Expression content) {
        this.content = content;
        adoptChildExpression(content);
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

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression). The default implementation does nothing.
     * @return the simplified expression
     * @throws org.orbeon.saxon.xpath.XPathException
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
     * @throws org.orbeon.saxon.xpath.XPathException
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
     * @throws XPathException
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
        DocumentInfo root;
        if (textOnly) {
            CharSequence textValue;
            if (constantText != null) {
                textValue = constantText;
            } else {
                textValue = new StringBuffer(100);
                SequenceIterator iter = content.iterate(context);
                if (iter instanceof AtomizableIterator) {
                    ((AtomizableIterator)iter).setIsAtomizing(true);
                }
                while (true) {
                    Item item = iter.next();
                    if (item==null) break;
                    ((StringBuffer)textValue).append(item.getStringValue());
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
            receiver.setPipelineConfiguration(controller.makePipelineConfiguration());

            c2.changeOutputDestination(null,
                    receiver,
                    false,
                    validationAction,
                    schemaType);
            Receiver out = c2.getReceiver();
            out.open();
            out.startDocument(0);

            content.process(c2);

            out.endDocument();
            out.close();

            root = (DocumentInfo)builder.getCurrentRoot();
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
        content.display(level+1, pool, out);
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
