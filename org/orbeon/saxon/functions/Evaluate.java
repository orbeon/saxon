package net.sf.saxon.functions;
import net.sf.saxon.expr.*;
import net.sf.saxon.instruct.InstructionDetails;
import net.sf.saxon.instruct.SlotManager;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamespaceResolver;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.SingletonIterator;
import net.sf.saxon.trace.Location;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.ObjectValue;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.xpath.StandaloneContext;
import net.sf.saxon.xpath.Variable;
import net.sf.saxon.xpath.XPathException;
import net.sf.saxon.xpath.DynamicError;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.ItemType;

import java.util.Iterator;


/**
* This class implements the saxon:evaluate(), saxon:expression(), and saxon:eval() extension functions,
* which are specially-recognized by the system because they need access to parts of the static context
*/

public class Evaluate extends SystemFunction {

    StandaloneContext staticContext;
    InstructionDetails details;
    public final static int EVALUATE = 0;
    public final static int EXPRESSION = 1;
    public final static int EVAL = 2;

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
        super.checkArguments(env);
        if (operation != EVAL) {
            NamespaceResolver nsContext = env.getNamespaceResolver();
            staticContext = new StandaloneContext(env.getConfiguration());
            staticContext.setBaseURI(env.getBaseURI());
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
        }

        // TODO: provide an option to take the namespace context for the expression from the
        // source document, rather than from the stylesheet. Perhaps as part of an XPointer
        // implementation?
    }

    /**
    * preEvaluate: this method suppresses compile-time evaluation by doing nothing
    * (because the value of the expression depends on the runtime context)
    */

    public Expression preEvaluate(StaticContext env) {
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

        } else {
            staticContext.setFunctionLibrary(context.getController().getExecutable().getFunctionLibrary());
            AtomicValue exprSource = (AtomicValue)argument[0].evaluateItem(context);

            PreparedExpression pexpr = new PreparedExpression();

            pexpr.variables = new Variable[10];
            for (int i=1; i<10; i++) {
                pexpr.variables[i-1] = staticContext.declareVariable("p"+i, EmptySequence.getInstance());
            }
            Expression expr;
            try {
                expr = ExpressionTool.make(exprSource.getStringValue(),
                                                                  staticContext,
                                                                  0, Token.EOF, 1);
            } catch (XPathException e) {
                DynamicError err = new DynamicError("Static error in XPath expression supplied to saxon:evaluate: " +
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
    }

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        PreparedExpression pexpr = prepareExpression(c);

        if (operation == EXPRESSION) {
            return new ObjectValue(pexpr);
        } else {
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
            return ExpressionTool.lazyEvaluate(pexpr.expression,  c2).iterate(c2);
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

    protected static class PreparedExpression {
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
