package net.sf.saxon.style;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.ExpressionTool;
import net.sf.saxon.expr.RoleLocator;
import net.sf.saxon.expr.TypeChecker;
import net.sf.saxon.instruct.*;
import net.sf.saxon.om.AttributeCollection;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.NamespaceException;
import net.sf.saxon.pattern.NoNodeTest;
import net.sf.saxon.pattern.Pattern;
import net.sf.saxon.trans.Mode;
import net.sf.saxon.trans.RuleManager;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.Err;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import java.util.StringTokenizer;
import java.math.BigDecimal;

/**
* An xsl:template element in the style sheet.
*/

public final class XSLTemplate extends StyleElement implements StylesheetProcedure {

    private String matchAtt = null;
    private String modeAtt = null;
    private String nameAtt = null;
    private String priorityAtt = null;
    private String asAtt = null;

    private int[] modeNameCodes;
    private String diagnosticId;
    private Pattern match;
    private boolean prioritySpecified;
    private double priority;
    private SlotManager stackFrameMap;
    private Template compiledTemplate = new Template();
    private SequenceType requiredType = null;

    /**
    * Determine whether this type of element is allowed to contain a template-body
    * @return true: yes, it may contain a template-body
    */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    /**
     * Specify that xsl:param is a permitted child
     */

    protected boolean isPermittedChild(StyleElement child) {
        return (child instanceof XSLParam);
    }

    /**
    * Return the fingerprint for the name of this template. Note that this may
     * be called before prepareAttributes has been called.
    */

    public int getTemplateFingerprint() {

    	//We use -1 to mean "not yet evaluated"

        try {
        	if (getObjectFingerprint()==-1) {
        		// allow for forwards references
        		String nameAtt = getAttributeValue(StandardNames.NAME);
        		if (nameAtt!=null) {
        			setObjectNameCode(makeNameCode(nameAtt.trim()));
                }
            }
            return getObjectFingerprint();
        } catch (NamespaceException err) {
            return -1;          // the errors will be picked up later
        } catch (XPathException err) {
            return -1;
        }
    }

    /**
     * Determine the type of item returned by this template
     * @return the item type returned
     */

    protected ItemType getReturnedItemType() {
        if (requiredType==null) {
            return getCommonChildItemType();
        } else {
            return requiredType.getPrimaryType();
        }
    }

    private int getMinImportPrecedence() {
        return getContainingStylesheet().getMinImportPrecedence();
    }

    public void prepareAttributes() throws TransformerConfigurationException {

		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f==StandardNames.MODE) {
        		modeAtt = atts.getValue(a).trim();
			} else if (f==StandardNames.NAME) {
        		nameAtt = atts.getValue(a).trim();
			} else if (f==StandardNames.MATCH) {
        		matchAtt = atts.getValue(a);
			} else if (f==StandardNames.PRIORITY) {
        		priorityAtt = atts.getValue(a).trim();
        	} else if (f==StandardNames.AS) {
        		asAtt = atts.getValue(a);
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }
        try {
            if (modeAtt==null) {
                modeNameCodes = new int[1];
                modeNameCodes[0] = -1;
            } else {
                if (matchAtt==null) {
                    compileError("The mode attribute must be absent if the match attribute is absent", "XT0500");
                }
                // mode is a space-separated list of mode names, or "#default", or "#all"

                int count = 0;
                boolean allModes = false;
                StringTokenizer st = new StringTokenizer(modeAtt);
                while (st.hasMoreTokens()) {
                    st.nextToken();
                    count++;
                }

                if (count==0) {
                    compileError("The mode attribute must not be empty", "XT0550");
                }

                modeNameCodes = new int[count];
                count = 0;
                st = new StringTokenizer(modeAtt);
                while (st.hasMoreTokens()) {
                    String s = st.nextToken();
                    int code;
                    if ("#default".equals(s)) {
                        code = Mode.DEFAULT_MODE;
                    } else if ("#all".equals(s)) {
                        allModes = true;
                        code = Mode.ALL_MODES;
                    } else {
                        code = makeNameCode(s);
                    }
                    for (int e=0; e < count; e++) {
                        if (modeNameCodes[e] == code) {
                            compileError("In the list of modes, the value " + s + " is duplicated", "XT0550");
                        }
                    }
                    modeNameCodes[count++] = code;
                }
                if (allModes && (count>1)) {
                    compileError("mode='#all' cannot be combined with other modes", "XT0550");
                }
            }

            if (nameAtt!=null) {
                setObjectNameCode(makeNameCode(nameAtt.trim()));
                diagnosticId = nameAtt;
            }
        } catch (NamespaceException err) {
            compileError(err.getMessage(), "XT0280");
        } catch (XPathException err) {
            compileError(err.getMessage());
        }

        prioritySpecified = (priorityAtt != null);
        if (prioritySpecified) {
            if (matchAtt==null) {
                compileError("The priority attribute must be absent if the match attribute is absent", "XT0500");
            }
            try {
                // it's got to be a valid decimal, but we want it as a double, so parse it twice
                new BigDecimal(priorityAtt.trim());
                priority = Double.parseDouble(priorityAtt.trim());
            } catch (NumberFormatException err) {
                compileError("Invalid numeric value for priority (" + priority + ')', "XT0530");
            }
        }

