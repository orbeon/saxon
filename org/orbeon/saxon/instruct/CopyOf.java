package org.orbeon.saxon.instruct;

import org.orbeon.saxon.Controller;
import org.orbeon.saxon.event.*;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.xpath.DynamicError;
import org.orbeon.saxon.xpath.XPathException;

import java.io.PrintStream;
import java.util.Iterator;


/**
 * An xsl:copy-of element in the stylesheet.
 */

public class CopyOf extends Instruction implements MappingFunction {

    private Expression select;
    private boolean copyNamespaces;
    private int validation;
    private SchemaType schemaType;
    private boolean requireDocumentOrElement = false;

    public CopyOf(Expression select,
                  boolean copyNamespaces,
                  int validation,
                  SchemaType schemaType) {
        this.select = select;
        this.copyNamespaces = copyNamespaces;
        this.validation = validation;
        this.schemaType = schemaType;
    }

    /**
     * Determine whether this instruction creates new nodes.
     * This implementation returns true.
     */

    public final boolean createsNewNodes() {
        // TODO: return false if the static type of the select expression is atomic
        return true;
    }

    /**
     * Get the name of this instruction, for diagnostics and tracing
     */

    public int getInstructionNameCode() {
        return StandardNames.XSL_COPY_OF;
    }

    /**
     * For XQuery, the operand (select) must be a single element or document node.
     * @param requireDocumentOrElement
     */
    public void setRequireDocumentOrElement(boolean requireDocumentOrElement) {
        this.requireDocumentOrElement = requireDocumentOrElement;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided. This implementation provides both iterate() and
     * process() methods natively.
     */

    public int getImplementationMethod() {
        return ITERATE_METHOD | PROCESS_METHOD;
    }

    /**
     * Process this xsl:copy-of instruction
     * @param context the dynamic context for the transformation
     * @return null - this implementation of the method never returns a TailCall
     */

    public TailCall processLeavingTail(XPathContext context) throws XPathException {

        Controller controller = context.getController();
        SequenceReceiver out = context.getReceiver();

        int whichNamespaces = (copyNamespaces ? NodeInfo.ALL_NAMESPACES : NodeInfo.NO_NAMESPACES);

        SequenceIterator iter = select.iterate(context);
        while (true) {


            Item item = iter.next();
            if (item == null) {
                break;
            }
            if (item instanceof NodeInfo) {
                NodeInfo source = (NodeInfo) item;
                int kind = source.getNodeKind();
                if (requireDocumentOrElement &&
                        !(kind == Type.ELEMENT || kind == Type.DOCUMENT)) {
                    DynamicError e = new DynamicError(
                            "Operand of validate expression must be a document or element node"
                    );
                    e.setXPathContext(context);
                    e.setErrorCode("XQ0030");
                    throw e;
                }
                switch (kind) {

                    case Type.ELEMENT:

                        Receiver eval = controller.getConfiguration().getElementValidator(
                                out, source.getNameCode(), locationId,
                                schemaType, validation,
                                controller.getNamePool()
                        );
                        source.copy(eval, whichNamespaces, true, locationId);
                        break;

                    case Type.ATTRIBUTE:
                        try {
                            copyAttribute(source, schemaType, validation, locationId, context);
                        } catch (NoOpenStartTagException err) {
                            DynamicError e = new DynamicError(err.getMessage());
                            e.setLocator(this);
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
                        val.setPipelineConfiguration(out.getPipelineConfiguration());
                        val.startDocument(0);
                        source.copy(val, whichNamespaces, true, locationId);
                        val.endDocument();
                        break;

                    default:
                        throw new IllegalArgumentException("Unknown node kind " + source.getNodeKind());
                }

            } else {
                out.append(item, locationId);
            }
        }
        return null;
    }

    protected static void copyAttribute(NodeInfo source,
                                        SchemaType schemaType,
                                        int validation,
                                        int locationId,
                                        XPathContext context)
            throws XPathException {
        int nameCode = source.getNameCode();
        int annotation = -1;
        int opt = 0;
        String value = source.getStringValue();
        if (schemaType != null) {
            if (schemaType.isSimpleType()) {
                try {
                    ((SimpleType) schemaType).validateContent(value, DummyNamespaceResolver.getInstance());
                    if (((SimpleType) schemaType).isNamespaceSensitive()) {
                        opt |= ReceiverOptions.NEEDS_PREFIX_CHECK;
                    }
                    annotation = schemaType.getFingerprint();
                } catch (UnresolvedReferenceException ure) {
                    throw new ValidationException(ure);
                } catch (ValidationException err) {
                    throw new ValidationException("Attribute being copied does not match the required type. " +
                            err.getMessage());
                }
            } else {
                DynamicError e = new DynamicError("Cannot validate an attribute against a complex type");
                e.setXPathContext(context);
                e.setErrorCode("XT1530");
                throw e;
            }
        } else if (validation == Validation.STRICT ||
                validation == Validation.LAX) {
            long res = context.getController().getConfiguration().validateAttribute(nameCode,
                    value,
                    validation);
            annotation = (int) (res & 0xffffffff);
            opt |= (int) (res >> 32);
        }

        context.getReceiver().attribute(nameCode, annotation, value, locationId, opt);
    }

    public Expression simplify(StaticContext env) throws XPathException {
        select = select.simplify(env);
        return this;
    }

    public ItemType getItemType() {
        return select.getItemType();
        // TODO: could do better than this if the instruction is validating
    }

    public int getCardinality() {
        return select.getCardinality();
    }

    public int getDependencies() {
        return select.getDependencies();
    }

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        select = select.promote(offer);
    }

    /**
     * Perform static analysis of an expression and its subexpressions.
     *
     * <p>This checks statically that the operands of the expression have
     * the correct type; if necessary it generates code to do run-time type checking or type
     * conversion. A static type error is reported only if execution cannot possibly succeed, that
     * is, if a run-time type error is inevitable. The call may return a modified form of the expression.</p>
     *
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable. However, the types of such functions and
     * variables will only be accurately known if they have been explicitly declared.</p>
     *
     * @param env the static context of the expression
     * @exception XPathException if an error is discovered during this phase
     *     (typically a type error)
     * @return the original expression, rewritten to perform necessary
     *     run-time type checks, and to perform other type-related
     *     optimizations
     */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        select = select.analyze(env, contextItemType);
        return this;
    }

