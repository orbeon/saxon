package org.orbeon.saxon.instruct;

import org.orbeon.saxon.Controller;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.functions.StringJoin;
import org.orbeon.saxon.functions.SystemFunction;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.event.SequenceReceiver;
import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.NodeKindTest;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.pull.UnconstructedDocument;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.tinytree.TinyBuilder;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.TextFragmentValue;
import org.orbeon.saxon.value.StringValue;
import org.orbeon.saxon.value.UntypedAtomicValue;

import java.io.PrintStream;


/**
 * An instruction to create a document node. This corresponds to the xsl:document-node
 * instruction in XSLT. It is also used to support the document node constructor
 * expression in XQuery, and is generated implicitly within an xsl:variable
 * that constructs a temporary tree.
 *
 * <p>Conceptually it represents an XSLT instruction xsl:document-node,
 * with no attributes, whose content is a complex content constructor for the
 * children of the document node.</p>
 */

public class DocumentInstr extends ParentNodeConstructor {

    //private static final int[] treeSizeParameters = {50, 10, 5, 200};
    // estimated size of a temporary tree: {nodes, attributes, namespaces, characters}

    private boolean textOnly;
    private String constantText;



    public DocumentInstr(boolean textOnly,
                         String constantText,
                         String baseURI) {
        this.textOnly = textOnly;
        this.constantText = constantText;
        setBaseURI(baseURI);
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is prefered. For instructions this is the process() method.
     */

    public int getImplementationMethod() {
        return Expression.EVALUATE_METHOD;
    }


    /**
     * Set the validation action
     */

    public void setValidationAction(int action) {
        validation = action;
    }

    /**
     * Determine whether this is a "text only" document: essentially, an XSLT xsl:variable that contains
     * a single text node or xsl:value-of instruction.
     */

    public boolean isTextOnly() {
        return textOnly;
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
        setLazyConstruction(env.getConfiguration().isLazyConstructionMode());
        return super.simplify(env);
    }

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        Expression res = super.typeCheck(env, contextItemType);
        //verifyLazyConstruction();
        return res;
    }

    /**
     * Check statically whether the content of the element creates attributes or namespaces
     * after creating any child nodes
     * @param env the static context
     * @throws XPathException
     */

    protected void checkContentForAttributes(StaticContext env) throws XPathException {
        if (content instanceof Block) {
            TypeHierarchy th = env.getConfiguration().getTypeHierarchy();
            Expression[] components = ((Block)content).getChildren();
            for (int i=0; i<components.length; i++) {
                ItemType it = components[i].getItemType(th);
                if (it instanceof NodeTest) {
                    int possibleNodeKinds = ((NodeTest)it).getNodeKindMask();
                    if (possibleNodeKinds == 1<<Type.ATTRIBUTE) {
                        DynamicError de = new DynamicError(
                                "Cannot create an attribute node whose parent is a document node");
                        de.setErrorCode(isXSLT() ? "XTDE0420" : "XPTY0004");
                        de.setLocator(this);
                        throw de;
                    } else if (possibleNodeKinds == 1<<Type.NAMESPACE) {
                        DynamicError de = new DynamicError(
                                "Cannot create a namespace node whose parent is a document node");
                        de.setErrorCode(isXSLT() ? "XTDE0420" : "XQTY0024");
                        de.setLocator(this);
                        throw de;
                    }
                }
            }

        }
    }

    /**
     * In the case of a text-only instruction (xsl:variable containing a text node or one or more xsl:value-of
     * instructions, return an expression that evaluates to the textual content
     * @return an expression that evaluates to the textual content
     */

    public Expression getStringValueExpression(StaticContext env) {
        if (textOnly) {
            if (constantText != null) {
                return new UntypedAtomicValue(constantText);
            } else if (content instanceof ValueOf) {
                return ((ValueOf)content).convertToStringJoin(env);
            } else {
                StringJoin fn = (StringJoin)SystemFunction.makeSystemFunction("string-join", 2, env.getNamePool());
                Expression[] args = new Expression[2];
                args[0] = content;
                args[1] = StringValue.EMPTY_STRING;
                fn.setArguments(args);
                return fn;
            }
        } else {
            throw new AssertionError("getStringValueExpression() called on non-text-only document instruction");
        }
    }

    /**
     * Get the item type
     * @param th The TypeHierarchy
     * @return the in
     */
    public ItemType getItemType(TypeHierarchy th) {
        return NodeKindTest.DOCUMENT;
    }

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        Item item = evaluateItem(context);
        if (item != null) {
            SequenceReceiver out = context.getReceiver();
            out.append(item, locationId, NodeInfo.ALL_NAMESPACES);
        }
        return null;
    }

    /**
     * Evaluate as an expression.
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        if (isLazyConstruction()) {
            // TODO: is validation being forgotten on this path?
            return new UnconstructedDocument(this, context);
        } else {
            Controller controller = context.getController();
            DocumentInfo root;
            if (textOnly) {
                CharSequence textValue;
                if (constantText != null) {
                    textValue = constantText;
                } else {
                    FastStringBuffer sb = new FastStringBuffer(100);
                    SequenceIterator iter = content.iterate(context);
                    if (iter instanceof AtomizableIterator) {
                        ((AtomizableIterator)iter).setIsAtomizing(true);
                    }
                    while (true) {
                        Item item = iter.next();
                        if (item==null) break;
                        sb.append(item.getStringValueCS());
                    }
                    textValue = sb.condense();
                }
                root = new TextFragmentValue(textValue, getBaseURI());
                ((TextFragmentValue)root).setConfiguration(controller.getConfiguration());
            } else {
                try {
                    XPathContext c2 = context.newMinorContext();
                    c2.setOrigin(this);

                    TinyBuilder builder = new TinyBuilder();
                    //builder.setSizeParameters(treeSizeParameters);
                    builder.setLineNumbering(controller.getConfiguration().isLineNumbering());

                    Receiver receiver = builder;
                    //receiver.setSystemId(getBaseURI());
                    builder.setBaseURI(getBaseURI());

                    final PipelineConfiguration pipe = controller.makePipelineConfiguration();
                    pipe.setHostLanguage(getHostLanguage());
                    //pipe.setBaseURI(baseURI);
                    receiver.setPipelineConfiguration(pipe);

                    c2.changeOutputDestination(null,
                            receiver,
                            false,
                            getHostLanguage(),
                            validation,
                            getSchemaType());
                    Receiver out = c2.getReceiver();
                    out.open();
                    out.startDocument(0);

                    content.process(c2);

                    out.endDocument();
                    out.close();

                    root = (DocumentInfo)builder.getCurrentRoot();
                } catch (XPathException e) {
                    if (e.getLocator() == null) {
                        e.setLocator(this);
                    }
                    if (e instanceof DynamicError && ((DynamicError)e).getXPathContext() == null) {
                        ((DynamicError)e).setXPathContext(context);
                    }
                    throw e;
                }
            }
            return root;
        }
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

    public void display(int level, PrintStream out, Configuration config) {
        out.println(ExpressionTool.indent(level) + "document-constructor");
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
