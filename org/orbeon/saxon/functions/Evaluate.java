package net.sf.saxon.functions;
import net.sf.saxon.expr.*;
import net.sf.saxon.instruct.InstructionDetails;
import net.sf.saxon.instruct.SlotManager;
import net.sf.saxon.om.*;
import net.sf.saxon.trace.Location;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.IndependentContext;
import net.sf.saxon.trans.Variable;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.*;

import java.util.Iterator;


/**
* This class implements the saxon:evaluate(), saxon:expression(), and saxon:eval() extension functions,
* which are specially-recognized by the system because they need access to parts of the static context
*/

public class Evaluate extends SystemFunction {

    // TODO: make saxon:expression into a data type rather than a function. The function then comes "for free"
    // as a constructor function, but it also becomes possible to write things like
    // <xsl:variable name="exp" as="saxon:expression">@price * @qty</xsl:variable>
    // <xsl:value-of select="sum(//item, $exp)"/>

    IndependentContext staticContext;
    InstructionDetails details;
    public static final int EVALUATE = 0;
    public static final int EXPRESSION = 1;
    public static final int EVAL = 2;
    public static final int EVALUATE_NODE = 3;

    /**
    * Get the required type of the nth argument
    */

    protected SequenceType getRequiredType(int arg) {
        if (arg==0) {
            return super.getRequiredType(arg);
        } else {
            return SequenceType.ANY_SEQUENCE;
        }
    }

    /**
    * Method supplied by each class of function to check arguments during parsing, when all
    * the argument expressions have been read
    */

    public void checkArguments(StaticContext env) throws XPathException {
        if (staticContext == null) {
            // only do this once
            super.checkArguments(env);
            if (operation == EVALUATE || operation == EXPRESSION) {
                NamespaceResolver nsContext = env.getNamespaceResolver();
                staticContext = new IndependentContext(env.getConfiguration());
                staticContext.setBaseURI(env.getBaseURI());
                staticContext.setSchemaImporter(env);
                staticContext.setDefaultFunctionNamespace(env.getDefaultFunctionNamespace());
                for (Iterator iter = nsContext.iteratePrefixes(); iter.hasNext();) {
                    String prefix = (String)iter.next();
                    String uri = nsContext.getURIForPrefix(prefix, true);
                    staticContext.declareNamespace(prefix, uri);
                }
                details = new InstructionDetails();
                details.setConstructType(Location.SAXON_EVALUATE);
                details.setSystemId(env.getLocationMap().getSystemId(this.locationId));
                details.setLineNumber(env.getLocationMap().getLineNumber(this.locationId));
            } else if (operation == EVALUATE_NODE) {
                staticContext = new IndependentContext(env.getConfiguration());
            }
        }
    }

    /**
    * preEvaluate: this method suppresses compile-time evaluation by doing nothing
    * (because the value of the expression depends on the runtime context)
    */

    public Expression preEvaluate(StaticContext env) throws XPathException {
        if (operation == EXPRESSION) {
            // compile-time evaluation of saxon:expression is allowed
            if (argument[0] instanceof StringValue) {
                PreparedExpression pexpr = new PreparedExpression();
                staticContext.setFunctionLibrary(env.getFunctionLibrary());
                String exprText = ((StringValue)argument[0]).getStringValue();
                pexpr.variables = new Variable[10];
                for (int i=1; i<10; i++) {
                    pexpr.variables[i-1] = staticContext.declareVariable("p"+i, EmptySequence.getInstance());
                }
                Expression expr = ExpressionTool.make(exprText, staticContext, 0, Token.EOF, 1);

                ItemType contextItemType = Type.ITEM_TYPE;
                expr = expr.analyze(staticContext, contextItemType);
                pexpr.stackFrameMap = staticContext.getStackFrameMap();
                ExpressionTool.allocateSlots(expr, pexpr.stackFrameMap.getNumberOfVariables(), pexpr.stackFrameMap);
                pexpr.expression = expr;
                return new ObjectValue(pexpr);
            }
        }
        // the other operations don't allow compile time evaluation because they need a run-time context
        return this;
    }

