package org.orbeon.saxon.style;
import org.orbeon.saxon.trans.Err;
import org.orbeon.saxon.value.Whitespace;
import org.orbeon.saxon.sort.IntHashMap;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.XPathException;

import java.util.*;

/**
* An xsl:character-map declaration in the stylesheet. <br>
*/

public class XSLCharacterMap extends StyleElement {

    //int fingerprint;
                // the name of this character map

    String use;
                // the value of the use-character-maps attribute, as supplied

    List characterMapElements = null;
                // list of XSLCharacterMap objects referenced by this one

    boolean validated = false;
                // set to true once validate() has been called

    boolean redundant = false;
                // set to true if another character-map overrrides this one

    /**
     * Get the fingerprint of the name of this character map
     * @return the fingerprint value
     */

    public StructuredQName getCharacterMapName() {
        StructuredQName name = getObjectName();
        if (name == null) {
            try {
                return makeQName(getAttributeValue("name"));
            } catch (Exception err) {
                // the error will be reported later
                return new StructuredQName("", "", "unnamedCharacterMap_" + hashCode());
            }
        }
        return name;
    }

    /**
     * Test whether this character map is redundant (because another with the
     * same name has higher import precedence). Note that a character map is not
     * considered redundant simply because it is not referenced in an xsl:output
     * declaration; we allow character-maps to be selected at run-time using the
     * setOutputProperty() API.
     * @return true if this character map is redundant
     */

    public boolean isRedundant() {
        return redundant;
    }

    /**
     * Validate the attributes on this instruction
     * @throws XPathException
     */

    public void prepareAttributes() throws XPathException {

		String name = null;
		use = null;

		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f.equals(StandardNames.NAME)) {
        		name = Whitespace.trim(atts.getValue(a));
        	} else if (f.equals(StandardNames.USE_CHARACTER_MAPS)) {
        		use = atts.getValue(a);
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (name==null) {
            reportAbsence("name");
            name = "unnamedCharacterMap_" + hashCode();
        }

        try {
            setObjectName(makeQName(name));
        } catch (NamespaceException err) {
            compileError(err.getMessage(), "XTSE0280");
            name = "unnamedCharacterMap_" + hashCode();
            setObjectName(new StructuredQName("", "", name));
        } catch (XPathException err) {
            compileError(err.getMessage(), "XTSE0020");
            name = "unnamedCharacterMap_" + hashCode();
            setObjectName(new StructuredQName("", "", name));
        }

    }

    public void validate() throws XPathException {

        if (validated) return;

        // check that this is a top-level declaration

        checkTopLevel(null);

        // check that the only children are xsl:output-character elements

        AxisIterator kids = iterateAxis(Axis.CHILD);
        while (true) {
            Item child = kids.next();
            if (child == null) {
                break;
            }
            if (!(child instanceof XSLOutputCharacter)) {
                compileError("Only xsl:output-character is allowed within xsl:character-map", "XTSE0010");
            }
        }

        // check that there isn't another character-map with the same name and import
        // precedence

        XSLStylesheet principal = getPrincipalStylesheet();
        XSLCharacterMap other = principal.getCharacterMap(getObjectName());
        if (other != this) {
            if (getPrecedence() == other.getPrecedence()) {
                compileError("There are two character-maps with the same name and import precedence", "XTSE1580");
            } else if (getPrecedence() < other.getPrecedence()) {
                redundant = true;
            }
        }

        // validate the use-character-maps attribute

        if (use!=null) {

            // identify any character maps that this one refers to

            characterMapElements = new ArrayList(5);
            StringTokenizer st = new StringTokenizer(use, " \t\n\r", false);

            while (st.hasMoreTokens()) {
                String displayname = st.nextToken();
                try {
                    String[] parts = getConfiguration().getNameChecker().getQNameParts(displayname);
                    String uri = getURIForPrefix(parts[0], false);
                    if (uri == null) {
                        compileError("Undeclared namespace prefix " + Err.wrap(parts[0])
                                + " in character map name", "XTSE0280");
                    }
                    StructuredQName qn = new StructuredQName(parts[0], uri, parts[1]);
                    XSLCharacterMap ref = principal.getCharacterMap(qn);
                    if (ref == null) {
                        compileError("No character-map named '" + displayname + "' has been defined", "XTSE1590");
                    } else {
                        characterMapElements.add(ref);
                    }
                } catch (QNameException err) {
                    compileError("Invalid character-map name. " + err.getMessage(), "XTSE1590");
                }
            }

            // check for circularity

            for (Iterator it=characterMapElements.iterator(); it.hasNext();) {
                ((XSLCharacterMap)it.next()).checkCircularity(this);
            }
        }

        validated = true;
    }

    /**
    * Check for circularity: specifically, check that this attribute set does not contain
    * a direct or indirect reference to the one supplied as a parameter
     * @param origin the start point of the search
    */

    private void checkCircularity(XSLCharacterMap origin) throws XPathException {
        if (this==origin) {
            compileError("The definition of the character map is circular", "XTSE1600");
            characterMapElements = null;    // for error recovery
        } else {
            if (!validated) {
                // if this attribute set isn't validated yet, we don't check it.
                // The circularity will be detected when the last attribute set in the cycle
                // gets validated
                return;
            }
            if (characterMapElements != null) {
                for (Iterator it=characterMapElements.iterator(); it.hasNext();) {
                    ((XSLCharacterMap)it.next()).checkCircularity(origin);
                }
            }
        }
    }

    /**
     * Assemble all the mappings defined by this character map, adding them to a
     * HashMap that maps integer codepoints to strings
     * @param map a hash map to be populated with the character mappings
     */

    public void assemble(IntHashMap map) {
        if (characterMapElements != null) {
            for (int i = 0; i < characterMapElements.size(); i++) {
                XSLCharacterMap charmap = (XSLCharacterMap) characterMapElements.get(i);
                charmap.assemble(map);
            }
        }
        AxisIterator kids = iterateAxis(Axis.CHILD);
        while (true) {
            Item child = kids.next();
            if (child == null) {
                return;
            }
            XSLOutputCharacter oc = (XSLOutputCharacter)child;
            map.put(oc.getCodePoint(), oc.getReplacementString());
        }
    }

    public Expression compile(Executable exec) throws XPathException {
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
