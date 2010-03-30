package org.orbeon.saxon.instruct;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.ContentTypeTest;
import org.orbeon.saxon.pattern.NodeKindTest;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.value.*;

import java.util.ArrayList;
import java.util.Iterator;


/**
 * An instruction representing an xsl:element element in an XSLT stylesheet,
 * or a computed element constructor in XQuery. (In both cases, if the element name
 * is expressed as a compile-time expression, then a FixedElement instruction
 * is used instead.)
 * @see FixedElement
 */

public class ComputedElement extends ElementCreator {

    private Expression elementName;
    private Expression namespace = null;
    private NamespaceResolver nsContext;
    //private String defaultNamespace;
    private boolean allowNameAsQName;
    private ItemType itemType;

    /**
     * Create an instruction that creates a new element node
     *
     * @param elementName      Expression that evaluates to produce the name of the
     *                         element node as a lexical QName
     * @param namespace        Expression that evaluates to produce the namespace URI of
     *                         the element node. Set to null if the namespace is to be deduced from the prefix
     *                         of the elementName.
     * @param nsContext        Saved copy of the static namespace context for the instruction.
     *                         Can be set to null if namespace is supplied. This namespace context
     *                         must resolve the null prefix correctly, based on the different rules for
     *                         XSLT and XQuery.
     //* @param defaultNamespace Default namespace to be used if no namespace is supplied and the
     //*                         computed element is a string with no prefix.
     * @param validation       Required validation mode (e.g. STRICT, LAX, SKIP)
     * @param inheritNamespaces true if child elements automatically inherit the namespaces of their parent
     * @param schemaType       The required schema type for the content
     * @param allowQName       True if the elementName expression is allowed to return a QNameValue; false if
     *                         it must return a string (that is, true in XQuery, false in XSLT).
     */
    public ComputedElement(Expression elementName,
                           Expression namespace,
                           NamespaceResolver nsContext,
                           //String defaultNamespace,
                           SchemaType schemaType,
                           int validation,
                           boolean inheritNamespaces,
                           boolean allowQName) {
        this.elementName = elementName;
        this.namespace = namespace;
        this.nsContext = nsContext;
        //this.defaultNamespace = defaultNamespace;
        setSchemaType(schemaType);
        this.validation = validation;
        preservingTypes = schemaType == null && validation == Validation.PRESERVE;
        this.inheritNamespaces = inheritNamespaces;
        allowNameAsQName = allowQName;
        adoptChildExpression(elementName);
        adoptChildExpression(namespace);
    }

    /**
     * Get the expression used to compute the element name
     * @return the expression used to compute the element name
     */

    public Expression getNameExpression() {
        return elementName;
    }

    /**
     * Get the expression used to compute the namespace URI
     * @return  the expression used to compute the namespace URI
     */

    public Expression getNamespaceExpression() {
        return namespace;
    }

    /**
     * Get the namespace resolver that provides the namespace bindings defined in the static context
     * @return the namespace resolver
     */

    public NamespaceResolver getNamespaceResolver() {
        return nsContext;
    }

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        elementName = visitor.simplify(elementName);
        namespace = visitor.simplify(namespace);
        Configuration config = visitor.getConfiguration();
        setLazyConstruction(config.isLazyConstructionMode());
        preservingTypes |= !config.isSchemaAware(Configuration.XML_SCHEMA);

