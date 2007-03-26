package org.orbeon.saxon.instruct;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.NamespaceResolver;
import org.orbeon.saxon.om.QNameException;
import org.orbeon.saxon.om.Validation;
import org.orbeon.saxon.pattern.ContentTypeTest;
import org.orbeon.saxon.pattern.NodeKindTest;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.StaticError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.value.*;

import java.io.PrintStream;
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
     *                         Can be set to null if namespace is supplied.
     * @param schemaType       The required schema type for the content
     * @param allowQName
     */
    public ComputedElement(Expression elementName,
                           Expression namespace,
                           NamespaceResolver nsContext,
                           SchemaType schemaType,
                           int validation,
                           boolean inheritNamespaces,
                           boolean allowQName) {
        this.elementName = elementName;
        this.namespace = namespace;
        this.nsContext = nsContext;
        setSchemaType(schemaType);
        this.validation = validation;
        this.validating = schemaType != null || validation != Validation.PRESERVE;
        this.inheritNamespaces = inheritNamespaces;
        this.allowNameAsQName = allowQName;
        adoptChildExpression(elementName);
        adoptChildExpression(namespace);
    }

    public Expression simplify(StaticContext env) throws XPathException {
        elementName = elementName.simplify(env);
        if (namespace != null) {
            namespace = namespace.simplify(env);
        }
        Configuration config = env.getConfiguration();
        setLazyConstruction(config.isLazyConstructionMode());
        validating &= config.isSchemaAware(Configuration.XML_SCHEMA);

        if (getSchemaType() != null) {
            itemType = new ContentTypeTest(Type.ELEMENT, getSchemaType(), config);
            getSchemaType().analyzeContentExpression(content, Type.ELEMENT, env);
        } else if (validation == Validation.STRIP || !config.isSchemaAware(Configuration.XML_SCHEMA)) {
            itemType = new ContentTypeTest(Type.ELEMENT,
                    BuiltInSchemaFactory.getSchemaType(StandardNames.XDT_UNTYPED),
                    config);
        } else {
            // paradoxically, we know less about the type if validation="strict" is specified!
            // We know that it won't be untyped, but we have no way of representing that.
            itemType = NodeKindTest.ELEMENT;
        }
        return super.simplify(env);
    }

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        elementName = elementName.typeCheck(env, contextItemType);
        //adoptChildExpression(elementName);
        RoleLocator role = new RoleLocator(RoleLocator.INSTRUCTION, "element/name", 0, null);
        role.setSourceLocator(this);
        if (allowNameAsQName) {
            // Can only happen in XQuery
            elementName = TypeChecker.staticTypeCheck(elementName,
                    SequenceType.SINGLE_ATOMIC, false, role, env);
        } else {
            elementName = TypeChecker.staticTypeCheck(elementName,
                    SequenceType.SINGLE_STRING, false, role, env);
        }
        if (namespace != null) {
            namespace = namespace.typeCheck(env, contextItemType);
            //adoptChildExpression(namespace);

            role = new RoleLocator(RoleLocator.INSTRUCTION, "attribute/namespace", 0, null);
            role.setSourceLocator(this);
            namespace = TypeChecker.staticTypeCheck(
                    namespace, SequenceType.SINGLE_STRING, false, role, env);
        }
        return super.typeCheck(env, contextItemType);
    }

    /**
     * Get the item type of the value returned by this instruction
     *
     * @return the item type
     * @param th
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
            StaticError err = new StaticError(
                    "Elements are not permitted here: the containing element has the simple type " + parentType.getDescription());
            err.setIsTypeError(true);
            err.setLocator(this);
            throw err;
        } else if (((ComplexType)parentType).isSimpleContent()) {
            StaticError err = new StaticError(
                    "Elements are not permitted here: the containing element has a complex type with simple content");
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
            throws XPathException, XPathException {

        Controller controller = context.getController();
        NamePool pool = controller.getNamePool();

        String prefix;
        String localName;
        String uri;

        // name needs to be evaluated at run-time
        AtomicValue nameValue = (AtomicValue)elementName.evaluateItem(context);
        if (nameValue == null) {
            DynamicError err1 = new DynamicError("Invalid element name (empty sequence)", this);
            err1.setErrorCode((isXSLT() ? "XTDE0820" : "XPTY0004"));
            err1.setXPathContext(context);
            throw dynamicError(this, err1, context);
        }
        nameValue = nameValue.getPrimitiveValue();
        if (nameValue instanceof StringValue) {  // which includes UntypedAtomic
            // this will always be the case in XSLT
            CharSequence rawName = nameValue.getStringValueCS();
            try {
                String[] parts = controller.getConfiguration().getNameChecker().getQNameParts(rawName);
                prefix = parts[0];
                localName = parts[1];
            } catch (QNameException err) {
                DynamicError err1 = new DynamicError("Invalid element name. " + err.getMessage(), this);
                err1.setErrorCode((isXSLT() ? "XTDE0820" : "XQDY0074"));
                err1.setXPathContext(context);
                throw dynamicError(this, err1, context);
            }
        } else if (nameValue instanceof QNameValue && allowNameAsQName) {
            // this is allowed in XQuery
            localName = ((QNameValue)nameValue).getLocalName();
            uri = ((QNameValue)nameValue).getNamespaceURI();
            namespace = new StringValue(uri);
            prefix = ((QNameValue)nameValue).getPrefix();
        } else {
            DynamicError err = new DynamicError("Computed element name has incorrect type");
            err.setErrorCode((isXSLT() ? "XTDE0820" : "XPTY0004"));
            err.setIsTypeError(true);
            err.setXPathContext(context);
            throw dynamicError(this, err, context);
        }

        if (namespace == null) {
            uri = nsContext.getURIForPrefix(prefix, true);
            if (uri == null) {
                DynamicError err = new DynamicError("Undeclared prefix in element name: " + prefix, this);
                err.setErrorCode((isXSLT() ? "XTDE0830" : "XQDY0074"));
                err.setXPathContext(context);
                throw dynamicError(this, err, context);
            }

        } else {
            if (namespace instanceof StringValue) {
                uri = ((StringValue)namespace).getStringValue();
            } else {
                uri = namespace.evaluateAsString(context);
                if (!AnyURIValue.isValidURI(uri)) {
                    DynamicError de = new DynamicError(
                            "The value of the namespace attribute must be a valid URI");
                    de.setErrorCode("XTDE0835");
                    de.setXPathContext(context);
                    de.setLocator(this);
                    throw de;
                }
            }
            if (uri.equals("")) {
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
     * Display this instruction as an expression, for diagnostics
     */

    public void display(int level, PrintStream out, Configuration config) {
        out.println(ExpressionTool.indent(level) + "element ");
        out.println(ExpressionTool.indent(level + 1) + "name");
        elementName.display(level + 2, out, config);
        out.println(ExpressionTool.indent(level + 1) + "content");
        content.display(level + 1, out, config);
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
