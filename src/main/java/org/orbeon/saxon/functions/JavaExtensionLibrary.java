package org.orbeon.saxon.functions;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ExternalObjectType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.*;

import java.io.PrintStream;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * The JavaExtensionLibrary is a FunctionLibrary that binds XPath function calls to
 * calls on Java methods (or constructors, or fields). It performs a mapping from
 * the namespace URI of the function to the Java class (the mapping is partly table
 * driven and partly algorithmic), and maps the local name of the function to the
 * Java method, constructor, or field within the class. If the Java methods are
 * polymorphic, then it tries to select the appropriate method based on the static types
 * of the supplied arguments. Binding is done entirely at XPath compilation time.
 */

public class JavaExtensionLibrary implements FunctionLibrary {

    private Configuration config;

    // HashMap containing URI->Class mappings. This includes conventional
    // URIs such as the Saxon and EXSLT namespaces, and mapping defined by
    // the user using saxon:script

    private HashMap explicitMappings = new HashMap(10);

    // Output destination for debug messages. At present this cannot be configured.

    private transient PrintStream diag = System.err;

    // flag to indicate that only the "java:" URI format is recognized

    private boolean strictUriFormat = false;

    /**
     * Construct a JavaExtensionLibrary and establish the default uri->class mappings.
     * @param config The Saxon configuration
     */

    public JavaExtensionLibrary(Configuration config) {
        this.config = config;
        setDefaultURIMappings();
    }

    /**
     * Define initial mappings of "well known" namespace URIs to Java classes (this covers
     * the Saxon and EXSLT extensions). The method is protected so it can be overridden in
     * a subclass.
     */
    protected void setDefaultURIMappings() {
        declareJavaClass(NamespaceConstant.SAXON, org.orbeon.saxon.functions.Extensions.class);
        declareJavaClass(NamespaceConstant.EXSLT_COMMON, org.orbeon.saxon.exslt.Common.class);
        declareJavaClass(NamespaceConstant.EXSLT_SETS, org.orbeon.saxon.exslt.Sets.class);
        declareJavaClass(NamespaceConstant.EXSLT_MATH, org.orbeon.saxon.exslt.Math.class);
        declareJavaClass(NamespaceConstant.EXSLT_DATES_AND_TIMES, org.orbeon.saxon.exslt.Date.class);
        declareJavaClass(NamespaceConstant.EXSLT_RANDOM, org.orbeon.saxon.exslt.Random.class);
    }

    /**
     * Indicate that only the strict "java:" URI format is to be recognized
     * @param strict true if only the strict format is recognized
     */

    public void setStrictJavaUriFormat(boolean strict) {
        strictUriFormat = strict;
    }

    /**
     * Declare a mapping from a specific namespace URI to a Java class
     * @param uri the namespace URI of the function name
     * @param theClass the Java class that implements the functions in this namespace
     */

    public void declareJavaClass(String uri, Class theClass) {
        explicitMappings.put(uri, theClass);
    }

    /**
     * Test whether an extension function with a given name and arity is available. This supports
     * the function-available() function in XSLT. This method may be called either at compile time
     * or at run time.
     * @param functionName The qualified name of the extension function
     * @param arity The number of arguments. This is set to -1 in the case of the single-argument
     * function-available() function; in this case the method should return true if there is some
     */

