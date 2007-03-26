package org.orbeon.saxon.instruct;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.event.*;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.ContentTypeTest;
import org.orbeon.saxon.pattern.NodeKindTest;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.Value;
import org.orbeon.saxon.value.Whitespace;

import java.io.PrintStream;
import java.util.Iterator;


/**
 * An xsl:copy-of element in the stylesheet.
 */

public class CopyOf extends Instruction {

    private Expression select;
    private boolean copyNamespaces;
    private int validation;
    private SchemaType schemaType;
    private boolean requireDocumentOrElement = false;
    private boolean rejectDuplicateAttributes;
    private boolean readOnce = false;
    private boolean validating;

    public CopyOf(Expression select,
                  boolean copyNamespaces,
                  int validation,
                  SchemaType schemaType,
                  boolean rejectDuplicatAttributes) {
        this.select = select;
        this.copyNamespaces = copyNamespaces;
        this.validation = validation;
        this.schemaType = schemaType;
        this.validating = schemaType != null || validation != Validation.PRESERVE;
        this.rejectDuplicateAttributes = rejectDuplicatAttributes;
        adoptChildExpression(select);
    }

    public void setReadOnce(boolean b) {
        readOnce = b;
    }

    /**
     * Determine whether this instruction creates new nodes.
     * The result depends on the type of the select expression.
     */

    public final boolean createsNewNodes() {
        Executable exec = getExecutable();
        if (exec == null) {
            return true;    // This shouldn't happen, but we err on the safe side
        }
        final TypeHierarchy th = exec.getConfiguration().getTypeHierarchy();
        return !select.getItemType(th).isAtomicType();
    }

    /**
     * Get the name of this instruction, for diagnostics and tracing
     */

    public int getInstructionNameCode() {
        return StandardNames.XSL_COPY_OF;
    }

    /**
     * For XQuery, the operand (select) must be a single element or document node.
     *
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
     *
     * @param context the dynamic context for the transformation
     * @return null - this implementation of the method never returns a TailCall
     */

    public TailCall processLeavingTail(XPathContext context) throws XPathException {

        Controller controller = context.getController();
        SequenceReceiver out = context.getReceiver();
        boolean copyBaseURI = (out.getSystemId() == null);

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
                    DynamicError e = new DynamicError("Operand of validate expression must be a document or element node");
                    e.setXPathContext(context);
                    e.setErrorCode("XQTY0030");
                    throw e;
                }
                switch (kind) {

                    case Type.ELEMENT:
                        Receiver eval = out;
                        if (validating) {
                            eval = controller.getConfiguration().getElementValidator(out, source.getNameCode(),
                                    locationId, schemaType, validation);
                        }
                        if (copyBaseURI) {
                            eval.setSystemId(source.getBaseURI());
                        }
                        try {
                            source.copy(eval, whichNamespaces, true, locationId);
                        } catch (CopyNamespaceSensitiveException e) {
                            e.setErrorCode((getHostLanguage() == Configuration.XSLT ? "XTTE0950" : "XQTY0086"));
                            throw e;
                        }
//                        if (eval != out) {
//                            eval.close();
//                        }
                        break;

                    case Type.ATTRIBUTE:
                        try {
                            copyAttribute(source, schemaType, validation, this, context, rejectDuplicateAttributes);
                        } catch (NoOpenStartTagException err) {
                            DynamicError e = new DynamicError(err.getMessage());
                            e.setLocator(this);
                            e.setXPathContext(context);
                            e.setErrorCode(err.getErrorCodeLocalPart());
                            throw dynamicError(this, e, context);
                        }
                        break;
                    case Type.TEXT:
                        out.characters(source.getStringValueCS(), locationId, 0);
                        break;

                    case Type.PROCESSING_INSTRUCTION:
                        if (copyBaseURI) {
                            out.setSystemId(source.getBaseURI());
                        }
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
                            //context.getController().recoverableError(e);
                            throw dynamicError(this, e, context);
                        }
                        break;

