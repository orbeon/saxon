package net.sf.saxon.style;
import net.sf.saxon.Loader;
import net.sf.saxon.expr.*;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.instruct.NumberInstruction;
import net.sf.saxon.instruct.ValueOf;
import net.sf.saxon.number.NumberFormatter;
import net.sf.saxon.number.Numberer;
import net.sf.saxon.number.Numberer_en;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.pattern.Pattern;
import net.sf.saxon.tree.AttributeCollection;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.xpath.XPathException;

import javax.xml.transform.TransformerConfigurationException;

/**
* An xsl:number element in the stylesheet. <br>
*/

public class XSLNumber extends StyleElement {

    private static final int SINGLE = 0;
    private static final int MULTI = 1;
    private static final int ANY = 2;
    private static final int SIMPLE = 3;

    private int level;
    private Pattern count = null;
    private Pattern from = null;
    private Expression select = null;
    private Expression value = null;
    private Expression format = null;
    private Expression groupSize = null;
    private Expression groupSeparator = null;
    private Expression letterValue = null;
    private Expression lang = null;
    private Expression ordinal = null;
    private NumberFormatter formatter = null;
    private Numberer numberer = null;
    private boolean hasVariablesInPatterns = false;

    private static Numberer defaultNumberer = new Numberer_en();

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public boolean isInstruction() {
        return true;
    }

    /**
     * Determine the type of item returned by this instruction (only relevant if
     * it is an instruction).
     * @return the item type returned
     */

    protected ItemType getReturnedItemType() {
        return NodeKindTest.TEXT;
    }

    public void prepareAttributes() throws TransformerConfigurationException {

		AttributeCollection atts = getAttributeList();

		String selectAtt = null;
        String valueAtt = null;
		String countAtt = null;
		String fromAtt = null;
		String levelAtt = null;
		String formatAtt = null;
		String gsizeAtt = null;
		String gsepAtt = null;
		String langAtt = null;
		String letterValueAtt = null;
        String ordinalAtt = null;

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f==StandardNames.SELECT) {
        		selectAtt = atts.getValue(a);
            } else if (f==StandardNames.VALUE) {
        		valueAtt = atts.getValue(a);
        	} else if (f==StandardNames.COUNT) {
        		countAtt = atts.getValue(a);
        	} else if (f==StandardNames.FROM) {
        		fromAtt = atts.getValue(a);
        	} else if (f==StandardNames.LEVEL) {
        		levelAtt = atts.getValue(a).trim();
        	} else if (f==StandardNames.FORMAT) {
        		formatAtt = atts.getValue(a);
        	} else if (f==StandardNames.LANG) {
        		langAtt = atts.getValue(a);
        	} else if (f==StandardNames.LETTER_VALUE) {
        		letterValueAtt = atts.getValue(a).trim();
        	} else if (f==StandardNames.GROUPING_SIZE) {
        		gsizeAtt = atts.getValue(a).trim();
        	} else if (f==StandardNames.GROUPING_SEPARATOR) {
        		gsepAtt = atts.getValue(a);
            } else if (f==StandardNames.ORDINAL) {
                ordinalAtt = atts.getValue(a);
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (selectAtt != null) {
            select = makeExpression(selectAtt);
        }

        if (valueAtt!=null) {
            value = makeExpression(valueAtt);
            if (selectAtt != null) {
                compileError("The select attribute and value attribute must not both be present");
            }
            if (countAtt != null) {
                compileError("The count attribute and value attribute must not both be present");
            }
            if (fromAtt != null) {
                compileError("The from attribute and value attribute must not both be present");
            }
            if (levelAtt != null) {
                compileError("The level attribute and value attribute must not both be present");
            }
        }

        if (countAtt!=null) {
            count = makePattern(countAtt);
            // the following test is a very crude way of testing if the pattern might
            // contain variables, but it's good enough...
            if (countAtt.indexOf('$')>=0) {
                hasVariablesInPatterns = true;
            }
        } else {
//            // if count is not specified, we try to supply a suitable value if the node type
//            // and name are known statically. This is important because it enables optimization
//            // (currently used only for level=any) when the pattern is the same each time.

              // Removed because it only worked under very limited circumstances,
              // and there were bugs (e.g. when used within a template having both
              // a name and a match attribute). TODO: Should be based on static type analysis.

//            if (select == null) {
//                Pattern pat = getContextItemType();
//                if (pat == null) {
//                    compileError("The context node is undefined");
//                }
//                if (pat instanceof NameTest) {
//                    count = pat;
//                }
//            }
        }

        if (fromAtt!=null) {
            from = makePattern(fromAtt);
            if (fromAtt.indexOf('$')>=0) {
                hasVariablesInPatterns = true;
            }
        }

        if (levelAtt==null) {
            level = SINGLE;
        } else if (levelAtt.equals("single")) {
            level = SINGLE;
        } else if (levelAtt.equals("multiple")) {
            level = MULTI;
        } else if (levelAtt.equals("any")) {
            level = ANY;
        } else {
            compileError("Invalid value for level attribute");
        }

        if (level==SINGLE && from==null && count==null) {
            level=SIMPLE;
        }

        if (formatAtt != null) {
            format = makeAttributeValueTemplate(formatAtt);
            if (format instanceof StringValue) {
                formatter = new NumberFormatter();
                formatter.prepare(((StringValue)format).getStringValue());
            }
            // else we'll need to allocate the formatter at run-time
        } else {
            formatter = new NumberFormatter();
            formatter.prepare("1");
        }

        if (gsepAtt!=null && gsizeAtt!=null) {
            // the spec says that if only one is specified, it is ignored
            groupSize = makeAttributeValueTemplate(gsizeAtt);
            groupSeparator = makeAttributeValueTemplate(gsepAtt);
        }

        if (langAtt==null) {
            numberer = defaultNumberer;
        } else {
            lang = makeAttributeValueTemplate(langAtt);
            if (lang instanceof StringValue) {
                numberer = makeNumberer(((StringValue)lang).getStringValue());
            }   // else we allocate a numberer at run-time
        }

        if (letterValueAtt != null) {
            letterValue = makeAttributeValueTemplate(letterValueAtt);
        }

        if (ordinalAtt != null) {
            ordinal = makeAttributeValueTemplate(ordinalAtt);
        }

    }