    public boolean isAvailable(StructuredQName functionName, int arity) {
        if (!config.isAllowExternalFunctions()) {
            return false;
        }
        Class reqClass;
        try {
            reqClass = getExternalJavaClass(functionName.getNamespaceURI());
            if (reqClass == null) {
                return false;
            }
        } catch (Exception err) {
            return false;
        }
        int significantArgs;

        Class theClass = reqClass;

        // if the method name is "new", look for a matching constructor

        String local = functionName.getLocalName();
        if ("new".equals(local)) {

            int mod = theClass.getModifiers();
            if (Modifier.isAbstract(mod)) {
                return false;
            } else if (Modifier.isInterface(mod)) {
                return false;
            } else if (Modifier.isPrivate(mod)) {
                return false;
            } else if (Modifier.isProtected(mod)) {
                return false;
            }

            if (arity == -1) return true;

            Constructor[] constructors = theClass.getConstructors();
            for (int c = 0; c < constructors.length; c++) {
                Constructor theConstructor = constructors[c];
                if (theConstructor.getParameterTypes().length == arity) {
                    return true;
                }
            }
            return false;
        } else {

            // convert any hyphens in the name, camelCasing the following character

            String name = ExtensionFunctionCall.toCamelCase(local, false, diag);

            // look through the methods of this class to find one that matches the local name

            Method[] methods = theClass.getMethods();
            for (int m = 0; m < methods.length; m++) {

                Method theMethod = methods[m];
                if (theMethod.getName().equals(name) && Modifier.isPublic(theMethod.getModifiers())) {
                    if (arity == -1) return true;
                    Class[] theParameterTypes = theMethod.getParameterTypes();
                    boolean isStatic = Modifier.isStatic(theMethod.getModifiers());

                    // if the method is not static, the first supplied argument is the instance, so
                    // discount it

                    significantArgs = (isStatic ? arity : arity - 1);

                    if (significantArgs >= 0) {

                        if (theParameterTypes.length == significantArgs &&
                                (significantArgs == 0 || theParameterTypes[0] != XPathContext.class)) {
                            return true;
                        }

                        // we allow the method to have an extra parameter if the first parameter is XPathContext

                        if (theParameterTypes.length == significantArgs + 1 &&
                                theParameterTypes[0] == XPathContext.class) {
                            return true;
                        }
                    }
                }
            }

            // look through the fields of this class to find those that matches the local name

            Field[] fields = theClass.getFields();
            for (int m = 0; m < fields.length; m++) {

                Field theField = fields[m];
                if (theField.getName().equals(name) && Modifier.isPublic(theField.getModifiers())) {
                    if (arity == -1) return true;
                    boolean isStatic = Modifier.isStatic(theField.getModifiers());

                    // if the field is not static, the first supplied argument is the instance, so
                    // discount it

                    significantArgs = (isStatic ? arity : arity - 1);

                    if (significantArgs == 0) {
                        return true;
                    }
                }
            }

            return false;
        }

    }

    /**
     * Bind an extension function, given the URI and local parts of the function name,
     * and the list of expressions supplied as arguments. This method is called at compile
     * time.
     * @param functionName the qualified name of the function being called
     * @param staticArgs  The expressions supplied statically in the function call. The intention is
     * that the static type of the arguments (obtainable via getItemType() and getCardinality()) may
     * be used as part of the binding algorithm.
     * @param env the static context
     * @return An object representing the extension function to be called, if one is found;
     * null if no extension function was found matching the required name, arity, or signature.
     */