                    case Type.DOCUMENT:
                        Receiver val = controller.getConfiguration().
                                getDocumentValidator(out,
                                        source.getBaseURI(),
                                        validation, Whitespace.NONE, schemaType);
                        val.setPipelineConfiguration(out.getPipelineConfiguration());
                        if (copyBaseURI) {
                            val.setSystemId(source.getBaseURI());
                        }
                        try {
                            source.copy(val, whichNamespaces, true, locationId);
                        } catch (CopyNamespaceSensitiveException e) {
                            e.setErrorCode((getHostLanguage() == Configuration.XSLT ? "XTTE0950" : "XQTY0086"));
                            throw e;
                        }
//                        if (val != out) {
//                            val.close();
//                        }
                        break;

                    default:
                        throw new IllegalArgumentException("Unknown node kind " + source.getNodeKind());
                }

            } else {
                out.append(item, locationId, NodeInfo.ALL_NAMESPACES);
            }
        }
        return null;
    }

    /**
     * Method shared by xsl:copy and xsl:copy-of to copy an attribute node
     * @param source            the node to be copied
     * @param schemaType        the simple type against which the value is to be validated, if any
     * @param validation        one of preserve, strip, strict, lax
     * @param instruction       the calling instruction, used for diagnostics
     * @param context           the dynamic context
     * @param rejectDuplicates  true if duplicate attributes with the same name are disallowed (XQuery)
     * @throws XPathException
     */

    static void copyAttribute(NodeInfo source,
                                        SchemaType schemaType,
                                        int validation,
                                        Instruction instruction,
                                        XPathContext context,
                                        boolean rejectDuplicates)
            throws XPathException {
        int nameCode = source.getNameCode();
        int annotation = StandardNames.XDT_UNTYPED_ATOMIC;
        int opt = 0;
        if (rejectDuplicates) {
            opt |= ReceiverOptions.REJECT_DUPLICATES;
        }
        CharSequence value = source.getStringValueCS();
        if (schemaType != null) {
            if (schemaType.isSimpleType()) {
                if (((SimpleType) schemaType).isNamespaceSensitive()) {
                    DynamicError err = new DynamicError("Cannot create a parentless attribute whose " +
                            "type is namespace-sensitive (such as xs:QName)");
                    err.setErrorCode("XTTE1545");
                    err.setXPathContext(context);
                    err.setLocator(instruction);
                    throw err;
                }
                try {
                    XPathException err = ((SimpleType) schemaType).validateContent(value, DummyNamespaceResolver.getInstance(), context.getConfiguration().getNameChecker());
                    if (err != null) {
                        throw new ValidationException("Attribute being copied does not match the required type. " +
                                err.getMessage());
                    }
                    annotation = schemaType.getFingerprint();
                } catch (UnresolvedReferenceException ure) {
                    throw new ValidationException(ure);
                }
            } else {
                DynamicError e = new DynamicError("Cannot validate an attribute against a complex type");
                e.setXPathContext(context);
                e.setErrorCode("XTSE1530");
                throw e;
            }
        } else if (validation == Validation.STRICT || validation == Validation.LAX) {
            try {
                annotation = context.getConfiguration().validateAttribute(nameCode, value, validation);
            } catch (ValidationException e) {
                DynamicError err = DynamicError.makeDynamicError(e);
                err.setErrorCode(e.getErrorCodeLocalPart());
                err.setXPathContext(context);
                err.setLocator(instruction);
                err.setIsTypeError(true);
                throw err;
            }

        } else if (validation == Validation.PRESERVE) {
            annotation = source.getTypeAnnotation() & NamePool.FP_MASK;
            if (annotation != StandardNames.XDT_UNTYPED_ATOMIC) {
                SchemaType type = context.getConfiguration().getSchemaType(annotation);
                if (((AtomicType) type).isNamespaceSensitive()) {
                    DynamicError err = new DynamicError("Cannot preserve type annotation when copying an attribute with namespace-sensitive content");
                    err.setErrorCode("XTTE0950");
                    err.setIsTypeError(true);
                    err.setXPathContext(context);
                    throw err;
                }
            }
        }

        context.getReceiver().attribute(nameCode, annotation, value, instruction.getLocationId(), opt);
    }

    public Expression simplify(StaticContext env) throws XPathException {
        select = select.simplify(env);
        return this;
    }

    public ItemType getItemType(TypeHierarchy th) {
        if (schemaType != null) {
            Configuration config = getExecutable().getConfiguration();
            ItemType in = select.getItemType(th);
            int e = th.relationship(in, NodeKindTest.ELEMENT);
            if (e == TypeHierarchy.SAME_TYPE || e == TypeHierarchy.SUBSUMED_BY) {
                return new ContentTypeTest(Type.ELEMENT, schemaType, config);
            }
            int a = th.relationship(in, NodeKindTest.ATTRIBUTE);
            if (a == TypeHierarchy.SAME_TYPE || a == TypeHierarchy.SUBSUMED_BY) {
                return new ContentTypeTest(Type.ATTRIBUTE, schemaType, config);
            }
        }
        return select.getItemType(th);
    }

    public int getCardinality() {
        return select.getCardinality();
    }

    public int getDependencies() {
        return select.getDependencies();
    }

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        select = doPromotion(select, offer);
    }

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        select = select.typeCheck(env, contextItemType);
        adoptChildExpression(select);
        return this;
    }

    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        if (readOnce) {
            Expression optcopy = opt.optimizeCopy(select);
            if (optcopy != null) {
                return optcopy;
            }
        }
        select = select.optimize(opt, env, contextItemType);
        adoptChildExpression(select);
        return this;
    }

    /**
     * Diagnostic print of expression structure. The expression is written to the System.err
     * output stream
     *
     * @param level  indentation level for this expression
     * @param out
     * @param config
     */

    public void display(int level, PrintStream out, Configuration config) {
        out.println(ExpressionTool.indent(level) + "copyOf " +
                ("validation=" + Validation.toString(validation)));
        select.display(level + 1, out, config);
    }

    public Iterator iterateSubExpressions() {
        return new MonoIterator(select);
    }

    /**
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        if (select == original) {
            select = replacement;
            found = true;
        }
                return found;
    }



    /**
     * Return the first item if there is one, or null if not
     *
     * @param context
     * @return the result of evaluating the instruction
     * @throws XPathException
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return super.evaluateItem(context);
    }

    public SequenceIterator iterate(final XPathContext context) throws XPathException {
        if (validation == Validation.PRESERVE && schemaType == null && copyNamespaces) {
            // create a virtual copy of the underlying nodes
            ItemMappingFunction copier = new ItemMappingFunction() {
                public Item map(Item item) {
                    if (item instanceof AtomicValue) {
                        return item;
                    }
                    VirtualCopy vc = VirtualCopy.makeVirtualCopy((NodeInfo) item, (NodeInfo) item);
                    int documentNumber =
                            context.getController().getConfiguration().getDocumentNumberAllocator().allocateDocumentNumber();
                    vc.setDocumentNumber(documentNumber);
                    return vc;
                }
            };
            return new ItemMappingIterator(select.iterate(context), copier);
        }
        Controller controller = context.getController();
        XPathContext c2 = context.newMinorContext();
        c2.setOrigin(this);
        SequenceOutputter out = new SequenceOutputter();
        final PipelineConfiguration pipe = controller.makePipelineConfiguration();
        pipe.setHostLanguage(getHostLanguage());
        out.setPipelineConfiguration(pipe);
        c2.setReceiver(out);
        try {
            process(c2);
            return Value.getIterator(out.getSequence());
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
