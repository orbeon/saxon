package org.orbeon.saxon.dotnet;

import cli.System.ArgumentException;
import cli.System.Collections.ICollection;
import cli.System.Collections.IEnumerable;
import cli.System.Collections.IEnumerator;
import cli.System.Collections.IList;
import cli.System.Decimal;
import cli.System.MethodAccessException;
import cli.System.Reflection.*;
import ikvm.lang.CIL;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.trans.Err;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.AnyNodeTest;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.value.*;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;


/**
 * This class acts as a container for an extension function defined to call a method
 * in a user-defined .NET class.
 * <p/>
 * <p>Note that the binding of an XPath function call to a .NET method is done in
 * class {@link org.orbeon.saxon.dotnet.DotNetExtensionLibrary}</p>
 */

public class DotNetExtensionFunctionCall extends FunctionCall {


    public static cli.System.Type CLI_BOOLEAN = type("System.Boolean");
    public static cli.System.Type CLI_INT64 = type("System.Int64");
    public static cli.System.Type CLI_INT32 = type("System.Int32");
    public static cli.System.Type CLI_INT16 = type("System.Int16");
    public static cli.System.Type CLI_DOUBLE = type("System.Double");
    public static cli.System.Type CLI_SINGLE = type("System.Single");
    public static cli.System.Type CLI_DECIMAL = type("System.Decimal");
    public static cli.System.Type CLI_STRING = type("System.String");
    public static cli.System.Type CLI_OBJECT = type("System.Object");
    //public static cli.System.Type CLI_XMLNODE = type("System.Xml.XmlNode");
    public static cli.System.Type CLI_ICOLLECTION = type("System.Collections.ICollection");
    public static cli.System.Type CLI_IENUMERABLE = type("System.Collections.IEnumerable");
    public static cli.System.Type CLI_ARRAYLIST = type("System.Collections.ArrayList");
    public static cli.System.Type CLI_NODEINFO = type("org.orbeon.saxon.om.NodeInfo");
    public static cli.System.Type CLI_DOCUMENTINFO = type("org.orbeon.saxon.om.DocumentInfo");
// TODO: allow extension functions to use the Saxon.Api types.
// Tried to achieve this Dec 2007, but couldn't find a way to build the software so that the callback references
// from saxon9.dll to saxon9api.dll worked properly.
//    public static cli.System.Type CLI_XDMVALUE = type("Saxon.Api.XdmValue");
//    public static cli.System.Type CLI_XDMATOMICVALUE = type("Saxon.Api.XdmAtomicValue");
//    public static cli.System.Type CLI_XDMITEM = type("Saxon.Api.XdmItem");
//    public static cli.System.Type CLI_XDMNODE = type("Saxon.Api.XdmNode");
    public static cli.System.Type CLI_ITEM = type("org.orbeon.saxon.om.Item");
    public static cli.System.Type CLI_VALUE = type("org.orbeon.saxon.value.Value");
    public static cli.System.Type CLI_SEQUENCEITERATOR = type("org.orbeon.saxon.om.SequenceIterator");

    private MemberInfo theMember;
    private ParameterInfo[] theParameterTypes;
    private cli.System.Type containingType;
    private Configuration config;

    /**
     * Default constructor
     */

    public DotNetExtensionFunctionCall() {
    }

    /**
     * Initialization: creates an ExtensionFunctionCall
     *
     * @param functionName the name of the function, for display purposes
     * @param theClass the .NET class containing the method to be called
     * @param object   the method, field, or constructor of the .NET class to be called
     * @param config   the Saxon configuration
     */

    public void init(StructuredQName functionName, cli.System.Type theClass, MemberInfo object, Configuration config) {
        setFunctionName(functionName);
        containingType = theClass;
        theMember = object;
        theParameterTypes = null;
        this.config = config;
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
     * Determine which aspects of the context the expression depends on. The result is
     * a bitwise-or'ed value composed from constants such as XPathContext.VARIABLES and
     * XPathContext.CURRENT_NODE
     */

    public int getIntrinsicDependencies() {
        int depend = StaticProperty.HAS_SIDE_EFFECTS;
        if (theMember instanceof MethodInfo) {
            ParameterInfo[] theParameterTypes = ((MethodInfo) theMember).GetParameters();
            if (theParameterTypes.length > 0 && theParameterTypes[0].get_Name().equals("XPathContext.class")) {
                depend |=
                        (StaticProperty.DEPENDS_ON_CONTEXT_ITEM |
                        StaticProperty.DEPENDS_ON_POSITION |
                        StaticProperty.DEPENDS_ON_LAST);
            }
        }
        return depend;
    }


    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        throw new UnsupportedOperationException();
    }