    public void validate() throws TransformerConfigurationException {
        checkWithinTemplate();
        checkEmpty();

        select = typeCheck("select", select);
        value = typeCheck("value", value);
        format = typeCheck("format", format);
        groupSize = typeCheck("group-size", groupSize);
        groupSeparator = typeCheck("group-separator", groupSeparator);
        letterValue = typeCheck("letter-value", letterValue);
        ordinal = typeCheck("ordinal", ordinal);
        lang = typeCheck("lang", lang);
        from = typeCheck("from", from);
        count = typeCheck("count", count);

        if (select != null) {
            try {
                RoleLocator role =
                    new RoleLocator(RoleLocator.INSTRUCTION, "xsl:number/select", 0);
                select = TypeChecker.staticTypeCheck(select,
                                            SequenceType.SINGLE_NODE,
                                            false, role, getStaticContext());
            } catch (XPathException err) {
                compileError(err);
            }
        }
    }

    public Expression compile(Executable exec) throws TransformerConfigurationException {
        NumberInstruction expr = new NumberInstruction ( select,
                                        level,
                                        count,
                                        from,
                                        value,
                                        format,
                                        groupSize,
                                        groupSeparator,
                                        letterValue,
                                        ordinal,
                                        lang,
                                        formatter,
                                        numberer,
                                        hasVariablesInPatterns );
        int loc = getStaticContext().getLocationMap().allocateLocationId(getSystemId(), getLineNumber());
        expr.setLocationId(loc);
        ExpressionTool.makeParentReferences(expr);
        ValueOf inst = new ValueOf(expr, false);
        inst.setLocationId(allocateLocationId(getSystemId(), getLineNumber()));
        ExpressionTool.makeParentReferences(inst);
        inst.setIsNumberingInstruction();
        return inst;
    }

    /**
    * Load a Numberer class for a given language and check it is OK.
    */

    protected static Numberer makeNumberer (String language) {
        Numberer numberer;
        if (language.equals("en")) {
            numberer = defaultNumberer;
        } else {
            String langClassName = "net.sf.saxon.number.Numberer_";
            for (int i=0; i<language.length(); i++) {
                if (Character.isLetter(language.charAt(i))) {
                    langClassName += language.charAt(i);
                }
            }
            try {
                numberer = (Numberer)(Loader.getInstance(langClassName));
            } catch (Exception err) {
                numberer = defaultNumberer;
            }
        }

        return numberer;
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