        if (matchAtt != null) {
            match = makePattern(matchAtt);
            if (diagnosticId == null) {
                diagnosticId = "match=\"" + matchAtt + '\"';
            }
        }

        if (match==null && nameAtt==null)
            compileError("xsl:template must have a name or match attribute (or both)", "XT0010");

        if (asAtt != null) {
            requiredType = makeSequenceType(asAtt);
        }

	}

    public void validate() throws TransformerConfigurationException {
        stackFrameMap = getConfiguration().makeSlotManager();
        checkTopLevel(null);

        // the check for duplicates is now done in the buildIndexes() method of XSLStylesheet
        if (match != null) {
            typeCheck("match", match);
            if (match.getNodeTest() instanceof NoNodeTest) {
                try {
                    getConfiguration().getErrorListener().warning(
                            new TransformerException("Match pattern cannot match any nodes", this));
                } catch (TransformerException e) {
                    compileError(e);
                }
            }
        }
        markTailCalls();
    }

    /**
    * Mark tail-recursive calls on templates and functions.
    */

    public void markTailCalls() {
        if (requiredType == null) {
            // don't attempt tail call optimization if the return type needs checking
            StyleElement last = getLastChildInstruction();
            if (last != null) {
                last.markTailCalls();
            }
        }
    }

    /**
    * Compile: this registers the template with the rule manager, and ensures
    * space is available for local variables
    */

    public Expression compile(Executable exec) throws TransformerConfigurationException {

        Expression block = compileSequenceConstructor(exec, iterateAxis(Axis.CHILD), true);
        if (block == null) {
            block = EmptySequence.getInstance();
        }
        compiledTemplate.setBody(block);
        compiledTemplate.setStackFrameMap(stackFrameMap);
        compiledTemplate.setExecutable(getExecutable());
        compiledTemplate.setSystemId(getSystemId());
        compiledTemplate.setLineNumber(getLineNumber());

        Expression exp = null;
        try {
            exp = block.simplify(getStaticContext());
        } catch (XPathException e) {
            compileError(e);
        }
        
        try {
            if (requiredType != null) {
                RoleLocator role =
                        new RoleLocator(RoleLocator.TEMPLATE_RESULT, diagnosticId, 0, null);
                exp = TypeChecker.staticTypeCheck(exp, requiredType, false, role, getStaticContext());
            }
        } catch (XPathException err) {
            compileError(err);
        }

        compiledTemplate.setBody(exp);
        compiledTemplate.init ( getObjectFingerprint(),
                                getPrecedence(),
                                getMinImportPrecedence());

        if (getConfiguration().getTraceListener() != null) {
            TraceWrapper trace = new TraceInstruction(exp, this);
            trace.setLocationId(allocateLocationId(getSystemId(), getLineNumber()));
            trace.setParentExpression(compiledTemplate);
            exp = trace;
            compiledTemplate.setBody(exp);
        }




        ItemType contextItemType = Type.ITEM_TYPE;
        if (getObjectFingerprint() == -1) {
            // the template can't be called by name, so the context item must match the match pattern
            contextItemType = match.getNodeTest();
        }


        try {
            Expression exp2 = exp.analyze(staticContext, contextItemType);
            if (exp != exp2) {
                compiledTemplate.setBody(exp2);
                exp = exp2;
            }
        } catch (XPathException e) {
            compileError(e);
        }
        super.allocateSlots(exp);
        if (match!=null) {
            RuleManager mgr = getPrincipalStylesheet().getRuleManager();
            for (int i=0; i<modeNameCodes.length; i++) {
                int nc = modeNameCodes[i];
                Mode mode = mgr.getMode(nc);
                if (prioritySpecified) {
                    mgr.setHandler(match, compiledTemplate, mode, getPrecedence(), priority);
                } else {
                    mgr.setHandler(match, compiledTemplate, mode, getPrecedence());
                }
            }
        }

        return null;

    }


    /**
    * Get associated Procedure (for details of stack frame)
    */

    public SlotManager getSlotManager() {
        return stackFrameMap;
    }

    /**
     * Allocate space for range variables within predicates in the match pattern. The xsl:template
     * element has no XPath expressions among its attributes, so if this method is called on this
     * object it can only be because there are variables used in the match pattern. We work out
     * how many slots are needed for the match pattern in each template rule, and then apply-templates
     * can allocate a stack frame that is large enough for the most demanding match pattern in the
     * entire stylesheet.
     * @param exp The expression containing range variables. This will be a predicate within a match pattern,
     * or possibly an argument to id() or key() used in a match pattern.
     */

    public void allocateSlots(Expression exp) {
        int highWater = ExpressionTool.allocateSlots(exp, 0, null);
        getContainingStylesheet().allocatePatternSlots(highWater);
    }
    /**
    * Get the compiled template
    */

    public Template getCompiledTemplate() {
        return compiledTemplate;
    }

    /**
     * Get the type of construct. This will be a constant in
     * class {@link net.sf.saxon.trace.Location}. This method is part of the {@link net.sf.saxon.trace.InstructionInfo} interface
     */

    public int getConstructType() {
        return StandardNames.XSL_TEMPLATE;
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
// Contributor(s):
// Portions marked "e.g." are from Edwin Glaser (edwin@pannenleiter.de)
//
