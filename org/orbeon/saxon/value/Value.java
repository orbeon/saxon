package net.sf.saxon.value;
import net.sf.saxon.Configuration;
import net.sf.saxon.dom.DOMNodeList;
import net.sf.saxon.event.Builder;
import net.sf.saxon.event.Sender;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.type.*;
import net.sf.saxon.xpath.DynamicError;
import net.sf.saxon.xpath.XPathException;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

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

    public final Container getParentExpression() {
        return null;
    }

    /**
     * Get the static properties of this expression (other than its type). For a
     * Value, the only special property is {@link StaticProperty#NON_CREATIVE}.
     * @return {@link StaticProperty#NON_CREATIVE}
     */


    public int getSpecialProperties() {
        return StaticProperty.NON_CREATIVE;
    }

    /**
     * Offer promotion for this subexpression. Values (constant expressions)
     * are never promoted
     * @param offer details of the offer, for example the offer to move
     *     expressions that don't depend on the context to an outer level in
     *     the containing expression
     * @return For a Value, this always returns the value unchanged
     */

     public final Expression promote(PromotionOffer offer) {
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
     * Check statically that the results of the expression are capable of constructing the content
     * of a given schema type.
     * @param parentType The schema type
     * @param env the static context
     * @param whole
     * @throws XPathException if the expression doesn't match the required content type
     */

    public void checkPermittedContents(SchemaType parentType, StaticContext env, boolean whole) throws XPathException {
        return;
    }

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
    * and also to process global parameters passed to the stylesheet or query.
    * @param object The Java object to be converted
    * @param requiredType The required type of the result (if known)
    * @param context The XPathContext: may be null, in which case a Source object cannot be
    * supplied
    * @return the result of converting the value. If the value is null, returns null.
    */

    public static Value convertJavaObjectToXPath(
            Object object, SequenceType requiredType, XPathContext context)
                                          throws XPathException {

        ItemType requiredItemType = requiredType.getPrimaryType();

        if (object==null) {
            return EmptySequence.getInstance();
        }

        if (requiredItemType instanceof ExternalObjectType) {
            Class theClass = ((ExternalObjectType)requiredItemType).getJavaClass();
            if (theClass.isAssignableFrom(object.getClass())) {
                return new ObjectValue(object);
            } else {
                throw new DynamicError("Supplied parameter value is not of class " + theClass.getName());
            }
        }

        Value value = convertToBestFit(object, context);
        return value;

    }

    private static Value convertToBestFit(Object object, XPathContext context) throws XPathException {
        if (object instanceof String) {
            return new StringValue((String)object);

        } else if (object instanceof Character) {
            return new StringValue(object.toString());

        } else if (object instanceof Boolean) {
            return BooleanValue.get(((Boolean)object).booleanValue());

        } else if (object instanceof Double) {
            return new DoubleValue(((Double)object).doubleValue());

        } else if (object instanceof Float) {
            return new FloatValue(((Float)object).floatValue());

        } else if (object instanceof Short) {
            return new IntegerValue(((Short)object).shortValue(),
                                    (AtomicType)BuiltInSchemaFactory.getSchemaType(StandardNames.XS_SHORT));
        } else if (object instanceof Integer) {
            return new IntegerValue(((Integer)object).intValue(),
                                    (AtomicType)BuiltInSchemaFactory.getSchemaType(StandardNames.XS_INT));
        } else if (object instanceof Long) {
            return new IntegerValue(((Long)object).longValue(),
                                    (AtomicType)BuiltInSchemaFactory.getSchemaType(StandardNames.XS_LONG));
        } else if (object instanceof Byte) {
            return new IntegerValue(((Byte)object).byteValue(),
                                    (AtomicType)BuiltInSchemaFactory.getSchemaType(StandardNames.XS_BYTE));

        } else if (object instanceof BigInteger) {
            return BigIntegerValue.makeValue(((BigInteger)object));

        } else if (object instanceof BigDecimal) {
            return new DecimalValue(((BigDecimal)object));

        } else if (object instanceof QName) {
            return new QNameValue((QName)object);

        } else if (object instanceof URI) {
            return new AnyURIValue(object.toString());

        } else if (object instanceof URL) {
            return new AnyURIValue(object.toString());

        } else if (object instanceof Closure) {
            // Force eager evaluation, because of problems with side-effects.
            // (The value might depend on data that is mutable.)
            return ExpressionTool.eagerEvaluate((Closure)object, null);

        } else if (object instanceof Value) {
            return (Value)object;

        } else if (object instanceof NodeInfo) {
            return new SingletonNode((NodeInfo)object);

        } else if (object instanceof SequenceIterator) {
            return new SequenceIntent((SequenceIterator)object);

        } else if (object instanceof List) {
            Item[] array = new Item[((List)object).size()];
            int a = 0;
            for (Iterator i=((List)object).iterator(); i.hasNext(); ) {
                Object obj = i.next();
                if (obj instanceof NodeInfo) {
                    array[a++] = (NodeInfo)obj;
                } else {
                    Value v = convertToBestFit(obj, context);
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

        } else if (object instanceof Object[]) {
             Item[] array = new Item[((Object[])object).length];
             int a = 0;
             for (int i = 0; i < ((Object[])object).length; i++){
                 Object obj = ((Object[])object)[i];
                 if (obj instanceof NodeInfo) {
                     array[a++] = (NodeInfo)obj;
                 } else {
                     Value v = convertToBestFit(obj, context);
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

        } else if (object instanceof long[]) {
             Item[] array = new Item[((long[])object).length];
             for (int i = 0; i < ((long[])object).length; i++){
                 array[i] = new IntegerValue(((long[])object)[i]);
             }
             return new SequenceExtent(array);

        } else if (object instanceof int[]) {
             Item[] array = new Item[((int[])object).length];
             for (int i = 0; i < ((int[])object).length; i++){
                 array[i] = new IntegerValue(((int[])object)[i]);
             }
             return new SequenceExtent(array);

        } else if (object instanceof short[]) {
             Item[] array = new Item[((short[])object).length];
             for (int i = 0; i < ((short[])object).length; i++){
                 array[i] = new IntegerValue(((short[])object)[i]);
             }
             return new SequenceExtent(array);

        } else if (object instanceof byte[]) {  // interpret this as unsigned bytes
             Item[] array = new Item[((byte[])object).length];
             for (int i = 0; i < ((byte[])object).length; i++){
                 array[i] = new IntegerValue(255 & (int)((byte[])object)[i]);
             }
             return new SequenceExtent(array);

        } else if (object instanceof char[]) {
             return new StringValue(new String((char[])object));

       } else if (object instanceof boolean[]) {
             Item[] array = new Item[((boolean[])object).length];
             for (int i = 0; i < ((boolean[])object).length; i++){
                 array[i] = BooleanValue.get(((boolean[])object)[i]);
             }
             return new SequenceExtent(array);

        } else if (object instanceof Source && context != null) {
            if (object instanceof DOMSource) {
                return new SingletonNode(context.getController().prepareInputTree((Source)object));
            }
            try {
                Builder b = context.getController().makeBuilder();
                new Sender(b.getPipelineConfiguration()).send((Source) object, b);
                return new SingletonNode(b.getCurrentRoot());
            } catch (XPathException err) {
                throw new DynamicError(err);
            }
        } else if (object instanceof DOMNodeList) {
            return ((DOMNodeList)object).getSequence();
            
        } else if (object instanceof org.w3c.dom.NodeList) {
            NodeList list = ((NodeList)object);
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
        } else if (object instanceof org.w3c.dom.Node) {
            throw new DynamicError("Supplied Java object is a non-Saxon DOM Node");
        } else {
            return new ObjectValue(object);
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