    public Expression bind(StructuredQName functionName, Expression[] staticArgs, StaticContext env)
            throws XPathException {

        boolean debug = config.isTraceExternalFunctions();
        if (!config.isAllowExternalFunctions()) {
            if (debug) {
                diag.println("Calls to extension functions have been disabled");
            }
            return null;
        }

        Class reqClass;
        Exception theException = null;
        ArrayList candidateMethods = new ArrayList(10);
        Class resultClass = null;

        try {
            reqClass = getExternalJavaClass(functionName.getNamespaceURI());
            if (reqClass == null) {
                return null;
            }
        } catch (Exception err) {
            throw new XPathException("Cannot load external Java class", err);
        }

        if (debug) {
            diag.println("Looking for method " + functionName.getLocalName() + " in Java class " + reqClass);
            diag.println("Number of actual arguments = " + staticArgs.length);
        }

        int numArgs = staticArgs.length;
        int significantArgs;

        Class theClass = reqClass;

        // if the method name is "new", look for a matching constructor

        if ("new".equals(functionName.getLocalName())) {

            if (debug) {
                diag.println("Looking for a constructor");
            }

            int mod = theClass.getModifiers();
            if (Modifier.isAbstract(mod)) {
                theException = new XPathException("Class " + theClass + " is abstract");
            } else if (Modifier.isInterface(mod)) {
                theException = new XPathException(theClass + " is an interface");
            } else if (Modifier.isPrivate(mod)) {
                theException = new XPathException("Class " + theClass + " is private");
            } else if (Modifier.isProtected(mod)) {
                theException = new XPathException("Class " + theClass + " is protected");
            }

            if (theException != null) {
                if (debug) {
                    diag.println("Cannot construct an instance: " + theException.getMessage());
                }
                return null;
            }

            Constructor[] constructors = theClass.getConstructors();
            for (int c = 0; c < constructors.length; c++) {
                Constructor theConstructor = constructors[c];
                if (debug) {
                    diag.println("Found a constructor with " + theConstructor.getParameterTypes().length + " arguments");
                }
                if (theConstructor.getParameterTypes().length == numArgs) {
                    candidateMethods.add(theConstructor);
                }
            }
            if (candidateMethods.isEmpty()) {
                theException = new XPathException("No constructor with " + numArgs +
                        (numArgs == 1 ? " parameter" : " parameters") +
                        " found in class " + theClass.getName());
                if (debug) {
                    diag.println(theException.getMessage());
                }
                return null;
            }
        } else {

            // convert any hyphens in the name, camelCasing the following character

            String name = ExtensionFunctionCall.toCamelCase(functionName.getLocalName(), debug, diag);

            // look through the methods of this class to find one that matches the local name

            Method[] methods = theClass.getMethods();
            boolean consistentReturnType = true;
            for (int m = 0; m < methods.length; m++) {

                Method theMethod = methods[m];

                if (debug) {
                    if (theMethod.getName().equals(name)) {
                        diag.println("Trying method " + theMethod.getName() + ": name matches");
                        if (!Modifier.isPublic(theMethod.getModifiers())) {
                            diag.println(" -- but the method is not public");
                        }
                    } else {
                        diag.println("Trying method " + theMethod.getName() + ": name does not match");
                    }
                }

                if (theMethod.getName().equals(name) &&
                        Modifier.isPublic(theMethod.getModifiers())) {

                    if (consistentReturnType) {
                        if (resultClass == null) {
                            resultClass = theMethod.getReturnType();
                        } else {
                            consistentReturnType =
                                    (theMethod.getReturnType() == resultClass);
                        }
                    }
                    Class[] theParameterTypes = theMethod.getParameterTypes();
                    boolean isStatic = Modifier.isStatic(theMethod.getModifiers());

                    // if the method is not static, the first supplied argument is the instance, so
                    // discount it

                    if (debug) {
                        diag.println("Method is " + (isStatic ? "" : "not ") + "static");
                    }

                    significantArgs = (isStatic ? numArgs : numArgs - 1);

                    if (significantArgs >= 0) {

                        if (debug) {
                            if (isStatic) {
                                diag.println("Method has " + theParameterTypes.length + " argument" +
                                        (theParameterTypes.length == 1 ? "" : "s") +
                                        "; expecting " + significantArgs);
                            } else {
                                diag.println("Method has " + theParameterTypes.length + " argument" +
                                        (theParameterTypes.length == 1 ? "" : "s") +
                                        "; expecting " + numArgs + " plus one for the target object");
                            }
                        }

                        if (theParameterTypes.length == significantArgs &&
                                (significantArgs == 0 || theParameterTypes[0] != XPathContext.class)) {
                            if (debug) {
                                diag.println("Found a candidate method:");
                                diag.println("    " + theMethod);
                            }
                            candidateMethods.add(theMethod);
                        }

                        // we allow the method to have an extra parameter if the first parameter is XPathContext

                        if (theParameterTypes.length == significantArgs + 1 &&
                                theParameterTypes[0] == XPathContext.class) {
                            if (debug) {
                                diag.println("Method is a candidate because first argument is XPathContext");
                            }
                            candidateMethods.add(theMethod);
                        }
                    }
                }
            }

            // Code added by GS -- start

            // look through the fields of this class to find those that matches the local name

            Field[] fields = theClass.getFields();
            for (int m = 0; m < fields.length; m++) {

                Field theField = fields[m];

                if (debug) {
                    if (theField.getName().equals(name)) {
                        diag.println("Trying field " + theField.getName() + ": name matches");
                        if (!Modifier.isPublic(theField.getModifiers())) {
                            diag.println(" -- but the field is not public");
                        }
                    } else {
                        diag.println("Trying field " + theField.getName() + ": name does not match");
                    }
                }

                if (theField.getName().equals(name) &&
                        Modifier.isPublic(theField.getModifiers())) {
                    if (consistentReturnType) {
                        if (resultClass == null) {
                            resultClass = theField.getType();
                        } else {
                            consistentReturnType =
                                    (theField.getType() == resultClass);
                        }
                    }
                    boolean isStatic = Modifier.isStatic(theField.getModifiers());

                    // if the field is not static, the first supplied argument is the instance, so
                    // discount it

                    if (debug) {
                        diag.println("Field is " + (isStatic ? "" : "not ") + "static");
                    }

                    significantArgs = (isStatic ? numArgs : numArgs - 1);

                    if (significantArgs == 0) {
                        if (debug) {
                            diag.println("Found a candidate field:");
                            diag.println("    " + theField);
                        }
                        candidateMethods.add(theField);
                    }
                }
            }

            // End of code added by GS

            // No method found?

            if (candidateMethods.isEmpty()) {
                theException = new XPathException("No method or field matching " + name +
                        " with " + numArgs +
                        (numArgs == 1 ? " parameter" : " parameters") +
                        " found in class " + theClass.getName());
                if (debug) {
                    diag.println(theException.getMessage());
                }
                return null;
            }
        }
        if (candidateMethods.isEmpty()) {
            if (debug) {
                diag.println("There is no suitable method matching the arguments of function "
                        + functionName.getLocalName());
            }
            return null;
        }
        AccessibleObject method = getBestFit(candidateMethods, staticArgs, theClass);
        if (method == null) {
            if (candidateMethods.size() > 1) {
                // There was more than one candidate method, and we can't decide which to use.
                // This may be because insufficient type information is available at this stage.
                // Return an UnresolvedExtensionFunction, and try to resolve it later when more
                // type information is known.
                return new UnresolvedExtensionFunction(functionName, theClass, candidateMethods, staticArgs);
            }
            return null;
        } else {
            JavaExtensionFunctionFactory factory =
                    (JavaExtensionFunctionFactory)config.getExtensionFunctionFactory("java");
            return factory.makeExtensionFunctionCall(functionName, theClass, method, staticArgs);
        }
    }


