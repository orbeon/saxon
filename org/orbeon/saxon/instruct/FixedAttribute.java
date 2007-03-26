package org.orbeon.saxon.instruct;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.Err;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.functions.SystemFunction;
import org.orbeon.saxon.event.ReceiverOptions;
import org.orbeon.saxon.event.SequenceReceiver;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.Orphan;
import org.orbeon.saxon.om.Validation;
import org.orbeon.saxon.pattern.NodeKindTest;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.trace.InstructionInfo;
import org.orbeon.saxon.trace.Location;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.StaticError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.StringValue;

import java.io.PrintStream;

/**
 * An instruction derived from an xsl:attribute element in stylesheet, or from
 * an attribute constructor in XQuery. This version deals only with attributes
 * whose name is known at compile time. It is also used for attributes of
 * literal result elements. The value of the attribute is in general computed
 * at run-time.
*/

public final class FixedAttribute extends SimpleNodeConstructor {

    private int nameCode;
    private SimpleType schemaType;
    private int annotation;
    private int options;
    private int validationAction;

    /**
    * Construct an Attribute instruction
    * @param nameCode Represents the attribute name
    * @param annotation Integer code identifying the type named in the <code>type</code> attribute
    * of the instruction - zero if the attribute was not present
    */

    public FixedAttribute (  int nameCode,
                             int validationAction,
                             SimpleType schemaType,
                             int annotation ) {
        this.nameCode = nameCode;
        this.schemaType = schemaType;
        if (annotation == -1) {
            this.annotation = StandardNames.XDT_UNTYPED_ATOMIC;
        } else {
            this.annotation = annotation;
        }
        this.validationAction = validationAction;
        this.options = 0;
    }

    /**
     * Get the name of this instruction (return 'xsl:attribute')
     */

    public int getInstructionNameCode() {
        return StandardNames.XSL_ATTRIBUTE;
    }
    /**
     * Indicate that two attributes with the same name are not acceptable.
     * (This option is set in XQuery, but not in XSLT)
     */

    public void setRejectDuplicates() {
        this.options |= ReceiverOptions.REJECT_DUPLICATES;
    }

    /**
     * Indicate that the attribute value contains no special characters that
     * might need escaping
     */

    public void setNoSpecialChars() {
        this.options |= ReceiverOptions.NO_SPECIAL_CHARS;
    }

    /**
     * Set the expression defining the value of the attribute. If this is a constant, and if
     * validation against a schema type was requested, the validation is done immediately.
     * @param select The expression defining the content of the attribute
     * @param config
     * @throws StaticError if the expression is a constant, and validation is requested, and
     * the constant doesn't match the required type.
     */
    public void setSelect(Expression select, Configuration config) throws StaticError {
        super.setSelect(select, config);

        // Attempt early validation if possible
        if (select instanceof AtomicValue && schemaType != null && !schemaType.isNamespaceSensitive()) {
            CharSequence value = ((AtomicValue)select).getStringValueCS();
            XPathException err = schemaType.validateContent(
                    value, DummyNamespaceResolver.getInstance(), config.getNameChecker());
            if (err != null) {
                StaticError se = new StaticError("Attribute value " + Err.wrap(value, Err.VALUE) +
                                               " does not the match the required type " +
                                               schemaType.getDescription() + ". " +
                                               err.getMessage());
                se.setErrorCode("XTTE1540");
                throw se;
            }
        }

        // If value is fixed, test whether there are any special characters that might need to be
        // escaped when the time comes for serialization
        if (select instanceof StringValue) {
            boolean special = false;
            CharSequence val = ((StringValue)select).getStringValueCS();
            for (int k=0; k<val.length(); k++) {
                char c = val.charAt(k);
                if ((int)c<33 || (int)c>126 ||
                         c=='<' || c=='>' || c=='&' || c=='\"') {
                    special = true;
                    break;
                 }
            }
            if (!special) {
                this.options |= ReceiverOptions.NO_SPECIAL_CHARS;
            }
        }

        // If attribute name is xml:id, add whitespace normalization
        if ((nameCode & NamePool.FP_MASK) == StandardNames.XML_ID) {
            Expression[] args = {select};
            FunctionCall fn = SystemFunction.makeSystemFunction("normalize-space", 1, config.getNamePool());
            fn.setArguments(args);
            select = fn;
            super.setSelect(select, config);
        }
    }

    public InstructionInfo getInstructionInfo() {
        InstructionDetails details = (InstructionDetails)super.getInstructionInfo();
        details.setConstructType(Location.LITERAL_RESULT_ATTRIBUTE);
        details.setObjectNameCode(nameCode);
        return details;
    }

    public ItemType getItemType(TypeHierarchy th) {
        return NodeKindTest.ATTRIBUTE;
    }

    public int getCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    public int getValidationAction() {
        return validationAction;
    }


    public void localTypeCheck(StaticContext env, ItemType contextItemType) {
    }

    public int evaluateNameCode(XPathContext context)  {
        return nameCode;
    }

//    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
//        return super.optimize(opt, env, contextItemType);
//    }

    /**
     * Check that any elements and attributes constructed or returned by this expression are acceptable
     * in the content model of a given complex type. It's always OK to say yes, since the check will be
     * repeated at run-time. The process of checking element and attribute constructors against the content
     * model of a complex type also registers the type of content expected of those constructors, so the
     * static validation can continue recursively.
     */