    /**
     * Evaluate the function. <br>
     *
     * @param context The context in which the function is to be evaluated
     * @return a Value representing the result of the function.
     * @throws org.orbeon.saxon.trans.XPathException
     *          if the function cannot be evaluated.
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        ValueRepresentation[] argValues = new ValueRepresentation[argument.length];
        for (int i = 0; i < argValues.length; i++) {
            argValues[i] = ExpressionTool.eagerEvaluate(argument[i], context);
        }
        try {
            return call(argValues, context);
        } catch (XPathException err) {
            String msg = err.getMessage();
            msg = "Error in call to extension function {" + theMember.toString() + "}: " + msg;
            XPathException err2 = new XPathException(msg, err.getException());
            err2.setXPathContext(context);
            err2.setLocator(this);
            err2.setErrorCode(err.getErrorCodeLocalPart());
            throw err2;
        }
    }

    /**
     * Get the class containing the method being called
     * @return the containing class
     */

    public cli.System.Type getTargetClass() {
        return containingType;
    }

    /**
     * Get the target method (or field, property, or constructor) being called
     * @return the target method
     */

    public MemberInfo getTargetMethod() {
        return theMember;
    }


    /**
     * Call an extension function previously identified using the bind() method. A subclass
     * can override this method.
     *
     * @param argValues The values of the arguments
     * @param context The XPath dynamic evaluation context
     * @return The value returned by the extension function
     */

    private SequenceIterator call(ValueRepresentation[] argValues, XPathContext context) throws XPathException {

//        Class[] theParameterTypes;

        if (theMember instanceof ConstructorInfo) {
            ConstructorInfo constructor = (ConstructorInfo) theMember;
            if (theParameterTypes == null) {
                theParameterTypes = constructor.GetParameters();
            }
            Object[] params = new Object[theParameterTypes.length];

            setupParams(argValues, params, theParameterTypes, 0, 0, context);

            try {
                Object result = invokeConstructor(constructor, params);
                return asIterator(result, context);
            } catch (InstantiationException err0) {
                throw new XPathException("Cannot instantiate class", err0);
            } catch (IllegalAccessException err1) {
                throw new XPathException("Constructor access is illegal", err1);
            } catch (IllegalArgumentException err2) {
                throw new XPathException("Argument is of wrong type", err2);
            } catch (NullPointerException err2) {
                throw new XPathException("Object is null");
            } catch (InvocationTargetException err3) {
                Throwable ex = err3.getTargetException();
                if (ex instanceof XPathException) {
                    throw (XPathException) ex;
                } else {
                    if (context.getController().isTracing() ||
                            context.getConfiguration().isTraceExternalFunctions()) {
                        err3.getTargetException().printStackTrace();
                    }
                    throw new XPathException("Exception in extension function: " +
                            err3.getTargetException().toString(), ex);
                }
            }
        } else if (theMember instanceof MethodInfo) {
            MethodInfo method = (MethodInfo) theMember;
            boolean isStatic = method.get_IsStatic();
            Object theInstance;
            if (theParameterTypes == null) {
                theParameterTypes = method.GetParameters();
            }
            boolean usesContext = theParameterTypes.length > 0 &&
                                theParameterTypes[0].get_ParameterType()
                                .get_FullName().equals("org.orbeon.saxon.expr.XPathContext");
            if (isStatic) {
                theInstance = null;
            } else {
                if (argValues.length == 0) {
                    throw new XPathException("Must supply an argument for a non-static extension function");
                }
                Value arg0 = Value.asValue(argValues[0]);
                theInstance = convertToDotNet(arg0, containingType, context);
                // this fails if the first argument is not of a suitable class
            }

            Object[] params = new Object[theParameterTypes.length];

            if (usesContext) {
                params[0] = context;
            }

            setupParams(argValues, params, theParameterTypes,
                    (usesContext ? 1 : 0),
                    (isStatic ? 0 : 1),
                    context);

            try {
                Object result = invokeMethod(method, theInstance, params);
                if (method.get_ReturnType() == cli.System.Type.GetType("System.Void")) {
                    // method returns void
                    return EmptyIterator.getInstance();
                }
                return asIterator(result, context);

            } catch (IllegalAccessException err1) {
                throw new XPathException("Method access is illegal", err1);
            } catch (IllegalArgumentException err2) {
                throw new XPathException("Argument is of wrong type", err2);
            } catch (NullPointerException err2) {
                throw new XPathException("Object is null", err2);
            } catch (InvocationTargetException err3) {
                Throwable ex = err3.getTargetException();
                if (ex instanceof XPathException) {
                    throw (XPathException) ex;
                } else {
                    if (context.getController().isTracing() ||
                            context.getConfiguration().isTraceExternalFunctions()) {
                        err3.getTargetException().printStackTrace();
                    }
                    throw new XPathException("Exception in extension function " +
                            err3.getTargetException().toString(), ex);
                }
            }
        } else if (theMember instanceof PropertyInfo) {

            PropertyInfo property = (PropertyInfo) theMember;
            boolean isStatic = property.GetGetMethod().get_IsStatic();
            Object theInstance;
            if (isStatic) {
                theInstance = null;
            } else {
                if (argValues.length == 0) {
                    throw new XPathException("Must supply an argument for an instance-level extension function");
                }
                Value arg0 = Value.asValue(argValues[0]);
                theInstance = convertToDotNet(arg0, containingType, context);
                // this fails if the first argument is not of a suitable class
            }

            try {
                Object result = getProperty(property, theInstance);
                return asIterator(result, context);

            } catch (IllegalAccessException err1) {
                throw new XPathException("Property access is illegal", err1);
            } catch (IllegalArgumentException err2) {
                throw new XPathException("Argument is of wrong type", err2);
            }
        } else if (theMember instanceof FieldInfo) {

            FieldInfo field = (FieldInfo) theMember;
            boolean isStatic = field.get_IsStatic();
            Object theInstance;
            if (isStatic) {
                theInstance = null;
            } else {
                if (argValues.length == 0) {
                    throw new XPathException("Must supply an argument for a non-static extension function");
                }
                Value arg0 = Value.asValue(argValues[0]);
                theInstance = convertToDotNet(arg0, containingType, context);
                // this fails if the first argument is not of a suitable class
            }

            try {
                Object result = getField(field, theInstance);
                return asIterator(result, context);

            } catch (IllegalAccessException err1) {
                throw new XPathException("Field access is illegal", err1);
            } catch (IllegalArgumentException err2) {
                throw new XPathException("Argument is of wrong type", err2);
            }
        } else {
            throw new AssertionError("property " + theMember + " is neither constructor, method, property, nor field");
        }

    }

