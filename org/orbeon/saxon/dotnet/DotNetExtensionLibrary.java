package org.orbeon.saxon.dotnet;

import cli.System.Reflection.*;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.ComputedExpression;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.functions.CompileTimeFunction;
import org.orbeon.saxon.functions.FunctionLibrary;
import org.orbeon.saxon.functions.JavaExtensionLibrary;
import org.orbeon.saxon.trans.StaticError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.Cardinality;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The DotNetExtensionLibrary is a FunctionLibrary that binds XPath function calls to
 * calls on .NET methods (or constructors, or properties). It performs a mapping from
 * the namespace URI of the function to the .NET assembly and type (the mapping is partly table
 * driven and partly algorithmic), and maps the local name of the function to the
 * .NET method, constructor, or property within the class. If the .NET methods are
 * polymorphic, then it tries to select the appropriate method based on the static types
 * of the supplied arguments. Binding is done entirely at XPath compilation time.
 */

// derived from JavaExtensionLibrary

// TODO: check that parameters are not Out or InOut parameters

public class DotNetExtensionLibrary implements FunctionLibrary {

    private Configuration config;

    // HashMap containing URI->Class mappings. This includes conventional
    // URIs such as the Saxon and EXSLT namespaces, and mapping defined by
    // the user using saxon:script

    private HashMap explicitMappings = new HashMap(10);

    // Output destination for debug messages. At present this cannot be configured.

    private transient PrintStream diag = System.err;

    /**
     * Construct a JavaExtensionLibrary and establish the default uri->class mappings.
     * @param config The Saxon configuration
     */

    public DotNetExtensionLibrary(Configuration config) {
        this.config = config;
        setDefaultURIMappings();
    }

    /**
     * Define initial mappings of "well known" namespace URIs to .NET classes (this covers
     * the Saxon and EXSLT extensions). The method is protected so it can be overridden in
     * a subclass.
     */
    protected void setDefaultURIMappings() {
        //declareJavaClass(NamespaceConstant.SAXON, org.orbeon.saxon.functions.Extensions.class);
    }

    /**
     * Declare a mapping from a specific namespace URI to a .NET class
     * @param uri the namespace URI of the function name
     * @param theClass the .NET class that implements the functions in this namespace
     */

    public void declareDotNetType(String uri, cli.System.Type theClass) {
        explicitMappings.put(uri, theClass);
    }

    /**
     * Test whether an extension function with a given name and arity is available. This supports
     * the function-available() function in XSLT. This method may be called either at compile time
     * or at run time.
     * @param fingerprint The code that identifies the function name in the NamePool. This must
     * match the supplied URI and local name.
     * @param uri  The URI of the function name
     * @param local  The local part of the function name
     * @param arity The number of arguments. This is set to -1 in the case of the single-argument
     * function-available() function; in this case the method should return true if there is some
     * matching extension function, regardless of its arity.
     */

