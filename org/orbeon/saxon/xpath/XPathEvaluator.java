package net.sf.saxon.xpath;
import net.sf.saxon.Configuration;
import net.sf.saxon.instruct.SlotManager;
import net.sf.saxon.event.Builder;
import net.sf.saxon.event.Stripper;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.*;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.*;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
  * <p>XPathEvaluator provides a simple API for standalone XPath processing (that is,
  * executing XPath expressions in the absence of an XSLT stylesheet). It is loosely modelled
  * on the proposed org.w3c.dom.xpath.XPathEvaluator interface, though it does not
  * actually implement this interface at present.</p>
  *
  * @author Michael H. Kay
  */


public class XPathEvaluator {

    private NodeInfo contextNode = null;
    private StaticContext staticContext;
    private boolean stripSpace = false;

    /**
    * Default constructor. If this constructor is used, a source document must be subsequently
    * supplied using the setSource() method.
    */

    public XPathEvaluator() {}

    /**
    * Construct an XPathEvaluator to process a particular source document. This is equivalent to
    * using the default constructor and immediately calling setSource().
    * @param source The source document (or a specific node within it).
    */

    public XPathEvaluator(Source source) throws XPathException {
        setSource(source);
    }

    /**
    * Indicate whether all whitespace text nodes in the source document are to be
    * removed. This option has no effect unless it is called before the call on setSource(),
    * and unless the Source supplied to setSource() is a SAXSource or StreamSource.
    * @param strip True if all whitespace text nodes are to be stripped from the source document,
    * false otherwise. The default if the method is not called is false.
    */

    public void setStripSpace(boolean strip) {
        stripSpace = strip;
    }

    /**
    * Supply the document against which XPath expressions are to be executed. This
    * method must be called before preparing or executing an XPath expression.
    * Setting a new source document clears all the namespaces that have been declared.
    * @param source Any javax.xml.transform.Source object representing the document against
    * which XPath expressions will be executed. Note that a Saxon {@link net.sf.saxon.om.DocumentInfo DocumentInfo}
     * (indeed any {@link net.sf.saxon.om.NodeInfo NodeInfo})
     * can be used as a Source. To use a third-party DOM Document as a source, create an instance of
     * {@link javax.xml.transform.dom.DOMSource DOMSource} to wrap it.
    *  <p>The Source object supplied also determines the initial setting
    * of the context item. In most cases the context node will be the root of the supplied document;
    * however, if a NodeInfo or DOMSource is supplied it can be any node in the document. </p>
    * @return the NodeInfo of the start node in the resulting document object.
    */

    public NodeInfo setSource(Source source) throws XPathException {
        NamePool pool;
        if (source instanceof NodeInfo) {
            pool = ((NodeInfo)source).getNamePool();
        } else {
            pool = NamePool.getDefaultNamePool();
        }
        Stripper stripper = null;
        if (stripSpace) {
            stripper = AllElementStripper.getInstance();
        }
        Configuration config = new Configuration();
        config.setNamePool(pool);
        contextNode = Builder.build(source, stripper, config);
        //document = contextNode.getDocumentRoot();
        staticContext = new StandaloneContext(config);
        return contextNode;
    }

    /**
    * Set the static context for compiling XPath expressions. This provides control over the
    * environment in which the expression is compiled, for example it allows namespace prefixes to
    * be declared, variables to be bound and functions to be defined. For most purposes, the static
    * context can be defined by providing and tailoring an instance of the StandaloneContext class.
    * Until this method is called, a default static context is used, in which no namespaces are defined
    * other than the standard ones (xml, xslt, and saxon), and no variables or functions (other than the
    * core XPath functions) are available.
    */

    public void setStaticContext(StaticContext context) {
        staticContext = context;
    }

    /**
    * Get the current static context
    */

    public StaticContext getStaticContext() {
        return staticContext;
    }

    /**
    * Prepare an XPath expression for subsequent evaluation. The prepared expression can only
    * be used with the document that has been established using setSource() at the time this method
    * is called.
    * @param expression The XPath expression to be evaluated, supplied as a string.
    * @return an XPathExpression object representing the prepared expression
    * @throws XPathException if the syntax of the expression is wrong, or if it references namespaces,
    * variables, or functions that have not been declared.
    */

    public XPathExpression createExpression(String expression)
    throws XPathException {
        Expression exp = ExpressionTool.make(expression, staticContext,0,-1,1);
        exp = exp.analyze(staticContext, Type.ITEM_TYPE);
        SlotManager map = staticContext.getConfiguration().makeSlotManager();
        ExpressionTool.allocateSlots(exp, 1, null);
        XPathExpression xpe = new XPathExpression(exp, staticContext.getConfiguration());
        xpe.setStackFrameMap(map);
        xpe.setContextNode(contextNode);
        return xpe;
    }

    /**
    * Set the context node. This provides the context node for any expressions executed after this
    * method is called, including expressions that were prepared before it was called.
    * @param node The node to be used as the context node. This must
    * be a node within the context document (the document supplied using the setSource() method).
    * @throws NullPointerException if the argument is null
    * @throws IllegalArgumentException if the supplied node is not a node in the context document
    */

    public void setContextNode(NodeInfo node) {
        if (node==null) {
            throw new NullPointerException("Context node cannot be null");
        }
        contextNode = node;
    }

