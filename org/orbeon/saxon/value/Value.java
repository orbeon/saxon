package net.sf.saxon.value;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.xpath.XPathException;
import net.sf.saxon.xpath.DynamicError;
import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.event.Builder;
import net.sf.saxon.event.Sender;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.BuiltInSchemaFactory;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import java.io.Serializable;
import java.math.BigInteger;
import java.math.BigDecimal;
import java.util.List;
import java.util.Iterator;
import java.util.Collections;

import org.w3c.dom.NodeList;

/**
* A value is the result of an expression but it is also an expression in its own right.
* Note that every value can be regarded as a sequence - in many cases, a sequence of
* length one.
*/

public abstract class Value implements Expression, Serializable {

    /**
     * Static method to make a Value from a given Item (which may be either an AtomicValue
     * or a NodeInfo
     * @param item      The supplied item, or null, indicating the empty sequence.
     * @return          The supplied item, if it is a value, or a SingletonNode that
     *                  wraps the item, if it is a node. If the supplied value was null,
     *                  return an EmptySequence
     */

    public static Value asValue(Item item) {
        if (item == null) {
            return EmptySequence.getInstance();
        } else if (item instanceof AtomicValue) {
            return (AtomicValue)item;
        } else {
            return new SingletonNode((NodeInfo)item);
        }
    }

    /**
     * Static method to make an Item from a Value
     * @param value the value to be converted
     * @param context the context. It is probably safe to set this to null.
     * @return null if the value is an empty sequence; or the only item in the value
     * if it is a singleton sequence
     * @throws XPathException if the Value contains multiple items
     */

    public static Item asItem(Value value, XPathContext context) throws XPathException {
        if (value instanceof EmptySequence) {
            return null;
        } else if (value instanceof SingletonNode) {
            return ((SingletonNode)value).getNode();
        } else if (value instanceof AtomicValue) {
            return (AtomicValue)value;
        } else if (value instanceof Closure) {
            return value.evaluateItem(context);
        } else {
            SequenceIterator iter = value.iterate(context);
            //if (iter.hasNext()) {
            Item item = iter.next();
            if (item == null) {
                return null;
            } else if (iter.next() != null) {
                throw new AssertionError("Attempting to access a sequence as an item");
            } else {
                return item;
            }
        }
    }

    /**
    * Static method to convert strings to numbers. Might as well go here as anywhere else.
    * @param s the String to be converted
    * @return a double representing the value of the String
     * @throws NumberFormatException if the value cannot be converted
    */

    public static double stringToNumber(CharSequence s) throws NumberFormatException {
        String n = trimWhitespace(s).toString();
        if ("INF".equals(n)) {
            return Double.POSITIVE_INFINITY;
        } else if ("-INF".equals(n)) {
            return Double.NEGATIVE_INFINITY;
        } else if ("NaN".equals(n)) {
            return Double.NaN;
        } else {
            return Double.parseDouble(n);
        }
    }


    /**
    * Normalize whitespace as defined in XML Schema
    */

