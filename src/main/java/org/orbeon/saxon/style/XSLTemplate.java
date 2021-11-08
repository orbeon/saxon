package org.orbeon.saxon.style;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.instruct.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.EmptySequenceTest;
import org.orbeon.saxon.pattern.Pattern;
import org.orbeon.saxon.trans.Mode;
import org.orbeon.saxon.trans.RuleManager;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.DecimalValue;
import org.orbeon.saxon.value.SequenceType;
import org.orbeon.saxon.value.Whitespace;

import javax.xml.transform.TransformerException;
import java.util.StringTokenizer;

/**
* An xsl:template element in the style sheet.
*/

public final class XSLTemplate extends StyleElement implements StylesheetProcedure {

    private String matchAtt = null;
    private String modeAtt = null;
    private String nameAtt = null;
    private String priorityAtt = null;
    private String asAtt = null;

    private StructuredQName[] modeNames;
    private String diagnosticId;
    private Pattern match;
    private boolean prioritySpecified;
    private double priority;
    private SlotManager stackFrameMap;
    private Template compiledTemplate = new Template();
    private SequenceType requiredType = null;
    private boolean hasRequiredParams = false;

    /**
    * Determine whether this type of element is allowed to contain a template-body
    * @return true: yes, it may contain a template-body
    */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    protected boolean mayContainParam() {
        return true;
    }

    /**
     * Specify that xsl:param is a permitted child
     */

    protected boolean isPermittedChild(StyleElement child) {
        return (child instanceof XSLParam);
    }

    /**
     * Return the name of this template. Note that this may
     * be called before prepareAttributes has been called.
     * @return the name of the template as a Structured QName.
    */

    public StructuredQName getTemplateName() {

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

    public void prepareAttributes() throws XPathException {

		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f.equals(StandardNames.MODE)) {
        		modeAtt = Whitespace.trim(atts.getValue(a));
			} else if (f.equals(StandardNames.NAME)) {
        		nameAtt = Whitespace.trim(atts.getValue(a));
			} else if (f.equals(StandardNames.MATCH)) {
        		matchAtt = atts.getValue(a);
			} else if (f.equals(StandardNames.PRIORITY)) {
        		priorityAtt = Whitespace.trim(atts.getValue(a));
        	} else if (f.equals(StandardNames.AS)) {
        		asAtt = atts.getValue(a);
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }
        try {
            if (modeAtt==null) {
                modeNames = new StructuredQName[1];
                modeNames[0] = Mode.DEFAULT_MODE_NAME;
            } else {
                if (matchAtt==null) {
                    compileError("The mode attribute must be absent if the match attribute is absent", "XTSE0500");
                }
                // mode is a space-separated list of mode names, or "#default", or "#all"

                int count = 0;
                boolean allModes = false;
                StringTokenizer st = new StringTokenizer(modeAtt, " \t\n\r", false);
                while (st.hasMoreTokens()) {
                    st.nextToken();
                    count++;
                }

                if (count==0) {
                    compileError("The mode attribute must not be empty", "XTSE0550");
                }

                modeNames = new StructuredQName[count];
                count = 0;
                st = new StringTokenizer(modeAtt, " \t\n\r", false);
                while (st.hasMoreTokens()) {
                    String s = st.nextToken();
                    StructuredQName mname;
                    if ("#default".equals(s)) {
                        mname = Mode.DEFAULT_MODE_NAME;
                    } else if ("#all".equals(s)) {
                        allModes = true;
                        mname = Mode.ALL_MODES;
                    } else {
                        mname = makeQName(s);
                    }
                    for (int e=0; e < count; e++) {
                        if (modeNames[e].equals(mname)) {
                            compileError("In the list of modes, the value " + s + " is duplicated", "XTSE0550");
                        }
                    }
                    modeNames[count++] = mname;
                }
                if (allModes && (count>1)) {
                    compileError("mode='#all' cannot be combined with other modes", "XTSE0550");
                }
            }
        } catch (NamespaceException err) {
            compileError(err.getMessage(), "XTSE0280");
        } catch (XPathException err) {
            if (err.getErrorCodeLocalPart() == null) {
                err.setErrorCode("XTSE0280");
            } else if (err.getErrorCodeLocalPart().equals("XTSE0020")) {
                err.setErrorCode("XTSE0550");
            }
            err.setIsStaticError(true);
            compileError(err);
        }

        try{
            if (nameAtt!=null) {
                StructuredQName qName = makeQName(nameAtt);
                setObjectName(qName);
                diagnosticId = nameAtt;
            }
        } catch (NamespaceException err) {
            compileError(err.getMessage(), "XTSE0280");
        } catch (XPathException err) {
            if (err.getErrorCodeLocalPart() == null) {
                err.setErrorCode("XTSE0280");
            } 
            err.setIsStaticError(true);
            compileError(err);
        }

        prioritySpecified = (priorityAtt != null);
        if (prioritySpecified) {
            if (matchAtt==null) {
                compileError("The priority attribute must be absent if the match attribute is absent", "XTSE0500");
            }
            try {
                // it's got to be a valid decimal, but we want it as a double, so parse it twice
                if (!DecimalValue.castableAsDecimal(priorityAtt)) {
                    compileError("Invalid numeric value for priority (" + priority + ')', "XTSE0530");
                }
                priority = Double.parseDouble(priorityAtt);
            } catch (NumberFormatException err) {
                // shouldn't happen
                compileError("Invalid numeric value for priority (" + priority + ')', "XTSE0530");
            }
        }

        if (matchAtt != null) {
            match = makePattern(matchAtt);
            if (diagnosticId == null) {
                diagnosticId = "match=\"" + matchAtt + '\"';
                if (modeAtt != null) {
                    diagnosticId += " mode=\"" + modeAtt + '\"';
                }
            }
        }

        if (match==null && nameAtt==null)
            compileError("xsl:template must have a name or match attribute (or both)", "XTSE0500");

        if (asAtt != null) {
            requiredType = makeSequenceType(asAtt);
        }
	}

