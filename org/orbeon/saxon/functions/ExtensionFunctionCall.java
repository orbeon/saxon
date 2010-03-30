package org.orbeon.saxon.functions;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AnyItemType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.SequenceType;
import org.orbeon.saxon.value.Value;

import java.io.*;
import java.lang.reflect.*;



/**
* This class acts as a container for an extension function defined to call a method
* in a user-defined class.
 *
 * <p>Note that the binding of an XPath function call to a Java method is done in
 * class {@link org.orbeon.saxon.functions.JavaExtensionLibrary}</p>
*/

public class ExtensionFunctionCall extends FunctionCall {

    private transient AccessibleObject theMethod;
             // declared transient because AccessibleObject is not serializable
    private MethodRepresentation persistentMethod;
             // a serializable representation of the method, constructor, or field to be called
    private transient Class[] theParameterTypes;
    private PJConverter[] argumentConverters;
    private JPConverter resultConverter;
    private boolean checkForNodes;
    private Class theClass;

    /**
     * Default constructor
     */

    public ExtensionFunctionCall() {}

    /**
     * Initialization: creates an ExtensionFunctionCall
     * @param functionName the name of the function, for display purposes
     * @param theClass the Java class containing the method to be called
     * @param object the method, field, or constructor of the Java class to be called
     * @param config the Saxon configuration
    */

    public void init(StructuredQName functionName, Class theClass, AccessibleObject object, Configuration config) {
        setFunctionName(functionName);
        this.theClass = theClass;
        theMethod = object;
        theParameterTypes = null;
    }