    public static CharSequence normalizeWhitespace(CharSequence in) {
        StringBuffer sb = new StringBuffer(in.length());
        for (int i=0; i<in.length(); i++) {
            char c = in.charAt(i);
            switch (c) {
                case '\n':
                case '\r':
                case '\t':
                    sb.append(' ');
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb;
    }

    /**
    * Collapse whitespace as defined in XML Schema
    */

    public static CharSequence collapseWhitespace(CharSequence in) {
        if (in.length()==0) {
            return in;
        }

        StringBuffer sb = new StringBuffer(in.length());
        boolean inWhitespace = true;
        int i = 0;
        for (; i<in.length(); i++) {
            char c = in.charAt(i);
            switch (c) {
                case '\n':
                case '\r':
                case '\t':
                case ' ':
                    if (inWhitespace) {
                        // remove the whitespace
                    } else {
                        sb.append(' ');
                        inWhitespace = true;
                    }
                    break;
                default:
                    sb.append(c);
                    inWhitespace = false;
                    break;
            }
        }
        if (sb.charAt(sb.length()-1)==' ') {
            sb.deleteCharAt(sb.length()-1);
        }
        return sb;
    }

    /**
     * Remove leading and trailing whitespace. This has the same effect as collapseWhitespace,
     * but is cheaper, for use by data types that do not allow internal whitespace.
     * @param in the input string whose whitespace is to be removed
     * @return the result of removing excess whitespace
     */
    public static CharSequence trimWhitespace(CharSequence in) {
        if (in.length()==0) {
            return in;
        }
        int first = 0;
        int last = in.length()-1;
        while (in.charAt(first) <= 0x20) {
            if (first++ >= last) {
                return "";
            }
        }
        while (in.charAt(last) <= 0x20) {
            last--;
        }
        return in.subSequence(first, last+1);
    }

    /**
    * Simplify an expression
    * @return for a Value, this always returns the value unchanged
    */

    public final Expression simplify(StaticContext env) {
        return this;
    }

    /**
    * TypeCheck an expression
    * @return for a Value, this always returns the value unchanged
    */

    public final Expression analyze(StaticContext env, ItemType contextItemType) {
        return this;
    }

    /**
     * Get the sub-expressions of this expression.
     * @return for a Value, this always returns an empty array
     */

    public final Iterator iterateSubExpressions() {
        return Collections.EMPTY_LIST.iterator();
    }

    /**
     * Get the expression that immediately contains this expression. This method
     * returns null for an outermost expression; it also return null in the case
     * of literal values. For an XPath expression occurring within an XSLT stylesheet,
     * this method returns the XSLT instruction containing the XPath expression.
     * @return the expression that contains this expression, if known; return null
     * if there is no containing expression or if the containing expression is unknown.
     */

    public Container getParentExpression() {
        return null;
    }

    /**
     * Get the static properties of this expression (other than its type). For a
     * Value, there are no special properties, so the return value is always zero.
     * @return zero
     */


    public int getSpecialProperties() {
        return 0;
    }

    /**
     * Offer promotion for this subexpression. Values (constant expressions)
     * are never promoted
     * @param offer details of the offer, for example the offer to move
     *     expressions that don't depend on the context to an outer level in
     *     the containing expression
     * @return For a Value, this always returns the value unchanged
     */

     public Expression promote(PromotionOffer offer) {
        return this;
    }

    /**
    * Determine which aspects of the context the expression depends on. The result is
    * a bitwise-or'ed value composed from constants such as StaticProperty.VARIABLES and
    * StaticProperty.CURRENT_NODE
     * @return for a Value, this always returns zero.
    */

    public final int getDependencies() {
        return 0;
    }

	/**
	* Return the inverse of a relational operator, so that "a op b" can be
	* rewritten as "b inverse(op) a"
	*/

    public static final int inverse(int operator) {
        switch(operator) {
            case Token.EQUALS:
            case Token.NE:
            case Token.FEQ:
            case Token.FNE:
                return operator;
            case Token.LT:
                return Token.GT;
            case Token.LE:
                return Token.GE;
            case Token.GT:
                return Token.LT;
            case Token.GE:
                return Token.LE;
            case Token.FLT:
                return Token.FGT;
            case Token.FLE:
                return Token.FGE;
            case Token.FGT:
                return Token.FLT;
            case Token.FGE:
                return Token.FLE;
            default:
                return operator;
        }
    }

    /**
     * Convert the value to a string, using the serialization rules.
     * For atomic values this is the same as a cast; for sequence values
     * it gives a space-separated list.
     */

    public abstract String getStringValue() throws XPathException;

    /**
    * Convert the value to a Java object (for passing to external functions)
    * @param target The class required by the external function
    * @param config The configuration (needed for access to schema information)
    * @return an object of the target class
    */

    public abstract Object convertToJava(Class target, Configuration config, XPathContext context) throws XPathException;

    /**
    * Convert a Java object to an XPath value. This method is called to handle the result
    * of an external function call (but only if the required type is not known),
    * and also to process global parameters passed to the stylesheet.
    * @param result The Java object to be converted
    * @param controller The controller: may be null, in which case a Source object cannot be
    * supplied
     * @return the result of converting the value. If the value is null, returns null.
    */

    public static Value convertJavaObjectToXPath(Object result, Controller controller)
                                          throws XPathException {

        if (result==null) {
            return null;

        } else if (result instanceof String) {
            return new StringValue((String)result);

        } else if (result instanceof Character) {
            return new StringValue(result.toString());

        } else if (result instanceof Boolean) {
            return BooleanValue.get(((Boolean)result).booleanValue());

        } else if (result instanceof Double) {
            return new DoubleValue(((Double)result).doubleValue());

        } else if (result instanceof Float) {
            return new FloatValue(((Float)result).floatValue());

        } else if (result instanceof Short) {
            return new IntegerValue(((Short)result).shortValue(),
                                    (ItemType)BuiltInSchemaFactory.getSchemaType(StandardNames.XS_SHORT));
        } else if (result instanceof Integer) {
            return new IntegerValue(((Integer)result).intValue(),
                                    (ItemType)BuiltInSchemaFactory.getSchemaType(StandardNames.XS_INT));
        } else if (result instanceof Long) {
            return new IntegerValue(((Long)result).longValue(),
                                    (ItemType)BuiltInSchemaFactory.getSchemaType(StandardNames.XS_LONG));
        } else if (result instanceof Byte) {
            return new IntegerValue(((Byte)result).byteValue(),
                                    (ItemType)BuiltInSchemaFactory.getSchemaType(StandardNames.XS_BYTE));

        } else if (result instanceof BigInteger) {
            return BigIntegerValue.makeValue(((BigInteger)result));

        } else if (result instanceof BigDecimal) {
            return new DecimalValue(((BigDecimal)result));

        } else if (result instanceof Closure) {
            // Force eager evaluation, because of problems with side-effects.
            // (The value might depend on data that is mutable.)
            return ExpressionTool.eagerEvaluate((Closure)result, null);

        } else if (result instanceof Value) {
            return (Value)result;

        } else if (result instanceof NodeInfo) {
            return new SingletonNode((NodeInfo)result);

        } else if (result instanceof SequenceIterator) {
            return new SequenceIntent((SequenceIterator)result);

        } else if (result instanceof List) {
            Item[] array = new Item[((List)result).size()];
            int a = 0;
            for (Iterator i=((List)result).iterator(); i.hasNext(); ) {
                Object obj = i.next();
                if (obj instanceof NodeInfo) {
                    array[a++] = (NodeInfo)obj;
                } else {
                    Value v = convertJavaObjectToXPath(obj, controller);
                    if (v!=null) {
                        if (v instanceof Item) {
                            array[a++] = (Item)v;
                        } else if (v instanceof EmptySequence) {
                            // no action
                        } else if (v instanceof SingletonNode) {
                            NodeInfo node = ((SingletonNode)v).getNode();
                            if (node != null) {
                                array[a++] = node;
                            }
                        } else {
                            throw new DynamicError(
                                    "Returned List contains an object that cannot be converted to an Item (" + obj.getClass() + ')');
                        }
                    }
                }
            }

            return new SequenceExtent(array);

        } else if (result instanceof Object[]) {
             Item[] array = new Item[((Object[])result).length];
             int a = 0;
             for (int i = 0; i < ((Object[])result).length; i++){
                 Object obj = ((Object[])result)[i];
                 if (obj instanceof NodeInfo) {
                     array[a++] = (NodeInfo)obj;
                 } else {
                     Value v = convertJavaObjectToXPath(obj, controller);
                     if (v!=null) {
                         if (v instanceof Item) {
                             array[a++] = (Item)v;
                         } else {
                             throw new DynamicError(
                                     "Returned array contains an object that cannot be converted to an Item (" + obj.getClass() + ')');
                         }
                     }
                 }
             }
             return new SequenceExtent(array);

        } else if (result instanceof long[]) {
             Item[] array = new Item[((long[])result).length];
             for (int i = 0; i < ((long[])result).length; i++){
                 array[i] = new IntegerValue(((long[])result)[i]);
             }
             return new SequenceExtent(array);

        } else if (result instanceof int[]) {
             Item[] array = new Item[((int[])result).length];
             for (int i = 0; i < ((int[])result).length; i++){
                 array[i] = new IntegerValue(((int[])result)[i]);
             }
             return new SequenceExtent(array);

        } else if (result instanceof short[]) {
             Item[] array = new Item[((short[])result).length];
             for (int i = 0; i < ((short[])result).length; i++){
                 array[i] = new IntegerValue(((short[])result)[i]);
             }
             return new SequenceExtent(array);

        } else if (result instanceof byte[]) {  // interpret this as unsigned bytes
             Item[] array = new Item[((byte[])result).length];
             for (int i = 0; i < ((byte[])result).length; i++){
                 array[i] = new IntegerValue(255 & (int)((byte[])result)[i]);
             }
             return new SequenceExtent(array);

        } else if (result instanceof char[]) {
             return new StringValue(new String((char[])result));

       } else if (result instanceof boolean[]) {
             Item[] array = new Item[((boolean[])result).length];
             for (int i = 0; i < ((boolean[])result).length; i++){
                 array[i] = BooleanValue.get(((boolean[])result)[i]);
             }
             return new SequenceExtent(array);

        } else if (result instanceof Source && controller != null) {
            if (result instanceof DOMSource) {
                return new SingletonNode(controller.prepareInputTree((Source)result));
            }
            try {
                Builder b = controller.makeBuilder();
                new Sender(controller.getConfiguration()).send((Source) result, b);
                return new SingletonNode(b.getCurrentDocument());
            } catch (XPathException err) {
                throw new DynamicError(err);
            }

        } else if (result instanceof org.w3c.dom.NodeList) {
            NodeList list = ((NodeList)result);
            NodeInfo[] nodes = new NodeInfo[list.getLength()];
            for (int i=0; i<list.getLength(); i++) {
                if (list.item(i) instanceof NodeInfo) {
                    nodes[i] = (NodeInfo)list.item(i);
                } else {
                    throw new DynamicError("Supplied NodeList contains non-Saxon DOM Nodes");
                }

            }
            return new SequenceExtent(nodes);
            // Note, we accept the nodes in the order returned by the function; there
            // is no requirement that this should be document order.
        } else if (result instanceof org.w3c.dom.Node) {
            throw new DynamicError("Supplied Java object is a non-Saxon DOM Node");
        } else {
            return new ObjectValue(result);
        }
    }

    /**
     * Convert to a string for diagnostic output
     */

    public String toString() {
        try {
            return getStringValue();
        } catch (XPathException err) {
            return super.toString();
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