    public void validate() throws XPathException {
        stackFrameMap = getConfiguration().makeSlotManager();
        checkTopLevel(null);

        // the check for duplicates is now done in the buildIndexes() method of XSLStylesheet
        if (match != null) {
            match = typeCheck("match", match);
            if (match.getNodeTest() instanceof EmptySequenceTest) {
                try {
                    getConfiguration().getErrorListener().warning(
                            new TransformerException("Match pattern cannot match any nodes", this));
                } catch (TransformerException e) {
                    compileError(XPathException.makeXPathException(e));
                }
            }
        }

        // See if there are any required parameters.
        AxisIterator kids = iterateAxis(Axis.CHILD);
        while(true) {
            NodeInfo param = (NodeInfo)kids.next();
            if (param == null) {
                break;
            }
            if (param instanceof XSLParam && ((XSLParam)param).isRequiredParam()) {
                hasRequiredParams = true;
                break;
            }
        }

    }


    public void postValidate() throws XPathException {
        markTailCalls();
    }

    /**
    * Mark tail-recursive calls on templates and functions.
    */

    public boolean markTailCalls() {
        StyleElement last = getLastChildInstruction();
        return last != null && last.markTailCalls();
    }

    /**
    * Compile: this registers the template with the rule manager, and ensures
    * space is available for local variables
    */

    public Expression compile(Executable exec) throws XPathException {

        Expression block = compileSequenceConstructor(exec, iterateAxis(Axis.CHILD), true);
        if (block == null) {
            block = Literal.makeEmptySequence();
        }
        compiledTemplate.setMatchPattern(match);
        compiledTemplate.setBody(block);
        compiledTemplate.setStackFrameMap(stackFrameMap);
        compiledTemplate.setExecutable(getExecutable());
        compiledTemplate.setSystemId(getSystemId());
        compiledTemplate.setLineNumber(getLineNumber());
        compiledTemplate.setHasRequiredParams(hasRequiredParams);
        compiledTemplate.setRequiredType(requiredType);

        Expression exp = null;
        try {
            exp = makeExpressionVisitor().simplify(block);
        } catch (XPathException e) {
            compileError(e);
        }

        try {
            if (requiredType != null) {
                RoleLocator role =
                        new RoleLocator(RoleLocator.TEMPLATE_RESULT, diagnosticId, 0);
                //role.setSourceLocator(new ExpressionLocation(this));
                role.setErrorCode("XTTE0505");
                exp = TypeChecker.staticTypeCheck(exp, requiredType, false, role, makeExpressionVisitor());
            }
        } catch (XPathException err) {
            compileError(err);
        }

        compiledTemplate.setBody(exp);
        compiledTemplate.init ( getObjectName(),
                                getPrecedence(),
                                getMinImportPrecedence());

        if (getConfiguration().isCompileWithTracing()) {
            TraceWrapper trace = new TraceInstruction(exp, this);
            trace.setLocationId(allocateLocationId(getSystemId(), getLineNumber()));
            trace.setContainer(compiledTemplate);
            exp = trace;
            compiledTemplate.setBody(exp);
        }

        ItemType contextItemType = Type.ITEM_TYPE;
        if (getObjectName() == null) {
            // the template can't be called by name, so the context item must match the match pattern
            contextItemType = match.getNodeTest();
        }

        ExpressionVisitor visitor = makeExpressionVisitor();
        try {
            // We've already done the typecheck of each XPath expression, but it's worth doing again at this
            // level because we have more information now.
            Expression exp2 = visitor.typeCheck(exp, contextItemType);
            exp2 = visitor.optimize(exp2, contextItemType);
            if (exp != exp2) {
                compiledTemplate.setBody(exp2);
                exp = exp2;
            }
        } catch (XPathException e) {
            compileError(e);
        }

        // Try to extract new global variables from the body of the function
//        ExpressionPresenter presenter = ExpressionPresenter.make(getConfiguration());
//        exp.explain(presenter);
//        presenter.close();
        if (!getConfiguration().isCompileWithTracing()) {
            Expression exp2 = getConfiguration().getOptimizer().promoteExpressionsToGlobal(exp, visitor);
            if (exp != exp2) {
                compiledTemplate.setBody(exp2);
                exp = exp2;
            }
        }

        allocateSlots(exp);
        if (match != null) {
            RuleManager mgr = getPrincipalStylesheet().getRuleManager();
            for (int i=0; i<modeNames.length; i++) {
                StructuredQName nc = modeNames[i];
                Mode mode = mgr.getMode(nc, true);
                if (prioritySpecified) {
                    mgr.setHandler(match, compiledTemplate, mode, getPrecedence(), priority);
                } else {
                    mgr.setHandler(match, compiledTemplate, mode, getPrecedence());
                }
            }

            allocatePatternSlots(match, getSlotManager());
        }

        if (isExplaining()) {
            System.err.println("Optimized expression tree for template at line " +
                    getLineNumber() + " in " + getSystemId() + ':');
            exp.explain(System.err);
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
     * Get the compiled template
     * @return the compiled template
    */

    public Template getCompiledTemplate() {
        return compiledTemplate;
    }

    /**
     * Get the type of construct. This will be a constant in
     * class {@link org.orbeon.saxon.trace.Location}. This method is part of the {@link org.orbeon.saxon.trace.InstructionInfo} interface
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
//
