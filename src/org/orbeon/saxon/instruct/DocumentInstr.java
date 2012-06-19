package org.orbeon.saxon.instruct;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.event.Builder;
import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.event.SequenceReceiver;
import org.orbeon.saxon.evpull.BracketedDocumentIterator;
import org.orbeon.saxon.evpull.EventIterator;
import org.orbeon.saxon.evpull.SingletonEventIterator;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.functions.StringJoin;
import org.orbeon.saxon.functions.SystemFunction;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.NodeKindTest;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.pull.UnconstructedDocument;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.value.StringValue;
import org.orbeon.saxon.value.TextFragmentValue;
import org.orbeon.saxon.value.UntypedAtomicValue;


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

    /**
     * Create a document constructor instruction
     * @param textOnly true if the content contains text nodes only
     * @param constantText if the content contains text nodes only and the text is known at compile time,
     *        supplies the textual content
     * @param baseURI the base URI of the instruction
     */

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
     * Determine whether this is a "text only" document: essentially, an XSLT xsl:variable that contains
     * a single text node or xsl:value-of instruction.
     * @return true if this is a text-only document
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
     * @param visitor an expression visitor
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        setLazyConstruction(visitor.getConfiguration().isLazyConstructionMode());
        return super.simplify(visitor);
    }



    /**
     * Check statically that the sequence of child instructions doesn't violate any obvious constraints
     * on the content of the node
     * @param env the static context
     * @throws XPathException
     */

    protected void checkContentSequence(StaticContext env) throws XPathException {
        checkContentSequence(env, content, validation, getSchemaType());
    }

    protected static void checkContentSequence(StaticContext env, Expression content, int validation, SchemaType type)
    throws XPathException {
        Expression[] components;
        if (content instanceof Block) {
            components = ((Block)content).getChildren();
        } else {
            components = new Expression[] {content};
        }

        int elementCount = 0;
        boolean isXSLT = content.getHostLanguage() == Configuration.XSLT;
        TypeHierarchy th = env.getConfiguration().getTypeHierarchy();
        for (int i=0; i<components.length; i++) {
            ItemType it = components[i].getItemType(th);
            if (it instanceof NodeTest) {
                int possibleNodeKinds = ((NodeTest)it).getNodeKindMask();
                if (possibleNodeKinds == 1<<Type.ATTRIBUTE) {
                    XPathException de = new XPathException("Cannot create an attribute node whose parent is a document node");
                    de.setErrorCode(isXSLT ? "XTDE0420" : "XPTY0004");
                    de.setLocator(components[i]);
                    throw de;
                } else if (possibleNodeKinds == 1<<Type.NAMESPACE) {
                    XPathException de = new XPathException("Cannot create a namespace node whose parent is a document node");
                    de.setErrorCode(isXSLT ? "XTDE0420" : "XQTY0024");
                    de.setLocator(components[i]);
                    throw de;
                }
                if (possibleNodeKinds == 1<<Type.ELEMENT) {
                    elementCount++;
                    if (elementCount > 1 &&
                            (validation==Validation.STRICT || validation==Validation.LAX || type!=null)) {
                        XPathException de = new XPathException("A valid document must have only one child element");
                        if (isXSLT) {
                            de.setErrorCode("XTTE1550");
                        } else {
                            de.setErrorCode("XQDY0061");
                        }
                        de.setLocator(components[i]);
                        throw de;
                    }
                    if (validation==Validation.STRICT && components[i] instanceof FixedElement) {
                        SchemaDeclaration decl = env.getConfiguration().getElementDeclaration(
                                ((FixedElement)components[i]).getNameCode(null) & NamePool.FP_MASK);
                        if (decl != null) {
                            ((FixedElement)components[i]).getContentExpression().
                                    checkPermittedContents(decl.getType(), env, true);
                        }
                    }
                }
            }
        }
    }

    /**
     * In the case of a text-only instruction (xsl:variable containing a text node or one or more xsl:value-of
     * instructions), return an expression that evaluates to the textual content as an instance of xs:untypedAtomic
     * @param env the static evaluation context
     * @return an expression that evaluates to the textual content
     */

    public Expression getStringValueExpression(StaticContext env) {
        if (textOnly) {
            if (constantText != null) {
                return new StringLiteral(new UntypedAtomicValue(constantText));
            } else if (content instanceof ValueOf) {
                return ((ValueOf)content).convertToStringJoin(env);
            } else {
                StringJoin fn = (StringJoin)SystemFunction.makeSystemFunction(
                        "string-join", new Expression[]{content, new StringLiteral(StringValue.EMPTY_STRING)});
                CastExpression cast = new CastExpression(fn, BuiltInAtomicType.UNTYPED_ATOMIC, false);
                ExpressionTool.copyLocationInfo(this, cast);
                return cast;
            }
        } else {
            throw new AssertionError("getStringValueExpression() called on non-text-only document instruction");
        }
    }


    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        DocumentInstr doc = new DocumentInstr(textOnly, constantText, getBaseURI());
        doc.setContentExpression(content.copy());
        doc.setValidationMode(getValidationMode());
        doc.setSchemaType(getSchemaType());
        doc.setLazyConstruction(isLazyConstruction());
        return doc;
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
        // TODO: we're always constructing the document in memory. Sometimes we could push it out directly.
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
        if (isLazyConstruction() && (
                context.getConfiguration().areAllNodesUntyped() ||
                        (validation == Validation.PRESERVE && getSchemaType() == null))) {
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

                    Builder builder = controller.makeBuilder();
                    //builder.setSizeParameters(treeSizeParameters);
                    builder.setLineNumbering(controller.getConfiguration().isLineNumbering());

                    //receiver.setSystemId(getBaseURI());
                    builder.setBaseURI(getBaseURI());
                    builder.setTiming(false);

                    PipelineConfiguration pipe = controller.makePipelineConfiguration();
                    pipe.setHostLanguage(getHostLanguage());
                    //pipe.setBaseURI(baseURI);
                    builder.setPipelineConfiguration(pipe);

                    c2.changeOutputDestination(null,
                            builder,
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
                    e.maybeSetLocation(this);
                    e.maybeSetContext(context);
                    throw e;
                }
            }
            return root;
        }
    }

    public EventIterator iterateEvents(XPathContext context) throws XPathException {
        if (validation != Validation.PRESERVE) {
            // Schema validation can't be done in pull mode
            return new SingletonEventIterator(evaluateItem(context));
        }
        return new BracketedDocumentIterator(content.iterateEvents(context));

    }


    /**
     * Get the name of this instruction for diagnostic and tracing purposes
     * (the string "document-constructor")
     */

    public int getInstructionNameCode() {
        return StandardNames.XSL_DOCUMENT;
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("documentNode");
        out.emitAttribute("validation", Validation.toString(validation));
        if (getSchemaType() != null) {
            out.emitAttribute("type", getSchemaType().getDescription());
        }        
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