    public boolean isAvailable(int fingerprint, String uri, String local, int arity) {
        if (!config.isAllowExternalFunctions()) {
            return false;
        }
        cli.System.Type reqClass;
        try {
            reqClass = getExternalDotNetType(uri);
            if (reqClass == null) {
                return false;
            }
        } catch (Exception err) {
            return false;
        }
        int significantArgs;

        cli.System.Type theClass = reqClass;

        // if the method name is "new", look for a matching constructor

        if ("new".equals(local)) {

            if (theClass.get_IsAbstract()) {
                return false;
            } else if (theClass.get_IsInterface()) {
                return false;
            } else if (!theClass.get_IsPublic()) {
                return false;
            }

            if (arity == -1) return true;

            ConstructorInfo[] constructors = theClass.GetConstructors();
            for (int c = 0; c < constructors.length; c++) {
                ConstructorInfo theConstructor = constructors[c];
                if (theConstructor.GetParameters().length == arity) {
                    return true;
                }
            }
            return false;
        } else {

            // convert any hyphens in the name, camelCasing the following character

            String name = JavaExtensionLibrary.toCamelCase(local, false, diag);

            // look through the methods of this class to find one that matches the local name

            MethodInfo[] methods = theClass.GetMethods();
            for (int m = 0; m < methods.length; m++) {

                MethodInfo theMethod = methods[m];
                if (theMethod.get_Name().equals(name) && theMethod.get_IsPublic()) {
                    if (arity == -1) {
                        return true;
                    }
                    ParameterInfo[] theParameterTypes = theMethod.GetParameters();
                    boolean isStatic = theMethod.get_IsStatic();

                    // if the method is not static, the first supplied argument is the instance, so
                    // discount it

                    significantArgs = (isStatic ? arity : arity - 1);

                    if (significantArgs >= 0) {

                        if (theParameterTypes.length == significantArgs &&
                                (significantArgs == 0 || !theParameterTypes[0].get_Name().equals("XPathContext"))) {
                            // TODO: XPathContext arguments won't work yet
                            return true;
                        }

                        // we allow the method to have an extra parameter if the first parameter is XPathContext

                        if (theParameterTypes.length == significantArgs + 1 &&
                                theParameterTypes[0].get_Name().equals("XPathContext")) {
                            return true;
                        }
                    }
                }
            }

            // look through the properties of this class to find those that match the local name

            PropertyInfo[] properties = theClass.GetProperties();
            for (int m = 0; m < properties.length; m++) {

                PropertyInfo theProperty = properties[m];
                if (theProperty.get_Name().equals(name) &&
                        theProperty.get_CanRead() && theProperty.GetGetMethod().get_IsPublic()) {
                    if (arity == -1) return true;
                    boolean isStatic = theProperty.GetGetMethod().get_IsStatic();

                    // if the property is not static, the first supplied argument is the instance, so
                    // discount it

                    significantArgs = (isStatic ? arity : arity - 1);

                    if (significantArgs == 0) {
                        return true;
                    }
                }
            }

            // look through the fields of this class to find those that match the local name

            FieldInfo[] fields = theClass.GetFields();
            for (int m = 0; m < fields.length; m++) {

                FieldInfo theField = fields[m];
                if (theField.get_Name().equals(name) && theField.get_IsPublic()) {
                    if (arity == -1) return true;
                    boolean isStatic = theField.get_IsStatic();

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
     * @param nameCode The namepool code of the function name. This must match the supplied
     * URI and local name.
     * @param uri  The URI of the function name
     * @param local  The local part of the function name
     * @param staticArgs  The expressions supplied statically in the function call. The intention is
     * that the static type of the arguments (obtainable via getItemType() and getCardinality()) may
     * be used as part of the binding algorithm.
     * @return An object representing the extension function to be called, if one is found;
     * null if no extension function was found matching the required name, arity, or signature.
     */

    public Expression bind(int nameCode, String uri, String local, Expression[] staticArgs)
            throws XPathException {

        boolean debug = config.isTraceExternalFunctions();
        if (!config.isAllowExternalFunctions()) {
            if (debug) {
                diag.println("Calls to extension functions have been disabled");
            }
            return null;
        }

        cli.System.Type requiredType;
        Exception theException = null;
        ArrayList candidateMethods = new ArrayList(10);
        cli.System.Type resultType = null;

        try {
            requiredType = getExternalDotNetType(uri);
            if (requiredType == null) {
                return null;
            }
        } catch (Exception err) {
            throw new StaticError("Cannot load external .NET type", err);
        }

        if (debug) {
            diag.println("Looking for method " + local + " in .NET type " + requiredType);
            diag.println("Number of actual arguments = " + staticArgs.length);
        }

        int numArgs = staticArgs.length;
        int significantArgs;

        cli.System.Type theType = requiredType;

        // if the method name is "new", look for a matching constructor

        if ("new".equals(local)) {

            if (debug) {
                diag.println("Looking for a constructor");
            }

            if (theType.get_IsAbstract()) {
                theException = new StaticError("Type " + theType + " is abstract");
            } else if (theType.get_IsInterface()) {
                theException = new StaticError("Type " + theType + " is an interface");
            } else if (!theType.get_IsPublic()) {
                theException = new StaticError("Type " + theType + " is not public");
            }

            if (theException != null) {
                if (debug) {
                    diag.println("Cannot construct an instance: " + theException.getMessage());
                }
                return null;
            }

            ConstructorInfo[] constructors = theType.GetConstructors();
            for (int c = 0; c < constructors.length; c++) {
                ConstructorInfo theConstructor = constructors[c];
                if (debug) {
                    diag.println("Found a constructor with " + theConstructor.GetParameters().length + " arguments");
                }
                if (theConstructor.GetParameters().length == numArgs) {
                    candidateMethods.add(theConstructor);
                }
            }
            if (candidateMethods.size() == 0) {
                theException = new StaticError("No constructor with " + numArgs +
                        (numArgs == 1 ? " parameter" : " parameters") +
                        " found in type " + theType.get_Name());
                if (debug) {
                    diag.println(theException.getMessage());
                }
                return null;
            }
        } else {

            // convert any hyphens in the name, camelCasing the following character

            String name = JavaExtensionLibrary.toCamelCase(local, debug, diag);

            // look through the methods of this class to find one that matches the local name

            MethodInfo[] methods = theType.GetMethods();
            boolean consistentReturnType = true;
            for (int m = 0; m < methods.length; m++) {

                MethodInfo theMethod = methods[m];

                if (debug) {
                    if (theMethod.get_Name().equals(name)) {
                        diag.println("Trying method " + theMethod.get_Name() + ": name matches");
                        if (!theMethod.get_IsPublic()) {
                            diag.println(" -- but the method is not public");
                        }
                    } else {
                        diag.println("Trying method " + theMethod.get_Name() + ": name does not match");
                    }
                }

                if (theMethod.get_Name().equals(name) && theMethod.get_IsPublic()) {

                    if (consistentReturnType) {
                        if (resultType == null) {
                            resultType = theMethod.get_ReturnType();
                        } else {
                            consistentReturnType =
                                    (theMethod.get_ReturnType() == resultType);
                        }
                    }
                    ParameterInfo[] theParameterTypes = theMethod.GetParameters();
                    boolean isStatic = theMethod.get_IsStatic();

                    // if the method is not static, the first supplied argument is the instance, so
                    // discount it

                    if (debug) {
                        diag.println("Method is " + (isStatic ? "" : "not ") + "static");
                    }

                    significantArgs = (isStatic ? numArgs : numArgs - 1);

                    if (significantArgs >= 0) {

                        if (debug) {
                            diag.println("Method has " + theParameterTypes.length + " argument" +
                                    (theParameterTypes.length == 1 ? "" : "s") +
                                    "; expecting " + significantArgs);
                        }

                        if (theParameterTypes.length == significantArgs &&
                                (significantArgs == 0 || !theParameterTypes[0].get_Name().equals("XPathContext"))) {
                            if (debug) {
                                diag.println("Found a candidate method:");
                                diag.println("    " + theMethod);
                            }
                            candidateMethods.add(theMethod);
                        }

                        // we allow the method to have an extra parameter if the first parameter is XPathContext

                        if (theParameterTypes.length == significantArgs + 1 &&
                                theParameterTypes[0].get_Name().equals("XPathContext")) {
                            if (debug) {
                                diag.println("Method is a candidate because first argument is XPathContext");
                            }
                            candidateMethods.add(theMethod);
                        }
                    }
                }
            }

            // look through the properties of this class to find those that matches the local name

            PropertyInfo[] properties = theType.GetProperties();
            for (int m = 0; m < properties.length; m++) {

                PropertyInfo theProperty = properties[m];

                if (debug) {
                    if (theProperty.get_Name().equals(name)) {
                        diag.println("Trying property " + theProperty.get_Name() + ": name matches");
                        if (!theProperty.get_CanRead()) {
                            diag.println("-- but the property is write-only");
                        }
                        if (!theProperty.GetGetMethod().get_IsPublic()) {
                            diag.println(" -- but the property is not public");
                        }
                    } else {
                        diag.println("Trying property " + theProperty.get_Name() + ": name does not match");
                    }
                }

                if (theProperty.get_Name().equals(name) &&
                        theProperty.get_CanRead() && theProperty.GetGetMethod().get_IsPublic()) {
                    if (consistentReturnType) {
                        if (resultType == null) {
                            resultType = theProperty.GetGetMethod().get_ReturnType();
                        } else {
                            consistentReturnType =
                                    (theProperty.GetGetMethod().get_ReturnType() == resultType);
                        }
                    }
                    boolean isStatic = theProperty.GetGetMethod().get_IsStatic();

                    // if the property is not static, the first supplied argument is the instance, so
                    // discount it

                    if (debug) {
                        diag.println("Property is " + (isStatic ? "" : "not ") + "static");
                    }

                    significantArgs = (isStatic ? numArgs : numArgs - 1);

                    if (significantArgs == 0) {
                        if (debug) {
                            diag.println("Found a candidate property:");
                            diag.println("    " + theProperty);
                        }
                        candidateMethods.add(theProperty);
                    }
                }
            }

            // look through the properties of this class to find those that matches the local name

            FieldInfo[] fields = theType.GetFields();
            for (int m = 0; m < fields.length; m++) {

                FieldInfo theField = fields[m];

                if (debug) {
                    if (theField.get_Name().equals(name)) {
                        diag.println("Trying field " + theField.get_Name() + ": name matches");

                        if (!theField.get_IsPublic()) {
                            diag.println(" -- but the field is not public");
                        }
                    } else {
                        diag.println("Trying field " + theField.get_Name() + ": name does not match");
                    }
                }

                if (theField.get_Name().equals(name) && theField.get_IsPublic()) {
                    if (consistentReturnType) {
                        if (resultType == null) {
                            resultType = theField.get_FieldType();
                        } else {
                            consistentReturnType =
                                    (theField.get_FieldType() == resultType);
                        }
                    }
                    boolean isStatic = theField.get_IsStatic();

                    // if the property is not static, the first supplied argument is the instance, so
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


            // No method found?

            if (candidateMethods.size() == 0) {
                theException = new StaticError("No method, property, or field matching " + name +
                        " with " + numArgs +
                        (numArgs == 1 ? " parameter" : " parameters") +
                        " found in class " + theType.get_Name());
                if (debug) {
                    diag.println(theException.getMessage());
                }
                return null;
            }
        }
        if (candidateMethods.size() == 0) {
            if (debug) {
                diag.println("There is no suitable method, property, or field matching the arguments of function " + local);
            }
            return null;
        }
        MemberInfo method = getBestFit(candidateMethods, staticArgs, theType);
        if (method == null) {
            if (candidateMethods.size() > 1) {
                // There was more than one candidate method, and we can't decide which to use.
                // This may be because insufficient type information is available at this stage.
                // Return an UnresolvedExtensionFunction, and try to resolve it later when more
                // type information is known.
                return new UnresolvedExtensionFunction(nameCode, theType, candidateMethods, staticArgs);
            }
            return null;
        } else {
            DotNetExtensionFunctionFactory factory =
                    ((DotNetPlatform)config.getPlatform()).getExtensionFunctionFactory();
            return factory.makeExtensionFunctionCall(nameCode, theType, method, staticArgs);
        }
    }


    /**
     * Get the best fit amongst all the candidate methods, constructors, or properties, based on the static types
     * of the supplied arguments
     * @param candidateMethods a list of all the methods, properties, and constructors that match the extension
     * function call in name and arity (but not necessarily in the types of the arguments)
     * @param args the expressions supplied as arguments
     * @param containingType the containing type
     * @return the result is either a MethodInfo or a ConstructorInfo or a PropertyInfo, or null if no unique best fit
     * method could be found.
     */

    private MemberInfo getBestFit(List candidateMethods, Expression[] args, cli.System.Type containingType) {
        boolean debug = config.isTraceExternalFunctions();
        int candidates = candidateMethods.size();

        if (candidates == 1) {
            // short cut: there is only one candidate method
            return (MemberInfo) candidateMethods.get(0);

        } else {
            // choose the best fit method or constructor or property
            // for each pair of candidate methods, eliminate either or both of the pair
            // if one argument is less-preferred

            if (debug) {
                diag.println("Finding best fit method with arguments:");
                for (int v = 0; v < args.length; v++) {
                    args[v].display(10, diag, config);
                }
            }

            boolean eliminated[] = new boolean[candidates];
            for (int i = 0; i < candidates; i++) {
                eliminated[i] = false;
            }

            if (debug) {
                for (int i = 0; i < candidates; i++) {
                    int[] pref_i = getConversionPreferences(
                            args,
                            (MemberInfo) candidateMethods.get(i), containingType);
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
                        (MemberInfo) candidateMethods.get(i), containingType);

                if (pref_i == null) {
                    eliminated[i] = true;
                }
                if (!eliminated[i]) {
                    for (int j = i + 1; j < candidates; j++) {
                        if (!eliminated[j]) {
                            int[] pref_j = getConversionPreferences(args,
                                    (MemberInfo)candidateMethods.get(j), containingType);
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
            MemberInfo theMethod = null;
            for (int r = 0; r < candidates; r++) {
                if (!eliminated[r]) {
                    theMethod = (MemberInfo) candidateMethods.get(r);
                    remaining++;
                }
            }

            if (debug) {
                diag.println("Number of candidates remaining: " + remaining);
            }

            if (remaining == 0) {
                if (debug) {
                    diag.println("There are " + candidates +
                            " candidate .NET members matching the function name, but none is a unique best match");
                }
                return null;
            }

            if (remaining > 1) {
                if (debug) {
                    diag.println("There are several .NET methods that match the function name equally well");
                }
                return null;
            }

            return theMethod;
        }
    }

    /**
     * Get an array of integers representing the conversion distances of each "real" argument
     * to a given method
     * @param args: the actual expressions supplied in the function call
     * @param method: the method or constructor.
     * @return an array of integers, one for each argument, indicating the conversion
     * distances. A high number indicates low preference. If any of the arguments cannot
     * be converted to the corresponding type defined in the method signature, return null.
     */

    private int[] getConversionPreferences(Expression[] args, MemberInfo method, cli.System.Type containingType) {

        ParameterInfo[] params;
        int firstArg;
        TypeHierarchy th = config.getTypeHierarchy();
        if (method instanceof ConstructorInfo) {
            firstArg = 0;
            params = ((ConstructorInfo) method).GetParameters();
        } else if (method instanceof MethodInfo) {
            boolean isStatic = ((MethodInfo)method).get_IsStatic();
            firstArg = (isStatic ? 0 : 1);
            params = ((MethodInfo) method).GetParameters();
        } else if (method instanceof PropertyInfo) {
            boolean isStatic = ((PropertyInfo)method).GetGetMethod().get_IsStatic();
            firstArg = (isStatic ? 0 : 1);
            params = NO_PARAMS;
        } else if (method instanceof FieldInfo) {
            boolean isStatic = ((FieldInfo)method).get_IsStatic();
            firstArg = (isStatic ? 0 : 1);
            params = NO_PARAMS;
        } else {
            throw new AssertionError("Member " + method + " is neither constructor, method, property, nor field");
        }

        int noOfArgs = args.length;
        int preferences[] = new int[noOfArgs];
        int firstParam = 0;

        if (params.length > 0 && params[0].get_Name().equals("XPathContext")) {
            firstParam = 1;
        }
        for (int i = firstArg; i < noOfArgs; i++) {
            preferences[i] = getConversionPreference(th, args[i], params[i + firstParam - firstArg].get_ParameterType());
            if (preferences[i] == -1) {
                return null;
            }
        }

        if (firstArg == 1) {
            preferences[0] = getConversionPreference(th, args[0], containingType);
            if (preferences[0] == -1) {
                return null;
            }
        }

        return preferences;
    }

    /**
     * Get the conversion preference from a given XPath type to a given .NET class
     * @param arg the supplied XPath expression (the static type of this expression
     * is used as input to the algorithm)
     * @param required the .NET class of the relevant argument of the .NET method
     * @return the conversion preference. A high number indicates a low preference;
     * -1 indicates that conversion is not possible.
     */

    private int getConversionPreference(TypeHierarchy th, Expression arg, cli.System.Type required) {
        ItemType itemType = arg.getItemType(th);
        int cardinality = arg.getCardinality();
        if (required == DotNetExtensionFunctionCall.CLI_OBJECT) {
            return 100;
        } else if (Cardinality.allowsMany(cardinality)) {
            if (required.IsAssignableFrom(DotNetExtensionFunctionCall.CLI_SEQUENCEITERATOR)) {
                return 20;
            } else if (required.IsAssignableFrom(DotNetExtensionFunctionCall.CLI_VALUE)) {
                return 21;
            } else if (DotNetExtensionFunctionCall.CLI_ICOLLECTION.IsAssignableFrom(required)) {
                return 22;
            } else if (required.get_IsArray()) {
                return 24;
                // sort out at run-time whether the component type of the array is actually suitable
            } else {
                return 80;    // conversion possible only if external object model supports it
            }
        } else {
            if (Type.isNodeType(itemType)) {
                if (required.IsAssignableFrom(DotNetExtensionFunctionCall.CLI_NODEINFO)) {
                    // TODO: support the wrapper classes in the .NET Saxon API
                    return 20;
                } else if (required.IsAssignableFrom(DotNetExtensionFunctionCall.CLI_DOCUMENTINFO)) {
                    return 21;
                } else {
                    return 80;
                }
//            } else if (itemType instanceof ExternalObjectType) {
                // TODO: support wrapping of external .NET objects
//                cli.System.Type ext = ((ExternalObjectType)itemType).getJavaClass();
//                if (required.IsAssignableFrom(ext)) {
//                    return 10;
//                } else {
//                    return -1;
//                }
            } else {
                int primitiveType = itemType.getPrimitiveType();
                return atomicConversionPreference(primitiveType, required);
            }
        }
    }

    private static final ParameterInfo[] NO_PARAMS = new ParameterInfo[0];


    /**
     * Get the conversion preference from an XPath primitive atomic type to a .NET type
     * @param primitiveType integer code identifying the XPath primitive type, for example
     * {@link org.orbeon.saxon.type.Type#INTEGER} or {@link org.orbeon.saxon.type.Type#STRING}
     * @param required The .NET Class named in the method signature
     * @return an integer indicating the relative preference for converting this primitive type
     * to this .NET class. A high number indicates a low preference. All values are in the range
     * 50 to 100. For example, the conversion of an XPath String to {@link org.orbeon.saxon.value.StringValue} is 50, while
     * XPath String to {@link String} is 51. The value -1 indicates that the conversion is not allowed.
     */

    protected int atomicConversionPreference(int primitiveType, cli.System.Type required) {
        if (required == DotNetExtensionFunctionCall.CLI_OBJECT) {
            return 100;
        }
        switch (primitiveType) {
            case Type.STRING:
                if (required.IsAssignableFrom(cli.System.Type.GetType("org.orbeon.saxon.StringValue"))) return 50;
                if (required == cli.System.Type.GetType("java.lang.String")) return 51;
                if (required == DotNetExtensionFunctionCall.CLI_STRING) return 51;
                return -1;
            case Type.DOUBLE:
                if (required.IsAssignableFrom(cli.System.Type.GetType("org.orbeon.saxon.DoubleValue"))) return 50;
                if (required == DotNetExtensionFunctionCall.CLI_DOUBLE) return 51;
                return -1;
            case Type.FLOAT:
                if (required.IsAssignableFrom(cli.System.Type.GetType("org.orbeon.saxon.FloatValue"))) return 50;
                if (required == DotNetExtensionFunctionCall.CLI_SINGLE) return 51;
                if (required == DotNetExtensionFunctionCall.CLI_DOUBLE) return 52;
                return -1;
            case Type.DECIMAL:
                if (required.IsAssignableFrom(cli.System.Type.GetType("org.orbeon.saxon.DecimalValue"))) return 50;
                if (required == DotNetExtensionFunctionCall.CLI_DECIMAL) return 51;
                if (required == DotNetExtensionFunctionCall.CLI_DOUBLE) return 52;
                if (required == DotNetExtensionFunctionCall.CLI_SINGLE) return 53;
                return -1;
            case Type.INTEGER:
                if (required.IsAssignableFrom(cli.System.Type.GetType("org.orbeon.saxon.IntegerValue"))) return 50;
                if (required == DotNetExtensionFunctionCall.CLI_DECIMAL) return 51;
                if (required == DotNetExtensionFunctionCall.CLI_INT64) return 52;
                if (required == DotNetExtensionFunctionCall.CLI_INT32) return 53;
                if (required == DotNetExtensionFunctionCall.CLI_INT16) return 54;
                if (required == DotNetExtensionFunctionCall.CLI_DOUBLE) return 55;
                if (required == DotNetExtensionFunctionCall.CLI_SINGLE) return 56;
                return -1;

            case Type.BOOLEAN:
                if (required.IsAssignableFrom(cli.System.Type.GetType("org.orbeon.saxon.BooleanValue"))) return 50;
                if (required == DotNetExtensionFunctionCall.CLI_BOOLEAN) return 51;
                return -1;
            case Type.DATE:
            case Type.G_DAY:
            case Type.G_MONTH_DAY:
            case Type.G_MONTH:
            case Type.G_YEAR_MONTH:
            case Type.G_YEAR:
                if (required.IsAssignableFrom(cli.System.Type.GetType("org.orbeon.saxon.DateValue"))) return 50;
                if (required == cli.System.Type.GetType("System.DateTime")) return 51;
                return -1;
            case Type.DATE_TIME:
                if (required.IsAssignableFrom(cli.System.Type.GetType("org.orbeon.saxon.DateTimeValue"))) return 50;
                if (required == cli.System.Type.GetType("System.DateTime")) return 51;
                return -1;
            case Type.TIME:
                if (required.IsAssignableFrom(cli.System.Type.GetType("org.orbeon.saxon.TimeValue"))) return 50;
                if (required == cli.System.Type.GetType("System.DateTime")) return 51;
                return -1;
            case Type.DURATION:
            case Type.YEAR_MONTH_DURATION:
            case Type.DAY_TIME_DURATION:
                if (required.IsAssignableFrom(cli.System.Type.GetType("org.orbeon.saxon.DurationValue"))) return 50;
                return -1;
            case Type.ANY_URI:
                if (required.IsAssignableFrom(cli.System.Type.GetType("org.orbeon.saxon.AnyURIValue"))) return 50;
                if (required == cli.System.Type.GetType("System.Uri")) return 51;
                if (required == DotNetExtensionFunctionCall.CLI_STRING) return 52;
                return -1;
            case Type.QNAME:
                if (required.IsAssignableFrom(cli.System.Type.GetType("org.orbeon.saxon.QNameValue"))) return 50;
                if (required == cli.System.Type.GetType("System.Xml.XmlQualifiedName")) return 51;
                return -1;
            case Type.BASE64_BINARY:
                if (required.IsAssignableFrom(cli.System.Type.GetType("org.orbeon.saxon.Base64BinaryValue"))) return 50;
                return -1;
            case Type.HEX_BINARY:
                if (required.IsAssignableFrom(cli.System.Type.GetType("org.orbeon.saxon.HexBinaryValue"))) return 50;
                return -1;
            case Type.UNTYPED_ATOMIC:
                return 50;
            default:
                return -1;
        }
    }

    /**
     * Get an external .NET class corresponding to a given namespace URI, if there is
     * one.
     * @param uri The namespace URI corresponding to the prefix used in the function call.
     * @return the .NET type if a suitable type exists, otherwise return null.
     */

    private cli.System.Type getExternalDotNetType(String uri) {

        // First see if an explicit mapping has been registered for this URI

        cli.System.Type c = (cli.System.Type) explicitMappings.get(uri);
        if (c != null) {
            return c;
        }

        // Failing that, try to identify a type directly from the URI

        try {

            // support the URN format type:full.type.Name?assembly=name;version=ver;culture=cult...

            if (uri.startsWith("type:")) {
                return ((DotNetPlatform)config.getPlatform()).dynamicLoad(uri, config.isTraceExternalFunctions());
            }
        } catch (XPathException err) {
            return null;
        }
        return null;
    }

    /**
     * Inner class representing an unresolved extension function call. This arises when there is insufficient
     * static type information available at the time the function call is parsed to determine which of several
     * candidate .NET methods to invoke. The function call cannot be executed; it must be resolved to an
     * actual .NET method during the analysis phase.
     */

    private class UnresolvedExtensionFunction extends CompileTimeFunction {

        List candidateMethods;
        int nameCode;
        cli.System.Type theClass;


        public UnresolvedExtensionFunction(int nameCode, cli.System.Type theClass, List candidateMethods, Expression[] staticArgs) {
            setArguments(staticArgs);
            this.nameCode = nameCode;
            this.theClass = theClass;
            this.candidateMethods = candidateMethods;
        }

        /**
         * Type-check the expression.
         */

        public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
            for (int i=0; i<argument.length; i++) {
                Expression exp = argument[i].typeCheck(env, contextItemType);
                if (exp != argument[i]) {
                    adoptChildExpression(exp);
                    argument[i] = exp;
                }
            }
            MemberInfo method = getBestFit(candidateMethods, argument, theClass);
            if (method == null) {
                StaticError err = new StaticError("There is more than one method matching the function call " +
                        config.getNamePool().getDisplayName(nameCode) +
                        ", and there is insufficient type information to determine which one should be used");
                err.setLocator(this);
                throw err;
            } else {
                DotNetExtensionFunctionFactory factory =
                        ((DotNetPlatform)config.getPlatform()).getExtensionFunctionFactory();
                Expression call = factory.makeExtensionFunctionCall(nameCode, theClass, method, argument);
                if (call instanceof ComputedExpression) {
                    ((ComputedExpression)call).setLocationId(getLocationId());
                    ((ComputedExpression)call).setParentExpression(getParentExpression());
                }
                return call;
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
        DotNetExtensionLibrary jel = new DotNetExtensionLibrary(config);
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
// The Initial Developer of the Original Code is Michael H. Kay. Contributions were made by Gunther Schadow.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//