    /**
     * Get the best fit amongst all the candidate methods, constructors, or fields, based on the static types
     * of the supplied arguments
     * @param candidateMethods a list of all the methods, fields, and constructors that match the extension
     * function call in name and arity (but not necessarily in the types of the arguments)
     * @param args the expressions supplied as arguments.
     * @param theClass the class that implements the extension function
     * @return the result is either a Method or a Constructor or a Field, or null if no unique best fit
     * method could be found.
     */

    private AccessibleObject getBestFit(List candidateMethods, Expression[] args, Class theClass) {
        boolean debug = config.isTraceExternalFunctions();
        int candidates = candidateMethods.size();

        if (candidates == 1) {
            // short cut: there is only one candidate method
            return (AccessibleObject) candidateMethods.get(0);

        } else {
            // choose the best fit method or constructor or field
            // for each pair of candidate methods, eliminate either or both of the pair
            // if one argument is less-preferred

            if (debug) {
                diag.println("Finding best fit method for arguments");
            }

            boolean eliminated[] = new boolean[candidates];
            for (int i = 0; i < candidates; i++) {
                eliminated[i] = false;
            }

            if (debug) {
                for (int i = 0; i < candidates; i++) {
                    int[] pref_i = getConversionPreferences(
                            args, (AccessibleObject)candidateMethods.get(i), theClass);
                    diag.println("Trying option " + i + ": " + candidateMethods.get(i).toString());
                    if (pref_i == null) {
                        diag.println("Arguments cannot be converted to required types");
                    } else {
                        String prefs = "[";
                        for (int p = 0; p < pref_i.length; p++) {
                            if (p != 0) prefs += ", ";
                            prefs += pref_i[p];
                        }
                        prefs += "]";
                        diag.println("Conversion preferences are " + prefs);
                    }
                }
            }

            for (int i = 0; i < candidates; i++) {
                int[] pref_i = getConversionPreferences(
                        args,
                        (AccessibleObject) candidateMethods.get(i), theClass);

                if (pref_i == null) {
                    eliminated[i] = true;
                }
                if (!eliminated[i]) {
                    for (int j = i + 1; j < candidates; j++) {
                        if (!eliminated[j]) {
                            int[] pref_j = getConversionPreferences(
                                    args,
                                    (AccessibleObject) candidateMethods.get(j), theClass);
                            if (pref_j == null) {
                                eliminated[j] = true;
                            } else {
                                for (int k = 0; k < pref_j.length; k++) {
                                    if (pref_i[k] > pref_j[k] && !eliminated[i]) { // high number means less preferred
                                        eliminated[i] = true;
                                        if (debug) {
                                            diag.println("Eliminating option " + i);
                                        }
                                    }
                                    if (pref_i[k] < pref_j[k] && !eliminated[j]) {
                                        eliminated[j] = true;
                                        if (debug) {
                                            diag.println("Eliminating option " + j);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            int remaining = 0;
            AccessibleObject theMethod = null;
            for (int r = 0; r < candidates; r++) {
                if (!eliminated[r]) {
                    theMethod = (AccessibleObject) candidateMethods.get(r);
                    remaining++;
                }
            }

            if (debug) {
                diag.println("Number of candidate methods remaining: " + remaining);
            }

            if (remaining == 0) {
                if (debug) {
                    diag.println("There are " + candidates +
                            " candidate Java methods matching the function name, but none is a unique best match");
                }
                return null;
            }

            if (remaining > 1) {
                if (debug) {
                    diag.println("There are several Java methods that match the function name equally well");
                }
                return null;
            }

            return theMethod;
        }
    }

    /**
     * Get an array of integers representing the conversion distances of each "real" argument
     * to a given method
     * @param args the actual expressions supplied in the function call
     * @param method  the method or constructor.
     * @param theClass  the class that implements the extension function
     * @return an array of integers, one for each argument, indicating the conversion
     * distances. A high number indicates low preference. If any of the arguments cannot
     * be converted to the corresponding type defined in the method signature, return null.
     */

    private int[] getConversionPreferences(Expression[] args, AccessibleObject method, Class theClass) {

        Class[] params;
        int firstArg;
        TypeHierarchy th = config.getTypeHierarchy();
        if (method instanceof Constructor) {
            firstArg = 0;
            params = ((Constructor) method).getParameterTypes();
        } else if (method instanceof Method) {
            boolean isStatic = Modifier.isStatic(((Method) method).getModifiers());
            firstArg = (isStatic ? 0 : 1);
            params = ((Method) method).getParameterTypes();
        } else if (method instanceof Field) {
            boolean isStatic = Modifier.isStatic(((Field) method).getModifiers());
            firstArg = (isStatic ? 0 : 1);
            params = NO_PARAMS;
        } else {
            throw new AssertionError("property " + method + " was neither constructor, method, nor field");
        }

        int noOfArgs = args.length;
        int preferences[] = new int[noOfArgs];
        int firstParam = 0;

        if (params.length > 0 && params[0] == XPathContext.class) {
            firstParam = 1;
        }
        for (int i = firstArg; i < noOfArgs; i++) {
            preferences[i] = getConversionPreference(th, args[i], params[i + firstParam - firstArg]);
            if (preferences[i] == -1) {
                return null;
            }
        }

        if (firstArg == 1) {
            preferences[0] = getConversionPreference(th, args[0], theClass);
            if (preferences[0] == -1) {
                return null;
            }
        }

        return preferences;
    }

    /**
     * Get the conversion preference from a given XPath type to a given Java class
     * @param th the type hierarchy cache
     * @param arg the supplied XPath expression (the static type of this expression
     * is used as input to the algorithm)
     * @param required the Java class of the relevant argument of the Java method
     * @return the conversion preference. A high number indicates a low preference;
     * -1 indicates that conversion is not possible.
     */

    private int getConversionPreference(TypeHierarchy th, Expression arg, Class required) {
        ItemType itemType = arg.getItemType(th);
        int cardinality = arg.getCardinality();
        if (required == Object.class) {
            return 100;
        } else if (Cardinality.allowsMany(cardinality)) {
            if (required.isAssignableFrom(SequenceIterator.class)) {
                return 20;
            } else if (required.isAssignableFrom(Value.class)) {
                return 21;
            } else if (Collection.class.isAssignableFrom(required)) {
                return 22;
            } else if (required.isArray()) {
                return 24;
                // sort out at run-time whether the component type of the array is actually suitable
            } else {
                return 80;    // conversion possible only if external object model supports it
            }
        } else {
            if (Type.isNodeType(itemType)) {
                if (required.isAssignableFrom(NodeInfo.class)) {
                    return 20;
                } else if (required.isAssignableFrom(DocumentInfo.class)) {
                    return 21;
                } else {
                    return 80;
                }
            } else if (itemType instanceof ExternalObjectType) {
                Class ext = ((ExternalObjectType)itemType).getJavaClass();
                if (required.isAssignableFrom(ext)) {
                    return 10;
                } else {
                    return -1;
                }
            } else {
                int primitiveType = itemType.getPrimitiveType();
                return atomicConversionPreference(primitiveType, required);
            }
        }
    }

    private static final Class[] NO_PARAMS = new Class[0];


    /**
     * Get the conversion preference from an XPath primitive atomic type to a Java class
     * @param primitiveType integer code identifying the XPath primitive type, for example
     * {@link StandardNames#XS_STRING} or {@link StandardNames#XS_STRING}
     * @param required The Java Class named in the method signature
     * @return an integer indicating the relative preference for converting this primitive type
     * to this Java class. A high number indicates a low preference. All values are in the range
     * 50 to 100. For example, the conversion of an XPath String to {@link org.orbeon.saxon.value.StringValue} is 50, while
     * XPath String to {@link java.lang.String} is 51. The value -1 indicates that the conversion is not allowed.
     */

    protected int atomicConversionPreference(int primitiveType, Class required) {
        if (required == Object.class) return 100;
        switch (primitiveType) {
            case StandardNames.XS_STRING:
                if (required.isAssignableFrom(StringValue.class)) return 50;
                if (required == String.class) return 51;
                if (required == CharSequence.class) return 51;
                return -1;
            case StandardNames.XS_DOUBLE:
                if (required.isAssignableFrom(DoubleValue.class)) return 50;
                if (required == double.class) return 50;
                if (required == Double.class) return 51;
                return -1;
            case StandardNames.XS_FLOAT:
                if (required.isAssignableFrom(FloatValue.class)) return 50;
                if (required == float.class) return 50;
                if (required == Float.class) return 51;
                if (required == double.class) return 52;
                if (required == Double.class) return 53;
                return -1;
            case StandardNames.XS_DECIMAL:
                if (required.isAssignableFrom(DecimalValue.class)) return 50;
                if (required == BigDecimal.class) return 50;
                if (required == double.class) return 51;
                if (required == Double.class) return 52;
                if (required == float.class) return 53;
                if (required == Float.class) return 54;
                return -1;
            case StandardNames.XS_INTEGER:
                if (required.isAssignableFrom(Int64Value.class)) return 50;
                if (required == BigInteger.class) return 51;
                if (required == BigDecimal.class) return 52;
                if (required == long.class) return 53;
                if (required == Long.class) return 54;
                if (required == int.class) return 55;
                if (required == Integer.class) return 56;
                if (required == short.class) return 57;
                if (required == Short.class) return 58;
                if (required == byte.class) return 59;
                if (required == Byte.class) return 60;
                if (required == double.class) return 61;
                if (required == Double.class) return 62;
                if (required == float.class) return 63;
                if (required == Float.class) return 64;
                return -1;
            case StandardNames.XS_BOOLEAN:
                if (required.isAssignableFrom(BooleanValue.class)) return 50;
                if (required == boolean.class) return 51;
                if (required == Boolean.class) return 52;
                return -1;
            case StandardNames.XS_DATE:
            case StandardNames.XS_G_DAY:
            case StandardNames.XS_G_MONTH_DAY:
            case StandardNames.XS_G_MONTH:
            case StandardNames.XS_G_YEAR_MONTH:
            case StandardNames.XS_G_YEAR:
                if (required.isAssignableFrom(DateValue.class)) return 50;
                if (required.isAssignableFrom(Date.class)) return 51;
                return -1;
            case StandardNames.XS_DATE_TIME:
                if (required.isAssignableFrom(DateTimeValue.class)) return 50;
                if (required.isAssignableFrom(Date.class)) return 51;
                return -1;
            case StandardNames.XS_TIME:
                if (required.isAssignableFrom(TimeValue.class)) return 50;
                return -1;
            case StandardNames.XS_DURATION:
            case StandardNames.XS_YEAR_MONTH_DURATION:
            case StandardNames.XS_DAY_TIME_DURATION:
                if (required.isAssignableFrom(DurationValue.class)) return 50;
                return -1;
            case StandardNames.XS_ANY_URI:
                if (required.isAssignableFrom(AnyURIValue.class)) return 50;
                if (required == URI.class) return 51;
                if (required == URL.class) return 52;
                if (required == String.class) return 53;
                if (required == CharSequence.class) return 53;
                return -1;
            case StandardNames.XS_QNAME:
                if (required.isAssignableFrom(QualifiedNameValue.class)) return 50;
                //if (required.isAssignableFrom(QName.class)) return 51;
                // TODO: reinstate above line under JDK 1.5
                if (required.getClass().getName().equals("javax.xml.namespace.QName")) return 51;
                return -1;
            case StandardNames.XS_BASE64_BINARY:
                if (required.isAssignableFrom(Base64BinaryValue.class)) return 50;
                return -1;
            case StandardNames.XS_HEX_BINARY:
                if (required.isAssignableFrom(HexBinaryValue.class)) return 50;
                return -1;
            case StandardNames.XS_UNTYPED_ATOMIC:
                return 50;
            default:
                return -1;
        }
    }

    /**
     * Get an external Java class corresponding to a given namespace prefix, if there is
     * one.
     * @param uri The namespace URI corresponding to the prefix used in the function call.
     * @return the Java class name if a suitable class exists, otherwise return null.
     */

    private Class getExternalJavaClass(String uri) {

        // First see if an explicit mapping has been registered for this URI

        Class c = (Class) explicitMappings.get(uri);
        if (c != null) {
            return c;
        }

        // Failing that, try to identify a class directly from the URI

        try {

            // support the URN format java:full.class.Name

            if (uri.startsWith("java:")) {
                return config.getClass(uri.substring(5), config.isTraceExternalFunctions(), null);
            }

            if (strictUriFormat) {
                return null;
            } else {

                // extract the class name as anything in the URI after the last "/"
                // if there is one, or the whole class name otherwise

                int slash = uri.lastIndexOf('/');
                if (slash < 0) {
                    return config.getClass(uri, config.isTraceExternalFunctions(), null);
                } else if (slash == uri.length() - 1) {
                    return null;
                } else {
                    return config.getClass(uri.substring(slash + 1), config.isTraceExternalFunctions(), null);
                }
            }
        } catch (XPathException err) {
            return null;
        }
    }

    /**
     * Inner class representing an unresolved extension function call. This arises when there is insufficient
     * static type information available at the time the function call is parsed to determine which of several
     * candidate Java methods to invoke. The function call cannot be executed; it must be resolved to an
     * actual Java method during the analysis phase.
     */

    private class UnresolvedExtensionFunction extends CompileTimeFunction {

        private List candidateMethods;
        private Class theClass;


        public UnresolvedExtensionFunction(StructuredQName functionName, Class theClass,
                                           List candidateMethods, Expression[] staticArgs) {
            setArguments(staticArgs);
            setFunctionName(functionName);
            this.theClass = theClass;
            this.candidateMethods = candidateMethods;
        }

        /**
         * Type-check the expression.
         */

        public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
            for (int i=0; i<argument.length; i++) {
                Expression exp = visitor.typeCheck(argument[i], contextItemType);
                if (exp != argument[i]) {
                    adoptChildExpression(exp);
                    argument[i] = exp;
                }
            }
            AccessibleObject method = getBestFit(candidateMethods, argument, theClass);
            if (method == null) {
                XPathException err = new XPathException("There is more than one method matching the function call " +
                        getFunctionName().getDisplayName() +
                        ", and there is insufficient type information to determine which one should be used");
                err.setLocator(this);
                throw err;
            } else {
                JavaExtensionFunctionFactory factory =
                        (JavaExtensionFunctionFactory)config.getExtensionFunctionFactory("java");
                Expression call = factory.makeExtensionFunctionCall(getFunctionName(), theClass, method, argument);
                ExpressionTool.copyLocationInfo(this, call);
                return call.typeCheck(visitor, contextItemType);
            }
        }
    }

    /**
     * This method creates a copy of a FunctionLibrary: if the original FunctionLibrary allows
     * new functions to be added, then additions to this copy will not affect the original, or
     * vice versa.
     *
     * @return a copy of this function library. This must be an instance of the original class.
     */

    public FunctionLibrary copy() {
        JavaExtensionLibrary jel = new JavaExtensionLibrary(config);
        jel.explicitMappings = new HashMap(explicitMappings);
        jel.diag = diag;
        return jel;
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