    private PreparedExpression prepareExpression(XPathContext context) throws XPathException {
        if (operation == EVAL) {
            Item item = argument[0].evaluateItem(context);
            if (!(item instanceof ObjectValue)) {
                dynamicError(
                    "First argument to saxon:eval must be an expression prepared using saxon:expression", context);
                return null;
            }
            ObjectValue obj = (ObjectValue)item;
            Object v = obj.getObject();
            if (!(v instanceof PreparedExpression)) {
                dynamicError(
                    "First argument to saxon:eval must be an expression prepared using saxon:expression", context);
                return null;
            }
            return (PreparedExpression)v;

        }

        PreparedExpression pexpr = new PreparedExpression();
        String exprText;
        if (operation == EVALUATE_NODE) {
            NodeInfo node = (NodeInfo)argument[0].evaluateItem(context);
            staticContext.setBaseURI(node.getBaseURI());
            staticContext.setNamespaces(node);
            exprText = node.getStringValue();
            context.setCurrentIterator(SingletonIterator.makeIterator(node));
        } else {
            staticContext.setFunctionLibrary(context.getController().getExecutable().getFunctionLibrary());
            AtomicValue exprSource = (AtomicValue)argument[0].evaluateItem(context);
            exprText = exprSource.getStringValue();
            pexpr.variables = new Variable[10];
            for (int i=1; i<10; i++) {
                pexpr.variables[i-1] = staticContext.declareVariable("p"+i, EmptySequence.getInstance());
            }
        }

        Expression expr;
        try {
            expr = ExpressionTool.make(exprText, staticContext, 0, Token.EOF, 1);
        } catch (XPathException e) {
            String name = context.getController().getNamePool().getDisplayName(getFunctionNameCode());
            DynamicError err = new DynamicError("Static error in XPath expression supplied to " + name + ": " +
                    e.getMessage().trim());
            err.setXPathContext(context);
            throw err;
        }
        ItemType contextItemType = Type.ITEM_TYPE;
        expr = expr.analyze(staticContext, contextItemType);
        pexpr.stackFrameMap = staticContext.getStackFrameMap();
        ExpressionTool.allocateSlots(expr, pexpr.stackFrameMap.getNumberOfVariables(), pexpr.stackFrameMap);
        pexpr.expression = expr;

        return pexpr;
    }

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        if (operation == EXPRESSION) {
            PreparedExpression pexpr = prepareExpression(c);
            return new ObjectValue(pexpr);
        } else if (operation == EVALUATE_NODE) {
            XPathContextMajor c2 = c.newCleanContext();
            PreparedExpression pexpr = prepareExpression(c2);
            c2.setOrigin(details);
            c2.openStackFrame(pexpr.stackFrameMap);
            return pexpr.expression.evaluateItem(c2);
        } else {
            PreparedExpression pexpr = prepareExpression(c);
            for (int i=1; i<argument.length; i++) {
                pexpr.variables[i-1].setXPathValue(ExpressionTool.eagerEvaluate(argument[i],c));
            }
            XPathContextMajor c2 = c.newCleanContext();
            c2.setOrigin(details);
            c2.openStackFrame(pexpr.stackFrameMap);
            c2.setCurrentIterator(c.getCurrentIterator());
            return pexpr.expression.evaluateItem(c2);
        }
    }

    /**
    * Iterate over the results of the function
    */

    public SequenceIterator iterate(XPathContext c) throws XPathException {
        PreparedExpression pexpr = prepareExpression(c);

        if (operation == EXPRESSION) {
            return SingletonIterator.makeIterator(new ObjectValue(pexpr));
        } else {
            for (int i=1; i<argument.length; i++) {
                pexpr.variables[i-1].setXPathValue(ExpressionTool.eagerEvaluate(argument[i],c));
            }
            XPathContextMajor c2 = c.newCleanContext();
            c2.setOrigin(details);
            c2.openStackFrame(pexpr.stackFrameMap);
            c2.setCurrentIterator(c.getCurrentIterator());
            return Value.getIterator(
                    ExpressionTool.lazyEvaluate(pexpr.expression,  c2, false));
        }
    }

    /**
    * Determine the dependencies
    */

    public int getIntrinsicDependencies() {
       return StaticProperty.DEPENDS_ON_FOCUS;
    }

    /**
    * Inner class PreparedExpression represents a compiled XPath expression together
    * with the standard variables $p1 .. $p9 available for use when the expression is
    * evaluated
    */

    public static class PreparedExpression {
        public Expression expression;
        public Variable[] variables;
        public SlotManager stackFrameMap;
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
