package org.orbeon.saxon.style;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.instruct.Block;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.instruct.SlotManager;
import org.orbeon.saxon.om.NamespaceException;
import org.orbeon.saxon.pattern.Pattern;
import org.orbeon.saxon.trans.KeyDefinition;
import org.orbeon.saxon.trans.KeyManager;
import org.orbeon.saxon.tree.AttributeCollection;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.SequenceType;
import org.orbeon.saxon.xpath.XPathException;
import org.orbeon.saxon.Err;

import javax.xml.transform.TransformerConfigurationException;
import java.text.Collator;
import java.util.Comparator;

/**
* Handler for xsl:key elements in stylesheet. <br>
*/

public class XSLKey extends StyleElement implements StylesheetProcedure {

    private Pattern match;
    private Expression use;
    private String collationName;
    SlotManager stackFrameMap;
                // needed if variables are used
    /**
      * Determine whether this type of element is allowed to contain a sequence constructor
      * @return true: yes, it may contain a sequence constructor
      */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    /**
     * Get the Procedure object that looks after any local variables declared in the content constructor
     */

    public SlotManager getSlotManager() {
        return stackFrameMap;
    }

    public void prepareAttributes() throws TransformerConfigurationException {

        String nameAtt = null;
        String matchAtt = null;
        String useAtt = null;

		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f==StandardNames.NAME) {
        		nameAtt = atts.getValue(a).trim();
        	} else if (f==StandardNames.USE) {
        		useAtt = atts.getValue(a);
        	} else if (f==StandardNames.MATCH) {
        		matchAtt = atts.getValue(a);
        	} else if (f==StandardNames.COLLATION) {
        		collationName = atts.getValue(a).trim();
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (nameAtt==null) {
            reportAbsence("name");
            return;
        }
        try {
            setObjectNameCode(makeNameCode(nameAtt.trim()));
        } catch (NamespaceException err) {
            compileError(err.getMessage());
        } catch (XPathException err) {
            compileError(err.getMessage());
        }

        if (matchAtt==null) {
            reportAbsence("match");
            matchAtt = "*";
        }
        match = makePattern(matchAtt);

        if (useAtt!=null) {
            use = makeExpression(useAtt);
        }
    }

    public void validate() throws TransformerConfigurationException {

        stackFrameMap = getConfiguration().makeSlotManager();
        checkTopLevel(null);
        if (use!=null) {
            // the value can be supplied as a content constructor in place of a use expression
            if (hasChildNodes()) {
                compileError("An xsl:key element with a use attribute must be empty", "XT1205");
            }
            try {
                RoleLocator role =
                    new RoleLocator(RoleLocator.INSTRUCTION, "xsl:key/use", 0);
                use = TypeChecker.staticTypeCheck(
                                use,
                                new SequenceType(Type.ANY_ATOMIC_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE),
                                false, role, getStaticContext());
            } catch (XPathException err) {
                compileError(err);
            }
        } else {
            if (!hasChildNodes()) {
                compileError("An xsl:key element must either have a use attribute or have content", "XT1205");
            }
        }
        use = typeCheck("use", use);
        match = typeCheck("match", match);

     }

    public Expression compile(Executable exec) throws TransformerConfigurationException {

        Collator collator = null;
        if (collationName != null) {
            Comparator comp = getPrincipalStylesheet().findCollation(collationName);
            if (comp==null) {
                 compileError("The collation name " + Err.wrap(collationName) + " is not recognized", "XT1210");
            }
            if (!(comp instanceof Collator)) {
                compileError("The collation used for xsl:key must be a java.text.Collator", "XT1210");
            }
            collator = (Collator)comp;
        }

        if (use==null) {
            Block body = new Block();
            compileChildren(exec, body, true);
            try {
                use = new Atomizer(body.simplify(getStaticContext()));
            } catch (XPathException e) {
                compileError(e);
            }

            try {
                RoleLocator role =
                    new RoleLocator(RoleLocator.INSTRUCTION, "xsl:key/use", 0);
                use = TypeChecker.staticTypeCheck(
                                use,
                                new SequenceType(Type.ANY_ATOMIC_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE),
                                false, role, getStaticContext());

            } catch (XPathException err) {
                compileError(err);
            }
        }

        allocateSlots(use);


        KeyManager km = getPrincipalStylesheet().getKeyManager();
        KeyDefinition keydef = new KeyDefinition(match, use, collationName, collator);
        keydef.setStackFrameMap(stackFrameMap);
        keydef.setLocation(getSystemId(), getLineNumber());
        keydef.setExecutable(getExecutable());
        try {
            km.setKeyDefinition(getObjectFingerprint(), keydef);
        } catch (TransformerConfigurationException err) {
            compileError(err);
        }
        return null;
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