    /**
    * Prepare and execute an XPath expression, supplied as a string, and returning the results
    * as a List.
    * @param expression The XPath expression to be evaluated, supplied as a string.
    * @return The results of the expression, as a List. The List represents the sequence
    * of items returned by the expression. Each item in the list will either be an object
    *  representing a node, or a Java object representing an atomic value.
    * The types of Java object that may be included in the list, and the XML Schema data types that they
    * correspond to, are as follows:<p>
    * <ul>
    * <li>Boolean (xs:boolean)</li>
    * <li>String (xs:string)</li>
    * <li>BigDecimal (xs:decimal)</li>
    * <li>Long (xs:integer and its derived types)</li>
    * <li>Double (xs:double)</li>
    * <li>Float (xs:float)</li>
    * <li>Date (xs:date, xs:dateTime)</li>
    * </ul>
    */

    public List evaluate(String expression) throws XPathException {
        Expression exp = ExpressionTool.make(expression, staticContext,0,-1,1);
        exp = exp.analyze(staticContext, Type.ITEM_TYPE);
        SlotManager map = staticContext.getConfiguration().makeSlotManager();
        ExpressionTool.allocateSlots(exp, 1, map);
        XPathContextMajor context = new XPathContextMajor(contextNode, staticContext.getConfiguration());
        context.openStackFrame(map);
        SequenceIterator iterator = exp.iterate(context);
        ArrayList list = new ArrayList(20);
        while (true) {
            Item item = iterator.next();
            if (item == null) {
                return list;
            }
            list.add(convert(item));
        }
    }

    /**
     * Internal method to convert an XPath value to a Java object.
     * An atomic value is returned as an instance
     * of the best available Java class. If the item is a node, the node is "unwrapped",
     * to return the underlying node in the original model (which might be, for example,
     * a DOM or JDOM node).
    */

    public static Object convert(Item item) throws XPathException {
        if (item instanceof NodeInfo) {
            Object node = item;
            while (node instanceof VirtualNode) {
                // strip off any layers of wrapping
                node = ((VirtualNode)node).getUnderlyingNode();
            }
            return node;
        } else {
            switch (((AtomicValue)item).getItemType().getPrimitiveType()) {
                case Type.STRING:
                case Type.UNTYPED_ATOMIC:
                case Type.ANY_URI:
                case Type.DURATION:
                    return item.getStringValue();
                case Type.BOOLEAN:
                    return (((BooleanValue)item).getBooleanValue() ? Boolean.TRUE : Boolean.FALSE );
                case Type.DECIMAL:
                    return ((DecimalValue)item).getValue();
                case Type.INTEGER:
                    return new Long(((NumericValue)item).longValue());
                case Type.DOUBLE:
                    return new Double(((DoubleValue)item).getDoubleValue());
                case Type.FLOAT:
                    return new Float(((FloatValue)item).getValue());
                case Type.DATE_TIME:
                    return ((DateTimeValue)item).getUTCDate();
                case Type.DATE:
                    return ((DateValue)item).getUTCDate();
                case Type.TIME:
                    return item.getStringValue();
                case Type.BASE64_BINARY:
                    return ((Base64BinaryValue)item).getBinaryValue();
                case Type.HEX_BINARY:
                    return ((HexBinaryValue)item).getBinaryValue();
                default:
                    return item;
                    //throw new XPathException.Dynamic("Unrecognized data type: " +
                    //                                 Type.displayTypeName(item));
            }
        }
    }

    /**
    * Prepare and execute an XPath expression, supplied as a string, and returning the first
    * item in the result. This is useful where it is known that the expression will only return
    * a singleton value (for example, a single node, or a boolean).
    * @param expression The XPath expression to be evaluated, supplied as a string.
    * @return The first item in the sequence returned by the expression. If the expression
    * returns an empty sequence, this method returns null. Otherwise, it returns the first
    * item in the result sequence, represented as a Java object using the same mapping as for
    * the evaluate() method
    */

    public Object evaluateSingle(String expression) throws XPathException {
        Expression exp = ExpressionTool.make(expression, staticContext,0,-1,1);
        exp = exp.analyze(staticContext, Type.ITEM_TYPE);
        SlotManager map = staticContext.getConfiguration().makeSlotManager();
        ExpressionTool.allocateSlots(exp, 0, map);
        XPathContextMajor context = new XPathContextMajor(contextNode, staticContext.getConfiguration());
        context.openStackFrame(map);
        SequenceIterator iterator = exp.iterate(context);
        Item item = iterator.next();
        if (item == null) {
            return null;
        } else {
            return convert(item);
        }
    }

    /**
     * A simple command-line interface for the XPathEvaluator (not documented).
     * First parameter is the filename containing the source document, second
     * parameter is the XPath expression.
     */

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("format: java XPathEvaluator source.xml \"expression\"");
            System.exit(2);
        }
        XPathEvaluator xpe = new XPathEvaluator(new StreamSource(new File(args[0])));
        List results = xpe.evaluate(args[1]);
        for (int i = 0; i < results.size(); i++) {
            Object o = results.get(i);
            System.err.println(o);
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

// The Initial Developer of the Original Code is Michael H. Kay
//
// The line marked PB-SYNC is by Peter Bryant (pbryant@bigfoot.com). All Rights Reserved.
//
// Contributor(s): Michael Kay, Peter Bryant, David Megginson
//