    /**
     * Convert the extension function result to an XPath value (a sequence) and return a
     * SequenceIterator over that sequence
     *
     * @param result  the result returned by the Java extension function
     * @param context the dynamic context
     * @return an iterator over the items in the result
     * @throws org.orbeon.saxon.trans.XPathException
     *
     */

    private SequenceIterator asIterator(Object result, XPathContext context) throws XPathException {
        if (result == null) {
            return EmptyIterator.getInstance();
        }
        if (result instanceof SequenceIterator) {
            return (SequenceIterator) result;
        }
        if (result instanceof Value) {
            return ((Value) result).iterate();
        }
        if (result instanceof NodeInfo) {
            return SingletonIterator.makeIterator(((NodeInfo) result));
        }
        Value actual = convertFromDotNet(result, getReturnType(), context);
        return actual.iterate();
    }

    /**
     * Set up parameters for the Java method call
     *
     * @param argValues  the supplied XPath argument values
     * @param params     the result of converting the XPath argument values to Java objects
     * @param paramTypes the Java classes defining the types of the arguments in the method signature
     * @param firstParam normally 0, but 1 if the first parameter to the Java method is an XPathContext object
     * @param firstArg   normally 0, but 1 if the first argument in the XPath call is the instance object whose method
     *                   is to be called
     * @param context    The dynamic context, giving access to a NamePool and to schema information
     * @throws org.orbeon.saxon.trans.XPathException
     *
     */

    private void setupParams(ValueRepresentation[] argValues,
                             Object[] params,
                             ParameterInfo[] paramTypes,
                             int firstParam,
                             int firstArg,
                             XPathContext context) throws XPathException {
        int j = firstParam;
        for (int i = firstArg; i < argValues.length; i++) {
            argValues[i] = Value.asValue(argValues[i]).reduce();
            params[j] = convertToDotNet(((Value) argValues[i]), paramTypes[j].get_ParameterType(), context);
            j++;
        }
    }

    /**
     * Determine the data type of the expression, if possible. All expressions return
     * sequences, in general; this method determines the type of the items within the
     * sequence, assuming that (a) this is known in advance, and (b) it is the same for
     * all items in the sequence.
     * <p/>
     * <p>This method will always return a result, though it may be the best approximation
     * that is available at the time.</p>
     *
     * @param th type hierarchy cache
     * @return the item type
     */

    public ItemType getItemType(TypeHierarchy th) {
        return convertDotNetToXPathType(getReturnType());
    }

    private ItemType convertDotNetToXPathType(cli.System.Type resultClass) {
        if (resultClass == null) {
            return AnyItemType.getInstance();
        }
        String name = resultClass.get_FullName();
        if (name.equals("org.orbeon.saxon.value.Value")) {
            return AnyItemType.getInstance();
        } else if (resultClass.toString().equals("void")) {
            return AnyItemType.getInstance();
        } else if (name.equals("System.String") || name.equals("org.orbeon.saxon.value.StringValue")) {
            return BuiltInAtomicType.STRING;
        } else if (name.equals("System.Boolean") || name.equals("org.orbeon.saxon.value.BooleanValue")) {
            return BuiltInAtomicType.BOOLEAN;
        } else if (name.equals("System.Double") || name.equals("org.orbeon.saxon.value.DoubleValue")) {
            return BuiltInAtomicType.DOUBLE;
        } else if (name.equals("System.Single") || name.equals("org.orbeon.saxon.value.FloatValue")) {
            return BuiltInAtomicType.FLOAT;
        } else if (name.equals("System.Int64") || name.equals("System.Int64") || name.equals("System.Int32") || name.equals("System.Int16") ||
                name.equals("System.Byte") || name.equals("org.orbeon.saxon.value.IntegerValue")) {
            return BuiltInAtomicType.INTEGER;
        } else if (name.equals("System.Decimal") || name.equals("org.orbeon.saxon.value.DecimalValue")) {
            return BuiltInAtomicType.DECIMAL;
        } else if (name.equals("Saxon.Api.XdmValue") || name.equals("Saxon.Api.XdmItem")) {
            return AnyItemType.getInstance();
        } else if (name.equals("Saxon.Api.XdmNode")) {
            return AnyNodeTest.getInstance();
        } else if (name.equals("Saxon.Api.XdmAtomicValue")) {
            return BuiltInAtomicType.ANY_ATOMIC;
        } else if (name.startsWith("System.Xml.Xml")) {
            return AnyNodeTest.getInstance();
        } else if (CLI_VALUE.IsAssignableFrom(resultClass) ||
                CLI_SEQUENCEITERATOR.IsAssignableFrom(resultClass)) {
            return AnyItemType.getInstance();

        }

        if (CLI_NODEINFO.IsAssignableFrom(resultClass) /*||
                    cli.System.Type.GetType("javax.xml.transform.Source").IsAssignableFrom(resultClass)*/) {
            return AnyNodeTest.getInstance();
            // we could be more specific regarding the kind of node
        } else if (CLI_ICOLLECTION.IsAssignableFrom(resultClass)) {
            return AnyItemType.getInstance();
        } else if (resultClass.get_IsArray()) {
            cli.System.Type component = resultClass.GetElementType();
            return convertDotNetToXPathType(component);
        } else if (name.equals("System.Object")) {
            // in this case everything is decided at run time
            return BuiltInAtomicType.ANY_ATOMIC;
        } else {
            return new DotNetExternalObjectType(resultClass, config);
        }
    }

