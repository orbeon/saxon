package org.orbeon.saxon.instruct;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.expr.ExpressionTool;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.trace.InstructionInfo;
import org.orbeon.saxon.trace.Location;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.xpath.XPathException;

import java.io.PrintStream;


/**
* An instruction that creates an element node whose name is known statically.
 * Used for literal results elements in XSLT, for direct element constructors
 * in XQuery, and for xsl:element in cases where the name and namespace are
 * known statically.
*/

public class FixedElement extends ElementCreator {

    private int nameCode;
    protected int[] namespaceCodes = null;

    /**
     * Create an instruction that creates a new element node
     * @param nameCode Represents the name of the element node
     * @param namespaceCodes List of namespaces to be added to the element node.
     *                       May be null if none are required.
     * @param useAttributeSets Array of attribute sets to be expanded. May be null
     *                       if none are required.
     * @param schemaType Type annotation for the new element node
     */
    public FixedElement( int nameCode,
                         int[] namespaceCodes,
                         AttributeSet[] useAttributeSets,
                         boolean inheritNamespaces,
                         SchemaType schemaType,
                         int validation) {
        this.nameCode = nameCode;
        this.namespaceCodes = namespaceCodes;
        this.useAttributeSets = useAttributeSets;
        this.inheritNamespaces = inheritNamespaces;
        this.schemaType = schemaType;
        this.validation = validation;
    }

    public InstructionInfo getInstructionInfo() {
        InstructionDetails details = (InstructionDetails)super.getInstructionInfo();
        details.setConstructType(Location.LITERAL_RESULT_ELEMENT);
        details.setObjectNameCode(nameCode);
        return details;
    }

    /**
     * Callback from the superclass ElementCreator to get the nameCode
     * for the element name
     * @param context The evaluation context (not used)
     * @return the name code for the element name
     */

    protected int getNameCode(XPathContext context) {
        return nameCode;
    }

    protected void outputNamespaceNodes(XPathContext context, Receiver out)
    throws XPathException {
        if (namespaceCodes != null) {
            for (int i=0; i<namespaceCodes.length; i++) {
                out.namespace(namespaceCodes[i], 0);
            }
        }
    }

    /**
     * Display this instruction as an expression, for diagnostics
     */

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "element ");
        out.println(ExpressionTool.indent(level+1) + "name " +
                (pool==null ? nameCode+"" : pool.getDisplayName(nameCode)));
        if (children==null || children.length==0) {
            out.println(ExpressionTool.indent(level+1) + "empty content");
        } else {
            InstructionWithChildren.displayChildren(children, level+1, pool, out);
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