    public void checkPermittedContents(SchemaType parentType, StaticContext env, boolean whole) throws XPathException {
        int fp = nameCode & NamePool.FP_MASK;
        if (fp == StandardNames.XSI_TYPE ||
                fp == StandardNames.XSI_SCHEMA_LOCATION ||
                fp == StandardNames.XSI_NIL ||
                fp == StandardNames.XSI_NO_NAMESPACE_SCHEMA_LOCATION) {
            return;
        }
        if (parentType instanceof SimpleType) {
            StaticError err = new StaticError("Attribute " + env.getNamePool().getDisplayName(nameCode) +
                    " is not permitted in the content model of the simple type " + parentType.getDescription());
            err.setIsTypeError(true);
            err.setLocator(this);
            if (getHostLanguage() == Configuration.XSLT) {
                err.setErrorCode("XTTE1510");
            } else {
                err.setErrorCode("XQDY0027");
            }
            throw err;
        }
        SchemaType type;
        try {
            type = ((ComplexType)parentType).getAttributeUseType(fp);
        } catch (SchemaException e) {
            throw new StaticError(e);
        }
        if (type == null) {
            StaticError err = new StaticError("Attribute " + env.getNamePool().getDisplayName(nameCode) +
                    " is not permitted in the content model of the complex type " + parentType.getDescription());
            err.setIsTypeError(true);
            err.setLocator(this);
            if (getHostLanguage() == Configuration.XSLT) {
                err.setErrorCode("XTTE1510");
            } else {
                err.setErrorCode("XQDY0027");
            }
            throw err;
        }
        if (type instanceof AnyType) {
            return;
        }

        try {
            select.checkPermittedContents(type, env, true);
            // TODO: does this allow for the fact that the content will be atomized?
        } catch (XPathException e) {
            if (e.getLocator() == null || e.getLocator() == e) {
                e.setLocator(this);
            }
            throw e;
        }
    }

    /**
    * Process this instruction
    * @param context the dynamic context of the transformation
    * @return a TailCall to be executed by the caller, always null for this instruction
    */

    public TailCall processLeavingTail(XPathContext context) throws XPathException
    {
        Controller controller = context.getController();
        SequenceReceiver out = context.getReceiver();
        int opt = options;
        int ann = annotation;

    	// we may need to change the namespace prefix if the one we chose is
    	// already in use with a different namespace URI: this is done behind the scenes
    	// by the Outputter

        CharSequence value = expandChildren(context);  
        if (schemaType != null) {
            // test whether the value actually conforms to the given type
            XPathException err = schemaType.validateContent(
                    value, DummyNamespaceResolver.getInstance(), context.getConfiguration().getNameChecker());
            if (err != null) {
                ValidationException verr = new ValidationException(
                        "Attribute value " + Err.wrap(value, Err.VALUE) +
                        " does not the match the required type " +
                        schemaType.getDescription() + ". " + err.getMessage());
                verr.setErrorCode("XTTE1540");
                verr.setLocator(this);
                throw verr;
            }
        } else if (validationAction==Validation.STRICT || validationAction==Validation.LAX) {
            try {
                ann = controller.getConfiguration().validateAttribute(
                        nameCode, value, validationAction);
            } catch (ValidationException e) {
                DynamicError err = DynamicError.makeDynamicError(e);
                String errorCode = e.getErrorCodeLocalPart();
                if (errorCode == null) {
                    errorCode = (validationAction==Validation.STRICT ? "XTTE1510" : "XTTE1515");
                }
                err.setErrorCode(errorCode);
                err.setXPathContext(context);
                err.setLocator(this);
                err.setIsTypeError(true);
                throw err;
            }
        }
        try {
            out.attribute(nameCode, ann, value, locationId, opt);
        } catch (XPathException err) {
            throw dynamicError(this, err, context);
        }

        return null;
    }

    public Item evaluateItem(XPathContext context) throws XPathException {
        Orphan o = (Orphan)super.evaluateItem(context);
        if (schemaType != null) {
            XPathException err = schemaType.validateContent(
                    o.getStringValueCS(), DummyNamespaceResolver.getInstance(), context.getConfiguration().getNameChecker());
            if (err != null) {
                throw new ValidationException("Attribute value " + Err.wrap(o.getStringValueCS(), Err.VALUE) +
                                           " does not the match the required type " +
                                           schemaType.getDescription() + ". " +
                                           err.getMessage());
            }
            o.setTypeAnnotation(schemaType.getFingerprint());
            if (schemaType.isNamespaceSensitive()) {
                throw new DynamicError("Cannot validate a parentless attribute whose content is namespace-sensitive");
            }
        } else if (validationAction==Validation.STRICT || validationAction==Validation.LAX) {
            try {
                int ann = context.getController().getConfiguration().validateAttribute(
                        nameCode, o.getStringValueCS(), validationAction);
                o.setTypeAnnotation(ann);
            } catch (ValidationException e) {
                DynamicError err = DynamicError.makeDynamicError(e);
                err.setErrorCode(e.getErrorCodeLocalPart());
                err.setXPathContext(context);
                err.setLocator(this);
                err.setIsTypeError(true);
                throw err;
            }
        }

        return o;
    }

     /**
     * Display this instruction as an expression, for diagnostics
     */

    public void display(int level, PrintStream out, Configuration config) {
        out.println(ExpressionTool.indent(level) + "attribute ");
        out.println(ExpressionTool.indent(level+1) + "name " +
                (config==null ? nameCode+"" : config.getNamePool().getDisplayName(nameCode)));
        super.display(level+1, out, config);
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