    public int computeCardinality() {
        cli.System.Type resultClass = getReturnType();
        String name = resultClass.get_FullName();
        if (resultClass == null) {
            // we don't know yet
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        }
//        cli.System.Type sourceType = cli.System.Type.GetType("javax.xml.transform.Source", true);
//        System.err.println("sourceType: " + sourceType);
        // Source is in a different assembly, it can't be directly loaded
        if (CLI_VALUE.IsAssignableFrom(resultClass) ||
                CLI_SEQUENCEITERATOR.IsAssignableFrom(resultClass) ||
                CLI_ICOLLECTION.IsAssignableFrom(resultClass) ||
                //sourceType.IsAssignableFrom(resultClass) ||
                resultClass.get_IsArray()) {
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        }

        if (resultClass.get_IsPrimitive()) {
            if (resultClass.equals(Void.TYPE)) {
                // this always returns an empty sequence, but we'll model it as
                // zero or one
                return StaticProperty.ALLOWS_ZERO_OR_ONE;
            } else {
                // return type = int, boolean, char etc
                return StaticProperty.EXACTLY_ONE;
            }
        } else if (name.equals("Saxon.Api.XdmValue")) {
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        } else {
            // Generally the result is now zero-or-one; but in the case where the return is XdmValue, it may
            // be a sequence, and we're not checking for this yet.
            return StaticProperty.ALLOWS_ZERO_OR_ONE;
        }
    }

    /**
     * Get the .NET type of the value returned by the method
     *
     * @return the .NET type of the value returned by the method
     */

    private cli.System.Type getReturnType() {
        if (theMember instanceof MethodInfo) {
            return ((MethodInfo) theMember).get_ReturnType();
        } else if (theMember instanceof FieldInfo) {
            return ((FieldInfo) theMember).get_FieldType();
        } else if (theMember instanceof ConstructorInfo) {
            return containingType;
        } else if (theMember instanceof PropertyInfo) {
            return ((PropertyInfo) theMember).GetGetMethod().get_ReturnType();
        } else {
            // cannot happen
            return null;
        }
    }

    /**
     * Invoke a constructor. This method is provided separately so that it can be refined in a subclass.
     * For example, a subclass might perform tracing of calls, or might trap exceptions.
     *
     * @param constructor The constructor to be invoked
     * @param params      The parameters to be passed to the constructor
     * @return The object returned by the constructor
     * @throws InstantiationException if the invocation throws an InstantiationException
     * @throws IllegalAccessException if the invocation throws an IllegalAccessException
     * @throws java.lang.reflect.InvocationTargetException
     *                                if the invocation throws an InvocationTargetException (which happens
     *                                when the constructor itself throws an exception)
     */