        if (getSchemaType() != null) {
            itemType = new ContentTypeTest(Type.ELEMENT, getSchemaType(), config);
            getSchemaType().analyzeContentExpression(content, Type.ELEMENT, visitor.getStaticContext());
        } else if (validation == Validation.STRIP || !config.isSchemaAware(Configuration.XML_SCHEMA)) {
            itemType = new ContentTypeTest(Type.ELEMENT, Untyped.getInstance(), config);
        } else {
            // paradoxically, we know less about the type if validation="strict" is specified!
            // We know that it won't be untyped, but we have no way of representing that.
            itemType = NodeKindTest.ELEMENT;
        }
        return super.simplify(visitor);
    }

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        elementName = visitor.typeCheck(elementName, contextItemType);
        //adoptChildExpression(elementName);
        RoleLocator role = new RoleLocator(RoleLocator.INSTRUCTION, "element/name", 0);
        //role.setSourceLocator(this);
        if (allowNameAsQName) {
            // Can only happen in XQuery
            elementName = TypeChecker.staticTypeCheck(elementName,
                    SequenceType.SINGLE_ATOMIC, false, role, visitor);
            TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
            ItemType supplied = elementName.getItemType(th);
            if (th.relationship(supplied, BuiltInAtomicType.STRING) == TypeHierarchy.DISJOINT &&
                    th.relationship(supplied, BuiltInAtomicType.UNTYPED_ATOMIC) == TypeHierarchy.DISJOINT &&
                    th.relationship(supplied, BuiltInAtomicType.QNAME) == TypeHierarchy.DISJOINT) {
                XPathException de = new XPathException("The name of a constructed element must be a string, QName, or untypedAtomic");
                de.setErrorCode("XPTY0004");
                de.setIsTypeError(true);
                de.setLocator(this);
                throw de;
            }
        } else {
            elementName = TypeChecker.staticTypeCheck(elementName,
                    SequenceType.SINGLE_STRING, false, role, visitor);
        }
        if (namespace != null) {
            namespace = visitor.typeCheck(namespace, contextItemType);
            //adoptChildExpression(namespace);

            role = new RoleLocator(RoleLocator.INSTRUCTION, "attribute/namespace", 0);
            //role.setSourceLocator(this);
            namespace = TypeChecker.staticTypeCheck(
                    namespace, SequenceType.SINGLE_STRING, false, role, visitor);
        }
        if (Literal.isAtomic(elementName)) {
            // Check we have a valid lexical QName, whose prefix is in scope where necessary
            try {
                AtomicValue val = (AtomicValue)((Literal)elementName).getValue();
                if (val instanceof StringValue) {
                    String[] parts = visitor.getConfiguration().getNameChecker().checkQNameParts(val.getStringValueCS());
                    if (namespace == null) {
                        String prefix = parts[0];
//                        String uri = (prefix.length()==0 ?
//                                defaultNamespace :
//                                getNamespaceResolver().getURIForPrefix(prefix, true));
                        String uri = getNamespaceResolver().getURIForPrefix(prefix, true);
                        if (uri == null) {
                            XPathException se = new XPathException("Prefix " + prefix + " has not been declared");
                            se.setErrorCode("XPST0081");
                            se.setIsStaticError(true);
                            throw se;
                        }
                        namespace = new StringLiteral(uri);
                    }
                }
            } catch (XPathException e) {
                if (e.getErrorCodeLocalPart() == null || e.getErrorCodeLocalPart().equals("FORG0001")) {
                    e.setErrorCode(isXSLT() ? "XTDE0820" : "XQDY0074");
                } else if (e.getErrorCodeLocalPart().equals("XPST0081")) {
                    e.setErrorCode(isXSLT() ? "XTDE0830" : "XQDY0074");
                }
                e.maybeSetLocation(this);
                e.setIsStaticError(true);
                throw e;
            }
        }
        return super.typeCheck(visitor, contextItemType);
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        elementName = visitor.optimize(elementName, contextItemType);
        return super.optimize(visitor, contextItemType);
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        ComputedElement ce = new ComputedElement(
                elementName.copy(), (namespace==null ? null : namespace.copy()),
                getNamespaceResolver(), /*defaultNamespace,*/ getSchemaType(),
                validation, inheritNamespaces, allowNameAsQName);
        ce.setContentExpression(content.copy());
        return ce;
    }

    /**
     * Get the item type of the value returned by this instruction
     *
     * @return the item type
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        if (itemType == null) {
            return super.getItemType(th);
        }
        return itemType;
    }

    public Iterator iterateSubExpressions() {
        ArrayList list = new ArrayList(3);
        list.add(content);
        list.add(elementName);
        if (namespace != null) {
            list.add(namespace);
        }
        return list.iterator();
    }

    /**
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        if (content == original) {
            content = replacement;
            found = true;
        }
        if (elementName == original) {
            elementName = replacement;
            found = true;
        }
        if (namespace == original) {
            namespace = replacement;
            found = true;
        }
        return found;
    }



    /**
     * Offer promotion for subexpressions. The offer will be accepted if the subexpression
     * is not dependent on the factors (e.g. the context item) identified in the PromotionOffer.
     * By default the offer is not accepted - this is appropriate in the case of simple expressions
     * such as constant values and variable references where promotion would give no performance
     * advantage. This method is always called at compile time.
     *
     * @param offer details of the offer, for example the offer to move
     *              expressions that don't depend on the context to an outer level in
     *              the containing expression
     * @throws org.orbeon.saxon.trans.XPathException if any error is detected
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        elementName = doPromotion(elementName, offer);
        if (namespace != null) {
            namespace = doPromotion(namespace, offer);
        }
        super.promoteInst(offer);
    }

    /**
     * Check that any elements and attributes constructed or returned by this expression are acceptable
     * in the content model of a given complex type. It's always OK to say yes, since the check will be
     * repeated at run-time. The process of checking element and attribute constructors against the content
     * model of a complex type also registers the type of content expected of those constructors, so the
     * static validation can continue recursively.
     */

    public void checkPermittedContents(SchemaType parentType, StaticContext env, boolean whole) throws XPathException {
        if (parentType instanceof SimpleType) {
            XPathException err = new XPathException("Elements are not permitted here: the containing element has the simple type " + parentType.getDescription());
            err.setIsTypeError(true);
            err.setLocator(this);
            throw err;
        } else if (((ComplexType)parentType).isSimpleContent()) {
            XPathException err = new XPathException("Elements are not permitted here: the containing element has a complex type with simple content");
            err.setIsTypeError(true);
            err.setLocator(this);
            throw err;
        }
        // NOTE: we could in principle check that if all the elements permitted in the content of the parentType
        // themselves have a simple type (not uncommon, perhaps) then this element must not have element content.
    }


    /**
     * Callback from the superclass ElementCreator to get the nameCode
     * for the element name
     *
     * @param context The evaluation context (not used)
     * @return the name code for the element name
     */

    public int getNameCode(XPathContext context)
            throws XPathException {

        Controller controller = context.getController();
        NamePool pool = controller.getNamePool();

        String prefix;
        String localName;
        String uri = null;

        // name needs to be evaluated at run-time
        AtomicValue nameValue = (AtomicValue)elementName.evaluateItem(context);
        if (nameValue == null) {
            XPathException err1 = new XPathException("Invalid element name (empty sequence)", this);
            err1.setErrorCode((isXSLT() ? "XTDE0820" : "XPTY0004"));
            err1.setXPathContext(context);
            throw dynamicError(this, err1, context);
        }
        //nameValue = nameValue.getPrimitiveValue();
        if (nameValue instanceof StringValue) {  // which includes UntypedAtomic
            // this will always be the case in XSLT
            CharSequence rawName = nameValue.getStringValueCS();
            try {
                String[] parts = controller.getConfiguration().getNameChecker().getQNameParts(rawName);
                prefix = parts[0];
                localName = parts[1];
            } catch (QNameException err) {
                XPathException err1 = new XPathException("Invalid element name. " + err.getMessage(), this);
                err1.setErrorCode((isXSLT() ? "XTDE0820" : "XQDY0074"));
                err1.setXPathContext(context);
                throw dynamicError(this, err1, context);
            }
        } else if (nameValue instanceof QNameValue && allowNameAsQName) {
            // this is allowed in XQuery
            localName = ((QNameValue)nameValue).getLocalName();
            uri = ((QNameValue)nameValue).getNamespaceURI();
            if (uri == null) {
                uri = "";
            }
            prefix = ((QNameValue)nameValue).getPrefix();
        } else {
            XPathException err = new XPathException("Computed element name has incorrect type");
            err.setErrorCode((isXSLT() ? "XTDE0820" : "XPTY0004"));
            err.setIsTypeError(true);
            err.setXPathContext(context);
            throw dynamicError(this, err, context);
        }

        if (namespace == null && uri == null) {
//            if (prefix.length() == 0) {
//                uri = defaultNamespace;
//            } else {
                uri = nsContext.getURIForPrefix(prefix, true);
                if (uri == null) {
                    XPathException err = new XPathException("Undeclared prefix in element name: " + prefix, this);
                    err.setErrorCode((isXSLT() ? "XTDE0830" : "XQDY0074"));
                    err.setXPathContext(context);
                    throw dynamicError(this, err, context);
                }
//            }
        } else {
            if (uri == null) {
                if (namespace instanceof StringLiteral) {
                    uri = ((StringLiteral)namespace).getStringValue();
                } else {
                    uri = namespace.evaluateAsString(context).toString();
                    if (!AnyURIValue.isValidURI(uri)) {
                        XPathException de = new XPathException("The value of the namespace attribute must be a valid URI");
                        de.setErrorCode("XTDE0835");
                        de.setXPathContext(context);
                        de.setLocator(this);
                        throw de;
                    }
                }
            }
            if (uri.length() == 0) {
                // there is a special rule for this case in the specification;
                // we force the element to go in the null namespace
                prefix = "";
            }
            if (prefix.equals("xmlns")) {
                // this isn't a legal prefix so we mustn't use it
                prefix = "x-xmlns";
            }
        }

        return pool.allocate(prefix, uri, localName);

    }

    public String getNewBaseURI(XPathContext context) {
        return getBaseURI();
    }

    /**
     * Callback to output namespace nodes for the new element.
     *
     * @param context The execution context
     * @param out     the Receiver where the namespace nodes are to be written
     * @throws XPathException
     */
    protected void outputNamespaceNodes(XPathContext context, Receiver out)
            throws XPathException {
        // do nothing
    }


    /**
     * Get the name of this instruction for diagnostic and tracing purposes
     */

    public int getInstructionNameCode() {
        return StandardNames.XSL_ELEMENT;
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("computedElement");
        out.emitAttribute("validation", Validation.toString(validation));
        if (getSchemaType() != null) {
            out.emitAttribute("type", getSchemaType().getDescription());
        }
        out.startSubsidiaryElement("name");
        elementName.explain(out);
        out.endSubsidiaryElement();
        if (namespace != null) {
            out.startSubsidiaryElement("namespace");
            namespace.explain(out);
            out.endSubsidiaryElement();
        }
        out.startSubsidiaryElement("content");
        content.explain(out);
        out.endSubsidiaryElement();
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
