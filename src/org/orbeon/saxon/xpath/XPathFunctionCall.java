package org.orbeon.saxon.xpath;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.ValueRepresentation;
import org.orbeon.saxon.om.EmptyIterator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.Value;

import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;
import java.util.ArrayList;
import java.util.List;



/**
* This class is an expression that calls an external function supplied using the
 * JAXP XPathFunction interface
*/

public class XPathFunctionCall extends FunctionCall {

    private XPathFunction function;
    /**
     * Default constructor
     */

    public XPathFunctionCall(XPathFunction function) {
        this.function = function;
    }

    /**
    * preEvaluate: this method suppresses compile-time evaluation by doing nothing
    * (because the external function might have side-effects and might use the context)
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) {
        return this;
    }


    /**
    * Method called by the expression parser when all arguments have been supplied
    */

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
    }


    /**
     * Determine which aspects of the context the expression depends on. XPath external
     * functions are given no access to context information so they cannot have any
     * dependencies on it.
    */

    public int getIntrinsicDependencies() {
        return 0;
    }


    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        throw new UnsupportedOperationException("copy");
    }


    /**
     * Add a representation of this expression to a PathMap. The PathMap captures a map of the nodes visited
     * by an expression in a source tree.
     * <p/>
     * <p>The default implementation of this method assumes that an expression does no navigation other than
     * the navigation done by evaluating its subexpressions, and that the subexpressions are evaluated in the
     * same context as the containing expression. The method must be overridden for any expression
     * where these assumptions do not hold. For example, implementations exist for AxisExpression, ParentExpression,
     * and RootExpression (because they perform navigation), and for the doc(), document(), and collection()
     * functions because they create a new navigation root. Implementations also exist for PathExpression and
     * FilterExpression because they have subexpressions that are evaluated in a different context from the
     * calling expression.</p>
     *
     * @param pathMap     the PathMap to which the expression should be added
     * @param pathMapNodeSet
     * @return the pathMapNode representing the focus established by this expression, in the case where this
     *         expression is the first operand of a path expression or filter expression. For an expression that does
     *         navigation, it represents the end of the arc in the path map that describes the navigation route. For other
     *         expressions, it is the same as the input pathMapNode.
     */

    public PathMap.PathMapNodeSet addToPathMap(PathMap pathMap, PathMap.PathMapNodeSet pathMapNodeSet) {
        return addExternalFunctionCallToPathMap(pathMap, pathMapNodeSet);
    }

    /**
    * Evaluate the function. <br>
    * @param context The context in which the function is to be evaluated
    * @return a Value representing the result of the function.
    * @throws XPathException if the function cannot be evaluated.
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        ValueRepresentation[] argValues = new ValueRepresentation[argument.length];
        for (int i=0; i<argValues.length; i++) {
            argValues[i] = ExpressionTool.lazyEvaluate(argument[i], context, 10);
        }
        return call(argValues, context);
    }


    /**
     * Call an extension function previously identified using the bind() method. A subclass
     * can override this method.
     * @param argValues  The values of the arguments
     * @param context The XPath dynamic context
     * @return  The value returned by the extension function
     */

    protected SequenceIterator call(ValueRepresentation[] argValues, XPathContext context) throws XPathException {
        List convertedArgs = new ArrayList(argValues.length);
        Configuration config = context.getConfiguration();
        for (int i=0; i<argValues.length; i++) {
            Value actual = Value.asValue(argValues[i]).reduce();
            PJConverter converter = PJConverter.allocate(
                    config, actual.getItemType(config.getTypeHierarchy()), actual.getCardinality(), Object.class);
            convertedArgs.add(converter.convert(actual, Object.class, context));
        }
        try {
            Object result = function.evaluate(convertedArgs);
            if (result == null) {
                return EmptyIterator.getInstance();
            }
            JPConverter converter = JPConverter.allocate(result.getClass(), config);
            return Value.asIterator(converter.convert(result, context));
            //return Value.convertJavaObjectToXPath(result, SequenceType.ANY_SEQUENCE, context).iterate();
        } catch (XPathFunctionException e) {
            throw new XPathException(e);
        }
    }

    /**
     * Determine the data type of the expression, if possible. All expressions return
     * sequences, in general; this method determines the type of the items within the
     * sequence, assuming that (a) this is known in advance, and (b) it is the same for
     * all items in the sequence.
     *
     * <p>This method will always return a result, though it may be the best approximation
     * that is available at the time.</p>
     *
     * @return the item type
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        return Type.ITEM_TYPE;
    }

    /**
     * Determine the cardinality of the result
     * @return ZERO_OR_MORE (we don't know)
     */
    public int computeCardinality() {
        return StaticProperty.ALLOWS_ZERO_OR_MORE;
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
// Contributor(s): Gunther Schadow (changes to allow access to public fields; also wrapping
// of extensions and mapping of null to empty sequence).
//