    /**
     * Diagnostic print of expression structure. The expression is written to the System.err
     * output stream
     *
     * @param level indentation level for this expression
     * @param out
     */

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "copyOf " +
                ("validation=" + Validation.toString(validation)));
        select.display(level + 1, pool, out);
    }

    public Iterator iterateSubExpressions() {
        return new MonoIterator(select);
    }

    /**
     * Return the first item if there is one, or null if not
     * @param context
     * @return the result of evaluating the instruction
     * @throws XPathException
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return super.evaluateItem(context);
    }

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        if (validation==Validation.PRESERVE && schemaType==null && copyNamespaces) {
            // create a virtual copy of the underlying nodes
            return new MappingIterator(select.iterate(context), this, null, context);
        }
        Controller controller = context.getController();
        XPathContext c2 = context.newMinorContext();
        c2.setOrigin(this);
        SequenceOutputter out = new SequenceOutputter();
        out.setPipelineConfiguration(controller.makePipelineConfiguration());
        c2.setReceiver(out);
        try {
            process(c2);
            return out.getSequence().iterate(c2);
        } catch (XPathException err) {
            if (err instanceof ValidationException) {
                ((ValidationException) err).setSourceLocator(this);
                ((ValidationException) err).setSystemId(getSystemId());
            }
            if (err.getLocator() == null) {
                err.setLocator(this);
            }
            throw err;
        }
    }

    /**
     * Mapping function used to perform the copy when using the iterate() method
     */

    /**
     * Map one item to a sequence.
     *
     * @param item    The item to be mapped.
     *                If context is supplied, this must be the same as context.currentItem().
     * @param context The processing context. This is supplied only for mapping constructs that
     *                set the context node, position, and size. Otherwise it is null.
     * @param info    Arbitrary information supplied by the creator of the MappingIterator. It must be
     *                read-only and immutable for the duration of the iteration.
     * @return either (a) a SequenceIterator over the sequence of items that the supplied input
     *         item maps to, or (b) an Item if it maps to a single item, or (c) null if it maps to an empty
     *         sequence.
     */

    public Object map(Item item, XPathContext context, Object info) throws XPathException {
        if (item instanceof AtomicValue) {
            return item;
        }
        VirtualCopy vc = VirtualCopy.makeVirtualCopy((NodeInfo)item, (NodeInfo)item);
        int documentNumber = ((XPathContext)info).getController().getNamePool().allocateDocumentNumber(vc);
        vc.setDocumentNumber(documentNumber);
        return vc;
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