    protected Object invokeConstructor(ConstructorInfo constructor, Object[] params)
            throws InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        try {
            // dummy code to satisfy the Java compiler
            if (false) throw new MethodAccessException("");
            if (false) throw new ArgumentException("");
            if (false) throw new TargetInvocationException(null);
            if (false) throw new TargetParameterCountException("");

            // Here's the real work
            return constructor.Invoke(params);

        } catch (MethodAccessException mae) {
            throw new IllegalAccessException(mae.getMessage());
        } catch (ArgumentException ae) {
            throw new InstantiationException(ae.getMessage());
        } catch (TargetParameterCountException tpce) {
            throw new InstantiationException(tpce.getMessage());
        } catch (TargetInvocationException tpce) {
            throw new InvocationTargetException(tpce.get_InnerException());
        }
    }

    /**
     * Invoke a method. This method is provided separately so that it can be refined in a subclass.
     * For example, a subclass might perform tracing of calls, or might trap exceptions.
     *
     * @param method   The method to be invoked
     * @param instance The object on which the method is to be invoked. This is set to null if the
     *                 method is static.
     * @param params   The parameters to be passed to the method
     * @return The object returned by the method
     * @throws IllegalAccessException if the invocation throws an IllegalAccessException
     * @throws java.lang.reflect.InvocationTargetException
     *                                if the invocation throws an InvocationTargetException (which happens
     *                                when the method itself throws an exception)
     */

    //@SuppressWarnings({"ConstantIfStatement"})
    protected Object invokeMethod(MethodInfo method, Object instance, Object[] params)
            throws IllegalAccessException,
            InvocationTargetException {

        try {
            // dummy code to satisfy the Java compiler
            if (false) throw new TargetException("");
            if (false) throw new MethodAccessException("");
            if (false) throw new ArgumentException("");
            if (false) throw new TargetInvocationException(null);
            if (false) throw new TargetParameterCountException("");

            // Here's the real work
            return method.Invoke(instance, params);

        } catch (TargetException te) {
            throw new IllegalAccessException(te.getMessage());
        } catch (MethodAccessException mae) {
            throw new IllegalAccessException(mae.getMessage());
        } catch (ArgumentException ae) {
            throw new IllegalAccessException(ae.getMessage());
        } catch (TargetParameterCountException tpce) {
            throw new IllegalAccessException(tpce.getMessage());
        } catch (TargetInvocationException tpce) {
            throw new InvocationTargetException(tpce.get_InnerException());
        }
    }

    /**
     * Access a property. This method is provided separately so that it can be refined in a subclass.
     * For example, a subclass might perform tracing of calls, or might trap exceptions.
     *
     * @param property The property to be retrieved
     * @param instance The object whose property is to be retrieved. This is set to null if the
     *                 property is static.
     * @return The value of the property
     * @throws IllegalAccessException if the invocation throws an IllegalAccessException
     */

    //@SuppressWarnings({"ConstantIfStatement"})
    protected Object getProperty(PropertyInfo property, Object instance)
            throws IllegalAccessException {
        try {
            // dummy code to satisfy the Java compiler
            if (false) throw new cli.System.Reflection.TargetException("");
            if (false) throw new cli.System.NotSupportedException("");
            if (false) throw new cli.System.ArgumentException("");
            if (false) throw new cli.System.FieldAccessException(null);

            // Here's the real work
            Object[] args = {};
            return property.GetGetMethod().Invoke(instance, args);

        } catch (cli.System.Reflection.TargetException te) {
            throw new IllegalAccessException(te.getMessage());
        } catch (cli.System.NotSupportedException nse) {
            throw new IllegalAccessException(nse.getMessage());
        } catch (cli.System.ArgumentException ae) {
            throw new IllegalAccessException(ae.getMessage());
        } catch (cli.System.FieldAccessException fae) {
            throw new IllegalAccessException(fae.getMessage());
        }
    }

    /**
     * Access a field. This method is provided separately so that it can be refined in a subclass.
     * For example, a subclass might perform tracing of calls, or might trap exceptions.
     *
     * @param field    The field to be retrieved
     * @param instance The object whose field is to be retrieved. This is set to null if the
     *                 field is static.
     * @return The value of the field
     * @throws IllegalAccessException if the invocation throws an IllegalAccessException
     */

    //@SuppressWarnings({"ConstantIfStatement"})
    protected Object getField(FieldInfo field, Object instance)
            throws IllegalAccessException {
        try {
            // dummy code to satisfy the Java compiler
            if (false) throw new cli.System.Reflection.TargetException("");
            if (false) throw new cli.System.NotSupportedException("");
            if (false) throw new cli.System.ArgumentException("");
            if (false) throw new cli.System.FieldAccessException(null);

            // Here's the real work
            return field.GetValue(instance);

        } catch (cli.System.Reflection.TargetException te) {
            throw new IllegalAccessException(te.getMessage());
        } catch (cli.System.NotSupportedException nse) {
            throw new IllegalAccessException(nse.getMessage());
        } catch (cli.System.ArgumentException ae) {
            throw new IllegalAccessException(ae.getMessage());
        } catch (cli.System.FieldAccessException fae) {
            throw new IllegalAccessException(fae.getMessage());
        }
    }

    /**
     * Convert an XPath value to a .NET object
     *
     * @param value      the XPath value to be converted
     * @param targetType the target type of the .NET object
     * @param context    XPath dynamic context
     * @return the .NET object
     * @throws XPathException
     */

    public static Object convertToDotNet(Value value, cli.System.Type targetType, XPathContext context)
            throws XPathException {

        // TODO: use a similar approach to the rewritten Java code (PJConverter/JPConverter)
        if (value instanceof DotNetObjectValue) {
            return ((DotNetObjectValue) value).getObject();
        }

        // Offer the object to registered external object models


        List externalObjectModels = context.getConfiguration().getExternalObjectModels();
        for (int m=0; m<externalObjectModels.size(); m++) {
            ExternalObjectModel model = (ExternalObjectModel)externalObjectModels.get(m);
            if (model instanceof DotNetObjectModel) {
                Object object = ((DotNetObjectModel)model).convertXPathValueToObject(value, targetType, context);
                if (object != null) {
                    return object;
                } else {
                    break;
                }
            }
        }


        // If there's no information about the required type, just choose something suitable

        if (targetType == CLI_OBJECT) {
            if (value.getLength() == 1) {
                Item first = value.itemAt(0);
                if (first instanceof VirtualNode) {
                    return ((VirtualNode) first).getUnderlyingNode();
                } else if (first instanceof NodeInfo) {
                    return first;
                } else {
                    if (first instanceof NumericValue) {
                        if (first instanceof Int64Value) {
                            return CIL.box_long(((Int64Value) first).longValue());
                        } else if (first instanceof DoubleValue) {
                            return CIL.box_double(((DoubleValue) first).getDoubleValue());
                        } else if (first instanceof FloatValue) {
                            return CIL.box_float(((FloatValue) first).getFloatValue());
                        } else {
                            return Decimal.Parse(first.getStringValue());
                        }
                    } else if (first instanceof BooleanValue) {
                        return CIL.box_boolean(((BooleanValue) first).getBooleanValue());
                    } else if (first instanceof StringValue) {
                        return value.getStringValue();
                    } else {
                        return value;
                    }
                }
            } else {  // value.getLength() != 1
                cli.System.Collections.IList list = new cli.System.Collections.ArrayList(20);
                return convertToDotNetList(value, list, context);
            }
        }

        if (targetType == CLI_STRING) {
            return getAtomicValue(value, targetType, context).getStringValue();
        } else if (targetType == CLI_BOOLEAN) {
            AtomicValue v = getAtomicValue(value, targetType, context);
            if (v instanceof BooleanValue) {
                return CIL.box_boolean(((BooleanValue) v).getBooleanValue());
            } else {
                fail(value, "System.Boolean");
            }
        } else if (targetType == CLI_INT64) {
            AtomicValue v = getAtomicValue(value, targetType, context);
            if (v instanceof Int64Value) {
                return CIL.box_long(((Int64Value) v).longValue());
            } else {
                fail(value, "System.Int64");
            }
        } else if (targetType == CLI_INT32) {
            AtomicValue v = getAtomicValue(value, targetType, context);
            if (v instanceof Int64Value) {
                return CIL.box_int((int) ((Int64Value) v).longValue());
            } else {
                fail(value, "System.Int32");
            }
        } else if (targetType == CLI_INT16) {
            AtomicValue v = getAtomicValue(value, targetType, context);
            if (v instanceof Int64Value) {
                return CIL.box_short((short) ((Int64Value) v).longValue());
            } else {
                fail(value, "System.Int16");
            }
        } else if (targetType == CLI_DOUBLE) {
            AtomicValue v = getAtomicValue(value, targetType, context);
            if (v instanceof NumericValue) {
                return CIL.box_double(((NumericValue) v).getDoubleValue());
            } else {
                fail(value, "System.Double");
            }
        } else if (targetType == CLI_SINGLE) {
            AtomicValue v = getAtomicValue(value, targetType, context);
            if (v instanceof NumericValue) {
                return CIL.box_float((float) ((NumericValue) v).getDoubleValue());
            } else {
                fail(value, "System.Single");
            }
        } else if (targetType == CLI_DECIMAL) {
            try {
                AtomicValue v = getAtomicValue(value, targetType, context);
                if (v instanceof DoubleValue) {
                    return new Decimal(((DoubleValue) v).getDoubleValue());
                } else if (v instanceof FloatValue) {
                    return new Decimal(((FloatValue) v).getFloatValue());
                } else if (v instanceof Int64Value) {
                    return new Decimal(((Int64Value) v).longValue());
                } else if (v instanceof BigIntegerValue) {
                    return Decimal.Parse(v.getStringValue());
                } else if (v instanceof DecimalValue) {
                    return Decimal.Parse(v.getStringValue());
                } else {
                    fail(value, "System.Decimal");
                }
            } catch (Exception e) {
                fail(value, "System.Decimal");
            }
        }

        // See if the extension function is written to accept native Saxon objects

        if (targetType.IsAssignableFrom(CLI_VALUE)) {
            return value;

//        } else if (targetType.IsAssignableFrom(CLI_XDMVALUE)) {
//            return XdmValue.Wrap(value);

        } else if (targetType.IsAssignableFrom(CLI_SEQUENCEITERATOR)) {
            return value.iterate();

        } else if (targetType.get_IsArray()) {
            cli.System.Type component = targetType.GetElementType();
            boolean convert = !(component.IsAssignableFrom(CLI_ITEM) ||
                    component.IsAssignableFrom(CLI_NODEINFO) ||
                    component.IsAssignableFrom(CLI_DOCUMENTINFO));

            Value extent = value;
            if (extent instanceof Closure) {
                extent = Value.asValue(SequenceExtent.makeSequenceExtent(extent.iterate()));
            }
            int length = extent.getLength();
            cli.System.Array array = cli.System.Array.CreateInstance(component, length);
            for (int i = 0; i < length; i++) {
                Item item = extent.itemAt(i);
                try {
                    Object obj = item;
                    if (convert) {
                        if (item instanceof AtomicValue) {
                            obj = convertToDotNet(((AtomicValue) item), CLI_OBJECT, context);
                        } else if (item instanceof VirtualNode) {
                            obj = (((VirtualNode)item).getUnderlyingNode());
                        }
                    }
                    array.SetValue(obj, i);
                } catch (IllegalArgumentException err) {
                    XPathException d = new XPathException("Item " + i + " in supplied sequence cannot be converted " +
                            "to the component type of the CLI array (" + component + ')', err);
                    d.setXPathContext(context);
                    throw d;
                }
            }
            return array;

        } else if (CLI_ICOLLECTION.IsAssignableFrom(targetType)) {
            cli.System.Collections.IList list;
            if (targetType.IsAssignableFrom(CLI_ARRAYLIST)) {
                list = new cli.System.Collections.ArrayList(100);
            } else {
                try {
                    list = (cli.System.Collections.IList) targetType.GetConstructor(null).Invoke(null);
                } catch (Exception e) {
                    XPathException de = new XPathException("Cannot instantiate collection class " + targetType);
                    de.setXPathContext(context);
                    throw de;
                }
            }
            return convertToDotNetList(value, list, context);

        } else if (targetType.IsAssignableFrom(CLI_ITEM) ||
                targetType.IsAssignableFrom(CLI_NODEINFO) ||
                targetType.IsAssignableFrom(CLI_DOCUMENTINFO)) {
            return extractSingleton(value, targetType, context);

//        } else if (targetType.IsAssignableFrom(CLI_XDMITEM) ||
//                targetType.IsAssignableFrom(CLI_XDMATOMICVALUE) ||
//                targetType.IsAssignableFrom(CLI_XDMNODE)) {
//            Item first = extractSingleton(value, CLI_ITEM, context);
//            if (targetType.IsAssignableFrom(CLI_XDMNODE) && !(first instanceof NodeInfo)) {
//                throw new XPathException(".NET method expects a node, but supplied value is atomic");
//            }
//            return XdmItem.Wrap(first);
//
//        } else if (targetType.IsAssignableFrom(CLI_XDMATOMICVALUE)) {
//            AtomicValue first = getAtomicValue(value, CLI_ITEM, context);
//            return XdmItem.Wrap(first);

        } else if (!(value instanceof AtomicValue)) {
            // try atomizing the value, unless this is an atomic value, in which case we've already tried that
            SequenceIterator it = Atomizer.getAtomizingIterator(value.iterate());
            Item first = null;
            while (true) {
                Item next = it.next();
                if (next == null) {
                    break;
                }
                if (first != null) {
                    XPathException err = new XPathException("Sequence contains more than one value; .NET method expects only one");
                    err.setXPathContext(context);
                    throw err;
                }
                first = next;
            }
            if (first == null) {
                // sequence is empty; pass a Java null
                return null;
            }
            if (targetType.IsAssignableFrom(type(first.getClass().getName()))) {
                return first;
            } else {
                return convertToDotNet(((AtomicValue) first), targetType, context);
            }
        }
        throw new XPathException("Cannot convert supplied XPath value to the required type for the extension function");
    }

    private static Item extractSingleton(Value value, cli.System.Type targetType, XPathContext context) throws XPathException {
        // try passing the first item in the sequence provided it is the only one
        SequenceIterator iter = value.iterate();
        Item first = null;
        while (true) {
            Item next = iter.next();
            if (next == null) {
                break;
            }
            if (first != null) {
                XPathException err = new XPathException("Sequence contains more than one value; .NET method expects only one");
                err.setXPathContext(context);
                throw err;
            }
            first = next;
        }
        if (first == null) {
            // sequence is empty; pass a Java null
            return null;
        }
        if (targetType.IsAssignableFrom(type(first.getClass().getName()))) {
            // covers Item and NodeInfo
            return first;
        }

        throw new XPathException("Cannot convert supplied XPath value to the required type for the extension function");
    }

    private static AtomicValue getAtomicValue(Value value, cli.System.Type targetType, XPathContext context) throws XPathException {
        if (value instanceof UntypedAtomicValue) {
            UntypedAtomicValue uav = (UntypedAtomicValue) value;
            ConversionResult result;
            if (targetType == CLI_BOOLEAN) {
                result = uav.convert(BuiltInAtomicType.BOOLEAN, true, context);
            } else if (targetType == CLI_STRING) {
                result = new StringValue(uav.getStringValue());
            } else if (targetType == CLI_INT64 || targetType == CLI_INT32 || targetType == CLI_INT16) {
                result = uav.convert(BuiltInAtomicType.INTEGER, true, context);
            } else if (targetType == CLI_DOUBLE) {
                result = uav.convert(BuiltInAtomicType.DOUBLE, true, context);
            } else if (targetType == CLI_SINGLE) {
                result = uav.convert(BuiltInAtomicType.FLOAT, true, context);
            } else if (targetType == CLI_DECIMAL) {
                result = uav.convert(BuiltInAtomicType.DECIMAL, true, context);
            } else {
                result = new StringValue(uav.getStringValue());
            }
            return result.asAtomic();
        }
        if (value instanceof AtomicValue) {
            return (AtomicValue) value;
        } else {
            SequenceIterator it = Atomizer.getAtomizingIterator(value.iterate());
            AtomicValue first = (AtomicValue) it.next();
            if (first == null) {
                return null;
            }
            Item second = it.next();
            if (second != null) {
                throw new XPathException("Supplied sequence contains more than one item, but the external function expects a single value");

            }
            // recurse to handle conversion of untyped atomic
            return getAtomicValue(first, targetType, context);
        }
    }

    private static void fail(Value value, String type) throws XPathException {
        throw new XPathException("Cannot convert value " + Err.wrap(value.getStringValue()) + " to required type " + type);
    }

    /**
     * Convert an XPath value to a .NET collection
     * @param value the value to be converted
     * @param list    an empty Collection, to which the relevant values will be added
     * @param context the evaluation context
     * @return the supplied list, with relevant values added
     * @throws XPathException
     */

    private static ICollection convertToDotNetList(Value value, IList list, XPathContext context) throws XPathException {
        SequenceIterator iter = value.iterate();
        while (true) {
            Item it = iter.next();
            if (it == null) {
                return list;
            }
            if (it instanceof AtomicValue) {
                list.Add(convertToDotNet(((AtomicValue) it), CLI_OBJECT, context));
            } else if (it instanceof VirtualNode) {
                list.Add(((VirtualNode) it).getUnderlyingNode());
            } else {
                list.Add(it);
            }
        }
    }


    public static cli.System.Type type(java.lang.String name) {
        cli.System.Type t = cli.System.Type.GetType(name);
        if (t == null) {
            System.err.println("** Failed to load type " + name);
            t = cli.System.Type.GetType("System.Object");
        }
        return t;
    }

    /**
     * Convert the result of a .NET extension function call to an XPath value
     * @param result the result returned by the call
     * @param returnType the declared result type
     * @param context the XPath dynamic evaluation context
     * @return the converted value
     */

    private static Value convertFromDotNet(Object result, cli.System.Type returnType, XPathContext context)
    throws XPathException {

        // Offer the object to all the registered external object models

        Configuration config = context.getConfiguration();
        List externalObjectModels = config.getExternalObjectModels();
        for (int m=0; m<externalObjectModels.size(); m++) {
            ExternalObjectModel model = (ExternalObjectModel)externalObjectModels.get(m);
            if (model instanceof DotNetObjectModel) {
                Value val = ((DotNetObjectModel)model).convertObjectToXPathValue(result, config);
                if (val != null) {
                    // Note: at this point the Java code checks that the value is consistent with the required type
                    return val;
                }
            }
        }

        // Handle return types that need unboxing
        
        if (returnType == CLI_BOOLEAN) {
            return BooleanValue.get(CIL.unbox_boolean(result));
        } else if (returnType == CLI_INT64) {
            return new Int64Value(CIL.unbox_long(result));
        } else if (returnType == CLI_INT32) {
            return new Int64Value(CIL.unbox_int(result));
        } else if (returnType == CLI_INT16) {
            return new Int64Value(CIL.unbox_short(result));
        } else if (returnType == CLI_DOUBLE) {
            return new DoubleValue(CIL.unbox_double(result));
        } else if (returnType == CLI_SINGLE) {
            return new FloatValue(CIL.unbox_float(result));
        }

        if (result instanceof String) {
            return new StringValue((String) result);
        } else if (result instanceof Boolean) {
            return BooleanValue.get(((Boolean) result).booleanValue());
        } else if (result instanceof Long) {
            return new Int64Value(((Long) result).longValue());
        } else if (result instanceof Integer) {
            return new Int64Value(((Integer) result).longValue());
        } else if (result instanceof Short) {
            return new Int64Value(((Short) result).longValue());
        } else if (result instanceof Double) {
            return new DoubleValue(((Double) result).doubleValue());
        } else if (result instanceof Float) {
            return new FloatValue((float) ((Float) result).doubleValue());
        } else if (result instanceof Decimal) {
            return (AtomicValue)DecimalValue.makeDecimalValue(((Decimal) result).toString(), true);

        } else if (returnType.get_IsArray()) {
            cli.System.Type elementType = returnType.GetElementType();
            Object[] array = (Object[]) result;
            Item[] values = new Item[array.length];
            for (int i = 0; i < array.length; i++) {
                values[i] = (Item) convertFromDotNet(array[i], elementType, context);
            }
            return new SequenceExtent(values);
        } else if (CLI_IENUMERABLE.IsAssignableFrom(returnType)) {
            // the difference here is we don't know the expected type of the elements
            IEnumerator enumerator = ((IEnumerable) result).GetEnumerator();
            List values = new ArrayList(20);
            while (enumerator.MoveNext()) {
                values.add(convertFromDotNet(enumerator.get_Current(), CLI_OBJECT, context));
            }
            return new SequenceExtent(values);
        } else {
            return new DotNetObjectValue(result);
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
// Contributor(s): Gunther Schadow (changes to allow access to public fields; also wrapping
// of extensions and mapping of null to empty sequence).
// Contributor(s): Wolfgang Hoschek (performance improvement by not recomputing theParameterTypes).
//