     /**
     * preEvaluate: this method suppresses compile-time evaluation by doing nothing
     * (because the external function might have side-effects and might use the context)
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) {
        return this;
    }


    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Expression tc = super.typeCheck(visitor, contextItemType);
        if (tc != this) {
            return tc;
        }

        Configuration config = visitor.getConfiguration();
        TypeHierarchy th = config.getTypeHierarchy();
        int firstParam = 0;
        int firstArg = 0;
        if (theMethod instanceof Constructor) {
            if (theParameterTypes == null) {
                theParameterTypes = ((Constructor)theMethod).getParameterTypes();
            }
        }
        if (theMethod instanceof Method) {
            if (theParameterTypes == null) {
                theParameterTypes = ((Method)theMethod).getParameterTypes();
            }
            boolean isStatic = Modifier.isStatic(((Method)theMethod).getModifiers());
            firstArg = (isStatic ? 0 : 1);
            boolean usesContext = theParameterTypes.length > 0 &&
                (theParameterTypes[0] == XPathContext.class);
            firstParam = (usesContext ? 1 : 0);
        }
        argumentConverters = new PJConverter[argument.length];
        if (firstArg != 0) {
            SequenceType st = PJConverter.getEquivalentItemType(theClass);
            if (st != null) {
                RoleLocator role = new RoleLocator(
                    RoleLocator.FUNCTION, getFunctionName(), 0);
                argument[0] = TypeChecker.staticTypeCheck(
                                argument[0], st, false, role, visitor);
            }
            argumentConverters[0] = PJConverter.allocate(
                    config, argument[0].getItemType(th), argument[0].getCardinality(), theClass);
        }
        int j = firstParam;
        for (int i = firstArg; i < argument.length; i++) {
            SequenceType st = PJConverter.getEquivalentItemType(theParameterTypes[j]);
            if (st != null) {
                RoleLocator role = new RoleLocator(
                    RoleLocator.FUNCTION, getFunctionName(), i);
                argument[i] = TypeChecker.staticTypeCheck(
                                argument[i], st, false, role, visitor);
            }
            argumentConverters[i] = PJConverter.allocate(
                    config, argument[i].getItemType(th), argument[i].getCardinality(), theParameterTypes[j]);
            j++;
        }

        if (theMethod instanceof Constructor) {
            // Constructors always return wrapped external objects, even if the Java class
            // is one known to Saxon such as java.util.Date
            //ItemType resultType = new ExternalObjectType(theClass, config);
            //resultConverter = new JPConverter.WrapExternalObject(resultType);
            resultConverter = JPConverter.allocate(theClass, config);
        } else if (theMethod instanceof Method) {
            Class resultClass = ((Method)theMethod).getReturnType();
            resultConverter = JPConverter.allocate(resultClass, config);
        } else if (theMethod instanceof Field) {
            Class resultClass = ((Field)theMethod).getType();
            resultConverter = JPConverter.allocate(resultClass, config);
        } else {
            throw new AssertionError("Unknown component type");
        }

        ItemType resultType = resultConverter.getItemType();
        checkForNodes = resultType == AnyItemType.getInstance() || resultType instanceof NodeTest;
        resetLocalStaticProperties();
        
        return this;
    }



    /**
    * Method called by the expression parser when all arguments have been supplied
    */

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {

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
    * Determine which aspects of the context the expression depends on. The result is
    * a bitwise-or'ed value composed from constants such as XPathContext.VARIABLES and
    * XPathContext.CURRENT_NODE
    */

    public int getIntrinsicDependencies() {
        int depend = StaticProperty.HAS_SIDE_EFFECTS;
        if (theMethod instanceof Method) {
            Class[] theParameterTypes = ((Method)theMethod).getParameterTypes();
            if (theParameterTypes.length > 0 && theParameterTypes[0] == XPathContext.class) {
                depend |=
                      ( StaticProperty.DEPENDS_ON_CONTEXT_ITEM |
                        StaticProperty.DEPENDS_ON_POSITION |
                        StaticProperty.DEPENDS_ON_LAST );
            }
        }
        return depend;
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
     * @param pathMapNodeSet the PathMapNodeSet to which the paths embodied in this expression should be added
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
    * @throws org.orbeon.saxon.trans.XPathException if the function cannot be evaluated.
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        ValueRepresentation[] argValues = new ValueRepresentation[argument.length];
        for (int i=0; i<argValues.length; i++) {
            argValues[i] = ExpressionTool.lazyEvaluate(argument[i], context, 1);
        }
//        try {
            return call(argValues, context);
//        } catch (XPathException err) {
//            err.printStackTrace();
//            String msg = err.getMessage();
//            msg = "Error in call to extension function {" + theMethod.toString() + "}: " + msg;
//            XPathException err2 = new XPathException(msg, err.getException());
//            err2.setXPathContext(context);
//            err2.setLocator(this);
//            err2.setErrorCode(err.getErrorCodeNamespace(), err.getErrorCodeLocalPart());
//            throw err2;
//        }
    }

    /**
     * Get the class containing the method being called
     * @return the class containing the target method
     */

    public Class getTargetClass() {
        return theClass;
    }

    /**
     * Get the target method (or field, or constructor) being called
     * @return the target method, field, or constructor
     */

    public AccessibleObject getTargetMethod() {
        return theMethod;
    }

    /**
     * Get the types of the arguments
     * @return an array of classes representing the declared types of the arguments to the method
     * or constructor
     */

    public Class[] getParameterTypes() {
        return theParameterTypes;
    }


    /**
     * Call an extension function previously identified using the bind() method. A subclass
     * can override this method.
     * @param argValues  The values of the arguments
     * @param context The XPath dynamic evaluation context
     * @return  The value returned by the extension function
     */

    protected SequenceIterator call(ValueRepresentation[] argValues, XPathContext context) throws XPathException {

//        Class[] theParameterTypes;

        if (theMethod instanceof Constructor) {
            Constructor constructor = (Constructor) theMethod;
            if (theParameterTypes == null) { // WH
            	theParameterTypes = constructor.getParameterTypes();
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
        } else if (theMethod instanceof Method) {
            Method method = (Method) theMethod;
            boolean isStatic = Modifier.isStatic(method.getModifiers());
            Object theInstance;
            if (theParameterTypes == null) { // WH
            	theParameterTypes = method.getParameterTypes();
            }
            boolean usesContext = theParameterTypes.length > 0 &&
                    (theParameterTypes[0] == XPathContext.class);
            if (isStatic) {
                theInstance = null;
            } else {
                if (argValues.length == 0) {
                    throw new XPathException("Must supply an argument for a non-static extension function");
                }
                theInstance = getTargetInstance(argValues[0], context);
                // this fails if the first argument is not of a suitable class
            }

            Object[] params = new Object[theParameterTypes.length];

            if (usesContext) {
                params[0] = context;
            }

            setupParams(argValues, params, theParameterTypes,
                    (usesContext ? 1 : 0),
                    (isStatic ? 0 : 1),
                    context
            );

            try {
                Object result = invokeMethod(method, theInstance, params);
                //Object result = method.invoke(theInstance, params);
                //if (method.getReturnType().toString().equals("void")) {
                if (method.getReturnType() == Void.TYPE) { // WH
                    // method returns void:
                    // tried (method.getReturnType()==Void.class) unsuccessfully
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
        } else if (theMethod instanceof Field) {

            // Start of code added by GS

            Field field = (Field) theMethod;
            boolean isStatic = Modifier.isStatic(field.getModifiers());
            Object theInstance;
            if (isStatic) {
                theInstance = null;
            } else {
                if (argValues.length == 0) {
                    throw new XPathException("Must supply an argument for a non-static extension function");
                }
                theInstance = getTargetInstance(argValues[0], context);
                //Value arg0 = Value.asValue(argValues[0]);
                //theInstance = arg0.convertToJava(theClass, context);
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
            throw new AssertionError("property " + theMethod + " is neither constructor, method, nor field");
        }

    }

    private Object getTargetInstance(ValueRepresentation arg0, XPathContext context) throws XPathException {
        Value val = Value.asValue(arg0).reduce();
//        Configuration config = context.getConfiguration();
//        PJConverter converter = PJConverter.allocate(
//                config, val.getItemType(config.getTypeHierarchy()), val.getCardinality(), theClass);
        PJConverter converter = argumentConverters[0];
        return converter.convert(val, theClass, context);
        //return arg0.convertToJava(theClass, context);
    }

    /**
     * Convert the extension function result to an XPath value (a sequence) and return a
     * SequenceIterator over that sequence
     * @param result the result returned by the Java extension function
     * @param context the dynamic context
     * @return an iterator over the items in the result
     * @throws org.orbeon.saxon.trans.XPathException
     */

    private SequenceIterator asIterator(Object result, XPathContext context) throws XPathException {
        if (result == null) {
            return EmptyIterator.getInstance();
        }
        SequenceIterator resultIterator;
        if (result instanceof SequenceIterator) {
            resultIterator = (SequenceIterator)result;
        } else {
            resultIterator = Value.asIterator(resultConverter.convert(result, context));
        }
        if (checkForNodes) {
            return new ItemMappingIterator(resultIterator,
                    new ConfigurationCheckingFunction(context.getConfiguration()));
        } else {
            return resultIterator;
        }
//        if (result instanceof Value) {
//            return new ItemMappingIterator(((Value) result).iterate(),
//                    new ConfigurationCheckingFunction(context.getConfiguration()));
//        }
//        if (result instanceof NodeInfo) {
//            if (!((NodeInfo)result).getConfiguration().isCompatible(context.getConfiguration())) {
//                XPathException err = new XPathException(
//                        "NodeInfo returned by extension function was created with an incompatible Configuration");
//                err.setLocator(this);
//                err.setXPathContext(context);
//                throw err;
//            }
//            return SingletonIterator.makeIterator(((NodeInfo) result));
//        }
//        Value actual = Value.convertJavaObjectToXPath(
//                result, SequenceType.ANY_SEQUENCE, context);
//        return actual.iterate();
    }

    /**
     * Set up parameters for the Java method call
     * @param argValues the supplied XPath argument values
     * @param params the result of converting the XPath argument values to Java objects
     * @param paramTypes the Java classes defining the types of the arguments in the method signature
     * @param firstParam normally 0, but 1 if the first parameter to the Java method is an XPathContext object
     * @param firstArg normally 0, but 1 if the first argument in the XPath call is the instance object whose method
     * is to be called
     * @param context The dynamic context, giving access to a NamePool and to schema information
     * @throws org.orbeon.saxon.trans.XPathException
     */

    private void setupParams(ValueRepresentation[] argValues,
                             Object[] params,
                             Class[] paramTypes,
                             int firstParam,
                             int firstArg,
                             XPathContext context) throws XPathException {
        int j = firstParam;
        for (int i = firstArg; i < argValues.length; i++) {
            ValueRepresentation val = argValues[i];
            if (val instanceof Value) {
                val = ((Value)val).reduce();
            }
            params[j] = argumentConverters[i].convert(val, paramTypes[j], context);
            j++;
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
        //return convertClassToType(getReturnClass());
        if (resultConverter == null) {
            return AnyItemType.getInstance();
        } else {
            return resultConverter.getItemType();
        }
    }

//    /**
//     * Given the return class of the Java method, determine the return type of the XPath function
//     * @param resultClass the Java return class of the method
//     * @return the XPath return type of the extension function
//     */

//    private ItemType convertClassToType(Class resultClass) {
//        if (resultClass==null || resultClass==Value.class) {
//            return AnyItemType.getInstance();
//        } else if (resultClass.toString().equals("void")) {
//            return AnyItemType.getInstance();
//        } else if (resultClass==String.class || resultClass==StringValue.class) {
//            return BuiltInAtomicType.STRING;
//        } else if (resultClass==Boolean.class || resultClass==boolean.class || resultClass == BooleanValue.class) {
//            return BuiltInAtomicType.BOOLEAN;
//        } else if (resultClass==Double.class || resultClass==double.class || resultClass==DoubleValue.class) {
//            return BuiltInAtomicType.DOUBLE;
//        } else if (resultClass==Float.class || resultClass==float.class || resultClass==FloatValue.class) {
//            return BuiltInAtomicType.FLOAT;
//        } else if (resultClass==Long.class || resultClass==long.class ||
//                    resultClass==Int64Value.class || resultClass==BigIntegerValue.class ||
//                    resultClass==Integer.class || resultClass==int.class ||
//                    resultClass==Short.class || resultClass==short.class ||
//                    resultClass==Byte.class || resultClass==byte.class ) {
//            return BuiltInAtomicType.INTEGER;
//        } else if (resultClass == BigDecimal.class) {
//            return BuiltInAtomicType.DECIMAL;
//        } else if (resultClass == Date.class) {
//            return BuiltInAtomicType.DATE_TIME;
//        } else if (Value.class.isAssignableFrom(resultClass) ||
//                    SequenceIterator.class.isAssignableFrom(resultClass)) {
//            return AnyItemType.getInstance();
//
//        } else {
//            // Offer the object to all the registered external object models
//            List externalObjectModels = config.getExternalObjectModels();
//            for (int m=0; m<externalObjectModels.size(); m++) {
//                ExternalObjectModel model = (ExternalObjectModel)externalObjectModels.get(m);
//                if (model.isRecognizedNodeClass(resultClass) || model.isRecognizedNodeListClass(resultClass)) {
//                    return AnyNodeTest.getInstance();
//                }
//            }
//        }
//
//        if ( NodeInfo.class.isAssignableFrom(resultClass) ||
//                    Source.class.isAssignableFrom(resultClass)) {
//            return AnyNodeTest.getInstance();
//            // we could be more specific regarding the kind of node
//        } else if (List.class.isAssignableFrom(resultClass)) {
//            return AnyItemType.getInstance();
//        } else if (resultClass.isArray()) {
//            Class component = resultClass.getComponentType();
//            return convertClassToType(component);
//        } else if (resultClass == Object.class) {
//            // in this case everything is decided at run time
//            return BuiltInAtomicType.ANY_ATOMIC;
//        } else {
//            return new ExternalObjectType(resultClass, config);
//        }
//    }
//
    public int computeCardinality() {
        Class resultClass = getReturnClass();
        if (resultClass.equals(Void.TYPE)) {
                // this always returns an empty sequence, but we'll model it as
                // zero or one
                return StaticProperty.ALLOWS_ZERO_OR_ONE;
        }
        if (resultConverter == null) {
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        } else {
            return resultConverter.getCardinality();
        }
//        if (resultClass==null) {
//            // we don't know yet
//            return StaticProperty.ALLOWS_ZERO_OR_MORE;
//        }
//        if (Value.class.isAssignableFrom(resultClass) ||
//                    SequenceIterator.class.isAssignableFrom(resultClass) ||
//                    List.class.isAssignableFrom(resultClass) ||
//                    Closure.class.isAssignableFrom(resultClass)||
//                    Source.class.isAssignableFrom(resultClass) ||
//                    resultClass.isArray()) {
//            return StaticProperty.ALLOWS_ZERO_OR_MORE;
//        }
//        List models = config.getExternalObjectModels();
//        for (int m=0; m<models.size(); m++) {
//            ExternalObjectModel model = (ExternalObjectModel)models.get(m);
//            if (model.isRecognizedNodeClass(resultClass)) {
//                return StaticProperty.ALLOWS_ZERO_OR_ONE;
//            } else if (model.isRecognizedNodeListClass(resultClass)) {
//                return StaticProperty.ALLOWS_ZERO_OR_MORE;
//            }
//        }
//        if (resultClass.isPrimitive()) {
//            if (resultClass.equals(Void.TYPE)) {
//                // this always returns an empty sequence, but we'll model it as
//                // zero or one
//                return StaticProperty.ALLOWS_ZERO_OR_ONE;
//            } else {
//                // return type = int, boolean, char etc
//                return StaticProperty.EXACTLY_ONE;
//            }
//        } else {
//            return StaticProperty.ALLOWS_ZERO_OR_ONE;
//        }
    }

    /**
     * Get the Java class of the value returned by the method
     * @return the Java class of the value returned by the method
     */

    public Class getReturnClass() {
        if (theMethod instanceof Method) {
            return ((Method)theMethod).getReturnType();
        } else if (theMethod instanceof Field) {
            return ((Field)theMethod).getType();
        } else if (theMethod instanceof Constructor) {
            return theClass;
        } else {
            // cannot happen
            return null;
        }
    }

    /**
     * Get the converters used to convert the arguments from XPath values to Java values
     * @return an array of converters, one per argument
     */

    public PJConverter[] getArgumentConverters() {
        return argumentConverters;
    }

    /**
     * Get the converter used to convert the result from a Java object to an XPath value
     * @return the converter that is used
     */

    public JPConverter getResultConverter() {
        return resultConverter;
    }

    /**
     * Invoke a constructor. This method is provided separately so that it can be refined in a subclass.
     * For example, a subclass might perform tracing of calls, or might trap exceptions.
     * @param constructor The constructor to be invoked
     * @param params The parameters to be passed to the constructor
     * @return The object returned by the constructor
     * @throws InstantiationException if the invocation throws an InstantiationException
     * @throws IllegalAccessException if the invocation throws an IllegalAccessException
     * @throws InvocationTargetException if the invocation throws an InvocationTargetException (which happens
     * when the constructor itself throws an exception)
     */

    protected Object invokeConstructor(Constructor constructor, Object[] params)
    throws java.lang.InstantiationException,
           java.lang.IllegalAccessException,
           java.lang.reflect.InvocationTargetException {
        return constructor.newInstance(params);
    }

    /**
     * Invoke a method. This method is provided separately so that it can be refined in a subclass.
     * For example, a subclass might perform tracing of calls, or might trap exceptions.
     * @param method The method to be invoked
     * @param instance The object on which the method is to be invoked. This is set to null if the
     * method is static.
     * @param params The parameters to be passed to the method
     * @return The object returned by the method
     * @throws IllegalAccessException if the invocation throws an IllegalAccessException
     * @throws InvocationTargetException if the invocation throws an InvocationTargetException (which happens
     * when the method itself throws an exception)
     */

    protected Object invokeMethod(Method method, Object instance, Object[] params)
    throws java.lang.IllegalAccessException,
           java.lang.reflect.InvocationTargetException {
        return method.invoke(instance, params);
    }

    /**
     * Access a field. This method is provided separately so that it can be refined in a subclass.
     * For example, a subclass might perform tracing of calls, or might trap exceptions.
     * @param field The field to be retrieved
     * @param instance The object whose field is to be retrieved. This is set to null if the
     * field is static.
     * @return The value of the field
     * @throws IllegalAccessException if the invocation throws an IllegalAccessException
     */

    protected Object getField(Field field, Object instance)
    throws java.lang.IllegalAccessException {
        return field.get(instance);
    }

    /**
     * Code to handle serialization, used when compiling a stylesheet containing calls to extension functions
     * @param s the serialization output stream
     */

    private void writeObject(ObjectOutputStream s) throws IOException {
        persistentMethod = new MethodRepresentation(theClass, theMethod);
        s.defaultWriteObject();
    }

    /**
     * Code to handle deserialization, used when reading in a compiled stylesheet
     * @param s the serialization input stream
     */

    private void readObject(ObjectInputStream s) throws IOException  {
        try {
            s.defaultReadObject();
            theMethod = persistentMethod.recoverAccessibleObject();
            theParameterTypes = null;
        } catch (ClassNotFoundException cnfe) {
             throw new IOException(
                     "Cannot load a class containing extension functions used by the stylesheet: " + cnfe.getMessage());
        } catch (Exception e) {
            throw new IOException("Failed to read compiled representation of extension function call to " +
                    (theClass == null ? "*unknown class*" : theClass.getClass().getName()) +
                    ": " + e.getMessage());

        }
    }

    /**
     * Convert a name to camelCase (by removing hyphens and changing the following
     * letter to capitals)
     * @param name the name to be converted to camelCase
     * @param debug true if tracing is required
     * @param diag the output stream for diagnostic trace output
     * @return the camelCased name
     */

    public static String toCamelCase(String name, boolean debug, PrintStream diag) {
        if (name.indexOf('-') >= 0) {
            FastStringBuffer buff = new FastStringBuffer(name.length());
            boolean afterHyphen = false;
            for (int n = 0; n < name.length(); n++) {
                char c = name.charAt(n);
                if (c == '-') {
                    afterHyphen = true;
                } else {
                    if (afterHyphen) {
                        buff.append(Character.toUpperCase(c));
                    } else {
                        buff.append(c);
                    }
                    afterHyphen = false;
                }
            }
            name = buff.toString();
            if (debug) {
                diag.println("Seeking a method with adjusted name " + name);
            }
        }
        return name;
    }


    /**
     * A Java AccessibleObject is not serializable. When compiling a stylesheet that contains extension
     * functions, we therefore need to create a serializable representation of the method (or constructor
     * or field) to be called. This is provided by the class MethodRepresentation.
     */

    private static class MethodRepresentation implements Serializable {
        private Class theClass;
        private byte category;     // one of Method, Constructor, Field
        private String name;        // the name of the method or field
        private Class[] params;     // the types of the parameters to a method or constructor

        public MethodRepresentation(Class theClass, AccessibleObject obj) {
            this.theClass = theClass;
            if (obj instanceof Method) {
                category = 0;
                name = ((Method)obj).getName();
                params = ((Method)obj).getParameterTypes();
            } else if (obj instanceof Constructor) {
                category = 1;
                params = ((Constructor)obj).getParameterTypes();
            } else {
                category = 2;
                name = ((Field)obj).getName();
            }
        }

        public AccessibleObject recoverAccessibleObject() throws NoSuchMethodException, NoSuchFieldException {
            switch (category) {
                case 0:
                    return theClass.getMethod(name, params);
                case 1:
                    return theClass.getConstructor(params);
                case 2:
                    return theClass.getField(name);
                default:
                    return null;
            }
        }
    }

    /**
     * This class checks that NodeInfo objects returned by an extension function were created
     * under the right Configuration
     */

    private static class ConfigurationCheckingFunction implements ItemMappingFunction {

        private Configuration config;

        public ConfigurationCheckingFunction(Configuration config) {
            this.config = config;
        }

        /**
         * Map one item to another item.
         *
         * @param item The input item to be mapped.
         * @return either the output item, or null.
         */

        public Item map(Item item) throws XPathException {
            if (item instanceof NodeInfo && !config.isCompatible(((NodeInfo)item).getConfiguration())) {
                throw new XPathException(
                        "NodeInfo returned by extension function was created with an incompatible Configuration");
            }
            return item;
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
