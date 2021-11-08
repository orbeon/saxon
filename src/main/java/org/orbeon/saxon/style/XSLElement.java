package org.orbeon.saxon.style;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.trans.Err;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.instruct.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.value.AnyURIValue;
import org.orbeon.saxon.value.EmptySequence;
import org.orbeon.saxon.value.Whitespace;


/**
 * An xsl:element element in the stylesheet. <br>
 */

public class XSLElement extends StyleElement {

    private Expression elementName;
    private Expression namespace = null;
    private String use;
    private AttributeSet[] attributeSets = null;
    private int validation;
    private SchemaType schemaType = null;
    private boolean inheritNamespaces = true;

    /**
     * Determine whether this node is an instruction.
     *
     * @return true - it is an instruction
     */

    public boolean isInstruction() {
        return true;
    }

    /**
     * Determine whether this type of element is allowed to contain a template-body
     *
     * @return true: yes, it may contain a template-body
     */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    public void prepareAttributes() throws XPathException {

        AttributeCollection atts = getAttributeList();

        String nameAtt = null;
        String namespaceAtt = null;
        String validationAtt = null;
        String typeAtt = null;
        String inheritAtt = null;

        for (int a = 0; a < atts.getLength(); a++) {
            int nc = atts.getNameCode(a);
            String f = getNamePool().getClarkName(nc);
            if (f.equals(StandardNames.NAME)) {
                nameAtt = Whitespace.trim(atts.getValue(a));
            } else if (f.equals(StandardNames.NAMESPACE)) {
                namespaceAtt = atts.getValue(a);
            } else if (f.equals(StandardNames.VALIDATION)) {
                validationAtt = Whitespace.trim(atts.getValue(a));
            } else if (f.equals(StandardNames.TYPE)) {
                typeAtt = Whitespace.trim(atts.getValue(a));
            } else if (f.equals(StandardNames.INHERIT_NAMESPACES)) {
                inheritAtt = Whitespace.trim(atts.getValue(a));
            } else if (f.equals(StandardNames.USE_ATTRIBUTE_SETS)) {
                use = atts.getValue(a);
            } else {
                checkUnknownAttribute(nc);
            }
        }

        if (nameAtt == null) {
            reportAbsence("name");
        } else {
            elementName = makeAttributeValueTemplate(nameAtt);
            if (elementName instanceof StringLiteral) {
                if (!getConfiguration().getNameChecker().isQName(((StringLiteral)elementName).getStringValue())) {
                    compileError("Element name " +
                            Err.wrap(((StringLiteral)elementName).getStringValue(), Err.ELEMENT) +
                            " is not a valid QName", "XTDE0820");
                    // to prevent duplicate error messages:
                    elementName = new StringLiteral("saxon-error-element");
                }
            }
        }

        if (namespaceAtt != null) {
            namespace = makeAttributeValueTemplate(namespaceAtt);
            if (namespace instanceof StringLiteral) {
                if (!AnyURIValue.isValidURI(((StringLiteral)namespace).getStringValue())) {
                    compileError("The value of the namespace attribute must be a valid URI", "XTDE0835");
                }
            }
        }

        if (validationAtt != null) {
            validation = Validation.getCode(validationAtt);
            if (validation != Validation.STRIP && !getConfiguration().isSchemaAware(Configuration.XSLT)) {
                compileError("To perform validation, a schema-aware XSLT processor is needed", "XTSE1660");
            }
            if (validation == Validation.INVALID) {
                compileError("Invalid value for @validation attribute. " +
                        "Permitted values are (strict, lax, preserve, strip)", "XTSE0020");
            }
        } else {
            validation = getContainingStylesheet().getDefaultValidation();
        }

        if (typeAtt != null) {
            if (!getConfiguration().isSchemaAware(Configuration.XSLT)) {
                compileError("The @type attribute is available only with a schema-aware XSLT processor", "XTSE1660");
            }
            schemaType = getSchemaType(typeAtt);
            validation = Validation.BY_TYPE;
        }

        if (typeAtt != null && validationAtt != null) {
            compileError("The @validation and @type attributes are mutually exclusive", "XTSE1505");
        }

        if (inheritAtt != null) {
            if (inheritAtt.equals("yes")) {
                inheritNamespaces = true;
            } else if (inheritAtt.equals("no")) {
                inheritNamespaces = false;
            } else {
                compileError("The @inherit-namespaces attribute has permitted values (yes, no)", "XTSE0020");
            }
        }
    }

    public void validate() throws XPathException {
        if (use != null) {
            attributeSets = getAttributeSets(use, null);        // find any referenced attribute sets
        }
        elementName = typeCheck("name", elementName);
        namespace = typeCheck("namespace", namespace);
    }

    public Expression compile(Executable exec) throws XPathException {

        NamespaceResolver nsContext = null;

        // deal specially with the case where the element name is known statically

        if (elementName instanceof StringLiteral) {
            CharSequence qName = ((StringLiteral)elementName).getStringValue();

            String[] parts;
            try {
                parts = getConfiguration().getNameChecker().getQNameParts(qName);
            } catch (QNameException e) {
                compileError("Invalid element name: " + qName, "XTDE0820");
                return null;
            }

            String nsuri = null;
            if (namespace instanceof StringLiteral) {
                nsuri = ((StringLiteral)namespace).getStringValue();
                if (nsuri.length() == 0) {
                    parts[0] = "";
                }
            } else if (namespace == null) {
                nsuri = getURIForPrefix(parts[0], true);
                if (nsuri == null) {
                    undeclaredNamespaceError(parts[0], "XTDE0830");
                }
            }
            if (nsuri != null) {
                // Local name and namespace are both known statically: generate a FixedElement instruction
                int nameCode = getNamePool().allocate(parts[0], nsuri, parts[1]);
                FixedElement inst = new FixedElement(nameCode,
                        null,
                        inheritNamespaces,
                        schemaType,
                        validation);
                inst.setBaseURI(getBaseURI());
                Expression content = compileSequenceConstructor(exec, iterateAxis(Axis.CHILD), true);

                if (attributeSets != null) {
                    UseAttributeSets use = new UseAttributeSets(attributeSets);
                    if (content == null) {
                        content = use;
                    } else {
                        content = Block.makeBlock(use, content);
                        content.setLocationId(
                                    allocateLocationId(getSystemId(), getLineNumber()));
                    }
                }
                if (content == null) {
                    content = new Literal(EmptySequence.getInstance());
                }
                inst.setContentExpression(content);
                return inst;
            }
        } else {
            // if the namespace URI must be deduced at run-time from the element name
            // prefix, we need to save the namespace context of the instruction

            if (namespace == null) {
                nsContext = makeNamespaceContext();
            }
        }

        ComputedElement inst = new ComputedElement(elementName,
                namespace,
                nsContext,
                //(nsContext==null ? null : nsContext.getURIForPrefix("", true)),
                schemaType,
                validation,
                inheritNamespaces,
                false);
        Expression content = compileSequenceConstructor(exec, iterateAxis(Axis.CHILD), true);
        if (attributeSets != null) {
            UseAttributeSets use = new UseAttributeSets(attributeSets);
            if (content == null) {
                content = use;
            } else {
                content = Block.makeBlock(use, content);
                content.setLocationId(
                            allocateLocationId(getSystemId(), getLineNumber()));
            }
        }
        if (content == null) {
            content = new Literal(EmptySequence.getInstance());
        }
        inst.setContentExpression(content);
        return inst;
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
