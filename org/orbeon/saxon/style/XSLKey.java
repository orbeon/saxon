package org.orbeon.saxon.style;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.trans.Err;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.instruct.SlotManager;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.Pattern;
import org.orbeon.saxon.sort.CodepointCollator;
import org.orbeon.saxon.sort.StringCollator;
import org.orbeon.saxon.trans.KeyDefinition;
import org.orbeon.saxon.trans.KeyManager;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.BuiltInAtomicType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.SequenceType;
import org.orbeon.saxon.value.Whitespace;

import java.net.URI;
import java.net.URISyntaxException;

/**
* Handler for xsl:key elements in stylesheet. <br>
*/

public class XSLKey extends StyleElement implements StylesheetProcedure {

    private Pattern match;
    private Expression use;
    private String collationName;
    private StructuredQName keyName;
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

    public void prepareAttributes() throws XPathException {

        String nameAtt = null;
        String matchAtt = null;
        String useAtt = null;

		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f.equals(StandardNames.NAME)) {
        		nameAtt = Whitespace.trim(atts.getValue(a)) ;
        	} else if (f.equals(StandardNames.USE)) {
        		useAtt = atts.getValue(a);
        	} else if (f.equals(StandardNames.MATCH)) {
        		matchAtt = atts.getValue(a);
        	} else if (f.equals(StandardNames.COLLATION)) {
        		collationName = Whitespace.trim(atts.getValue(a)) ;
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (nameAtt==null) {
            reportAbsence("name");
            return;
        }
        try {
            keyName = makeQName(nameAtt);
            setObjectName(keyName);
        } catch (NamespaceException err) {
            compileError(err.getMessage(), "XTSE0280");
        } catch (XPathException err) {
            compileError(err);
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

    public StructuredQName getKeyName() {
    	//We use null to mean "not yet evaluated"
        try {
        	if (getObjectName()==null) {
        		// allow for forwards references
        		String nameAtt = getAttributeValue(StandardNames.NAME);
        		if (nameAtt != null) {
        			setObjectName(makeQName(nameAtt));
                }
            }
            return getObjectName();
        } catch (NamespaceException err) {
            return null;          // the errors will be picked up later
        } catch (XPathException err) {
            return null;
        }
    }

    public void validate() throws XPathException {

        stackFrameMap = getConfiguration().makeSlotManager();
        checkTopLevel(null);
        if (use!=null) {
            // the value can be supplied as a content constructor in place of a use expression
            if (hasChildNodes()) {
                compileError("An xsl:key element with a @use attribute must be empty", "XTSE1205");
            }
            try {
                RoleLocator role =
                    new RoleLocator(RoleLocator.INSTRUCTION, "xsl:key/use", 0);
                //role.setSourceLocator(new ExpressionLocation(this));
                use = TypeChecker.staticTypeCheck(
                                use,
                                SequenceType.makeSequenceType(BuiltInAtomicType.ANY_ATOMIC, StaticProperty.ALLOWS_ZERO_OR_MORE),
                                false, role, makeExpressionVisitor());
            } catch (XPathException err) {
                compileError(err);
            }
        } else {
            if (!hasChildNodes()) {
                compileError("An xsl:key element must either have a @use attribute or have content", "XTSE1205");
            }
        }
        use = typeCheck("use", use);
        match = typeCheck("match", match);

        // Do a further check that the use expression makes sense in the context of the match pattern
        if (use != null) {
            use = makeExpressionVisitor().typeCheck(use, match.getNodeTest());
        }

        if (collationName != null) {
            URI collationURI;
            try {
                collationURI = new URI(collationName);
                if (!collationURI.isAbsolute()) {
                    URI base = new URI(getBaseURI());
                    collationURI = base.resolve(collationURI);
                    collationName = collationURI.toString();
                }
            } catch (URISyntaxException err) {
                compileError("Collation name '" + collationName + "' is not a valid URI");
                //collationName = NamespaceConstant.CODEPOINT_COLLATION_URI;
            }
        } else {
            collationName = getDefaultCollationName();
        }
    }

    public Expression compile(Executable exec) throws XPathException {
        StaticContext env = getStaticContext();
        Configuration config = env.getConfiguration();
        StringCollator collator = null;
        if (collationName != null) {
            collator = getPrincipalStylesheet().findCollation(collationName);
            if (collator==null) {
                compileError("The collation name " + Err.wrap(collationName, Err.URI) + " is not recognized", "XTSE1210");
                collator = CodepointCollator.getInstance();
            }
            if (collator instanceof CodepointCollator) {
                // if the user explicitly asks for the codepoint collation, treat it as if they hadn't asked
                collator = null;
                collationName = null;

            } else if (!Configuration.getPlatform().canReturnCollationKeys(collator)) {
                compileError("The collation used for xsl:key must be capable of generating collation keys", "XTSE1210");
            }
        }

        if (use==null) {
            Expression body = compileSequenceConstructor(exec, iterateAxis(Axis.CHILD), true);

            try {
                use = new Atomizer(makeExpressionVisitor().simplify(body), config);
            } catch (XPathException e) {
                compileError(e);
            }

            try {
                RoleLocator role =
                    new RoleLocator(RoleLocator.INSTRUCTION, "xsl:key/use", 0);
                //role.setSourceLocator(new ExpressionLocation(this));
                use = TypeChecker.staticTypeCheck(
                                use,
                                SequenceType.makeSequenceType(BuiltInAtomicType.ANY_ATOMIC, StaticProperty.ALLOWS_ZERO_OR_MORE),
                                false, role, makeExpressionVisitor());
                // Do a further check that the use expression makes sense in the context of the match pattern
                use = makeExpressionVisitor().typeCheck(use, match.getNodeTest());


            } catch (XPathException err) {
                compileError(err);
            }
        }
        final TypeHierarchy th = config.getTypeHierarchy();
        BuiltInAtomicType useType = (BuiltInAtomicType)use.getItemType(th).getPrimitiveItemType();
        if (backwardsCompatibleModeIsEnabled()) {
            if (!useType.equals(BuiltInAtomicType.STRING) && !useType.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
                use = new AtomicSequenceConverter(use, BuiltInAtomicType.STRING);
                useType = BuiltInAtomicType.STRING;
            }
        }
        allocateSlots(use);
        allocatePatternSlots(match, stackFrameMap);
        //allocateSlots(new PatternSponsor(match));


        KeyManager km = getPrincipalStylesheet().getKeyManager();
        KeyDefinition keydef = new KeyDefinition(match, use, collationName, collator);
        keydef.setIndexedItemType(useType);
        keydef.setStackFrameMap(stackFrameMap);
        keydef.setLocation(getSystemId(), getLineNumber());
        keydef.setExecutable(getExecutable());
        keydef.setBackwardsCompatible(backwardsCompatibleModeIsEnabled());
        try {
            km.addKeyDefinition(keyName, keydef, exec.getConfiguration());
        } catch (XPathException err) {
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
