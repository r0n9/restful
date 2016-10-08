package vip.fanrong.REST;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import vip.fanrong.REST.Status.StatusCode;

public class REST {
    private static Logger log = LogManager.getLogger(REST.class);

    private REST() {
        super();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface Method {
        String value() default "";

        String type() default "sync";

        String mkey() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface Service {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER})
    public @interface Param {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER})
    public @interface OptParam {
        String name();

        String defaultValue();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER})
    public @interface HttpResponse {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER})
    public @interface Monitor {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER})
    public @interface InputBody {
    }

    enum RESTError {
        BadArgumentType, MalformedRequestURL, ClassNotFound, ClassNotPublished, MethodNotFound, MethodNotPublished, ArgumentMissing, BadArgument, InternalError
    };

    static class DispatcherException extends Exception {

        private static final long serialVersionUID = 1L;

        private RESTError code;

        DispatcherException(RESTError c, String message) {
            super(message);
            code = c;
        }
    }

    static void sendError(OutputStream os, DispatcherException de) {
        PrintStream ps = new PrintStream(os);
        ps.print("<error>");
        ps.print("<code>");
        ps.print(de.code);
        ps.print("</code>");
        ps.print("<message>");
        ps.print(de.getMessage());
        ps.print("</message>");
        ps.print("</error>");
    }

    static void sendException(OutputStream os, Exception e) {
        PrintStream ps = new PrintStream(os);
        ps.print("<error>");
        ps.print("<code>");
        ps.print(RESTError.InternalError);
        ps.print("</code>");
        ps.print("<message>");
        ps.print(e.toString());
        ps.print("</message>");
        ps.print("</error>");
    }

    private static boolean basicType(Class<?> resT) {
        return ((resT == int.class) || (resT == java.lang.Integer.class) || (resT == byte.class)
                || (resT == java.lang.Byte.class) || (resT == char.class)
                || (resT == java.lang.Character.class) || (resT == short.class)
                || (resT == java.lang.Short.class) || (resT == long.class)
                || (resT == java.lang.Long.class) || (resT == java.lang.String.class)
                || (resT == boolean.class) || (resT == java.lang.Boolean.class)
                || (resT == float.class) || (resT == java.lang.Float.class)
                || (resT == double.class) || (resT == java.lang.Double.class));
    }

    public static Map<String, List<String>> getQueryMap(String query) {

        Map<String, List<String>> map = new HashMap<String, List<String>>();
        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                String[] pieces = param.split("=");
                String name = pieces[0];
                String value = null;
                if (pieces.length > 1) {
                    value = pieces[1];
                }

                if (name.contains("[") && name.contains("]")) {
                    String newName = name.substring(0, name.indexOf('['));
                    String varStart = name.substring(name.indexOf('[') + 1);
                    varStart = varStart.replaceAll("\\]", "--");
                    varStart = varStart.replaceAll("\\[", "");
                    value = "[" + varStart + value + "]";
                    name = newName;
                }

                List<String> entry = map.get(name);
                if (entry == null) {
                    entry = new ArrayList<String>();
                    map.put(name, entry);
                }

                entry.add(value);
                map.put(name, entry);
            }
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    static Object parseArgument(String arg, List<String> argVals, Class<?> argT,
            Map<String, List<String>> map, String name) throws DispatcherException {
        if ((argT == int.class)) {
            if (arg == null) {
                throw new DispatcherException(RESTError.BadArgumentType,
                        "argument of type 'int' must have value provided");
            }
            return Integer.parseInt(arg);
        } else if ((argT == java.lang.Integer.class)) {
            if (arg == null || arg.equalsIgnoreCase("null")) {
                return null;
            } else {
                return Integer.valueOf(arg);
            }
        } else if ((argT == boolean.class) || (argT == java.lang.Boolean.class)) {
            if (arg == null) {
                throw new DispatcherException(RESTError.BadArgumentType,
                        "argument of type 'bool' must have value provided");
            }
            return Boolean.parseBoolean(arg);
        } else if (argT == java.lang.String.class) {
            arg = decodeParameter(arg);
            return arg;
        } else if (argT == java.lang.Long.class) {
            if (arg == null || arg.equalsIgnoreCase("null")) {
                return null;
            } else {
                return Long.parseLong(arg);
            }
        } else if (argT == long.class) {
            if (arg == null || arg.equalsIgnoreCase("null")) {
                return 0;
            } else {
                return Long.parseLong(arg);
            }
        } else if (argT == List.class) {
            if (arg == null) {
                throw new DispatcherException(RESTError.BadArgumentType,
                        "argument of type 'List' must have value provided");
            }
            List<String> l = map.get(name);
            if (l == null) {
                l = new ArrayList<String>();
            }
            if (argVals != null) {
                for (String str : argVals) {
                    l.add(decodeParameter(str));
                }
            }

            map.put(name, l);
            return l;
        } else if (argT.isEnum()) {
            if (arg.equalsIgnoreCase("null")) {
                return null;
            }
            try {
                Class<? extends Enum> enumClass = (Class<? extends Enum>) argT;
                return Enum.valueOf(enumClass, arg);
            } catch (Exception e) {
                throw new DispatcherException(RESTError.BadArgumentType, "argument of enum " + argT
                        + " have a invalid value: " + arg + " provided");
            }
        } else {
            throw new DispatcherException(RESTError.BadArgumentType, "argument of type \'"
                    + argT.getName() + "\' not supported");
        }
    }

    private static String decodeParameter(String arg) {
        if (arg == null || arg.equalsIgnoreCase("null")) {
            return null;
        }
        try {
            arg = URLDecoder.decode(arg, "utf-8");
        } catch (UnsupportedEncodingException e) {
            log.warn("", e);
        }
        return arg;
    }

    public static RESTResult dispatch(HttpServletResponse response, String path, String query)
            throws DispatcherException {
        return dispatch(response, path, query, null);
    }

    private static ConcurrentHashMap<String, Status> statusQueue =
            new ConcurrentHashMap<String, Status>();

    /**
     * Enhanced dispatch, support sync and async invoke which indicated on method for async invoke,
     * mkey must be specified for async invoke, if parameter checkStatus is set, return status
     * instead of invoke
     * 
     * @param os
     * @param path
     * @param query
     * @param requestBody
     * @return
     * @throws DispatcherException
     */
    public static RESTResult dispatch(HttpServletResponse response, final String path,
            String query, String requestBody) throws DispatcherException {

        Map<String, List<String>> args = getQueryMap(query);
        String methodName = null;
        String className = null;
        try {
            int rslash = path.lastIndexOf('/');
            methodName = path.substring(rslash + 1);
            className = path.substring(1, rslash).replace('/', '.');
        } catch (Exception e) {
            throw new DispatcherException(RESTError.MalformedRequestURL, "illegal request URL");
        }

        // find the service class somewhere in the class path of the default
        // class loader.

        Class<?> c = null;

        try {
            c = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new DispatcherException(RESTError.ClassNotFound, "class \'" + className
                    + "\' not found");
        }

        Service rsa = c.getAnnotation(Service.class);
        if (rsa == null) {
            throw new DispatcherException(RESTError.ClassNotPublished, "class \'" + className
                    + "\' is not a REST service");
        }

        // introspect for the method being called

        // the algorithm is to look for through all methods with a "restmethod"
        // annotation.
        // the chosen method is the first one who has a non-null value for the
        // annotation
        // that matches the method name being invoked OR has a null value for
        // the annotation
        // and has an actual name that matches the method being invoked.

        java.lang.reflect.Method verbmethod = null;
        for (java.lang.reflect.Method m : c.getMethods()) {

            Method rma = m.getAnnotation(Method.class);
            if (rma == null) {
                continue;
            } // not an exposed REST method

            if (!rma.value().equals("")) {
                if (rma.value().equals(methodName)) {
                    verbmethod = m;
                    break;
                }
            } else {
                if (m.getName().equals(methodName)) {
                    verbmethod = m;
                    break;
                }
            }
        }

        if (verbmethod == null) {
            throw new DispatcherException(RESTError.MethodNotFound, "no such method \'" + className
                    + "." + methodName + "\'");
        }

        Method rma = verbmethod.getAnnotation(Method.class);
        if (rma == null) {
            throw new DispatcherException(RESTError.MethodNotPublished, "the method \'" + className
                    + "." + methodName + "\' is not a REST method");
        }

        // now, look at the method and match up parameters with the query
        // parameters

        Class<?>[] parameterTypes = verbmethod.getParameterTypes();
        Annotation[][] parameterAnnotations = verbmethod.getParameterAnnotations();

        int parameterCount = parameterTypes.length;
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        final Object[] arguments = new Object[parameterCount];

        final ExecutionMonitor monitor = new ExecutionMonitor();
        for (int p = 0; p < parameterCount; p++) {
            Class<?> pt = parameterTypes[p];

            // ok, now I have the type, but I need the name too. I get that from
            // the annotation...
            // Java sucks in that you can't get this without adding annotations
            // to your code.

            Annotation[] pas = parameterAnnotations[p];

            if (pas[0] instanceof HttpResponse) {
                arguments[p] = response;
            } else if (pas[0] instanceof Monitor) {
                arguments[p] = monitor;
            } else if (pas[0] instanceof OptParam) {
                OptParam op = (OptParam) pas[0];

                String pName = op.name();
                String defaultValue = op.defaultValue();

                List<String> argvals = args.get(pName);
                if (argvals != null) {
                    String valStr = argvals.get(0);
                    arguments[p] = parseArgument(valStr, argvals, pt, map, pName);
                } else {
                    arguments[p] = parseArgument(defaultValue, null, pt, map, pName);
                }
            } else if (pas[0] instanceof InputBody) {
                arguments[p] = requestBody;
            } else { // assume it is a param
                Param pa = (Param) pas[0];

                String pName = pa.value();

                List<String> argvals = args.get(pName);
                if (argvals == null) {
                    throw new DispatcherException(RESTError.ArgumentMissing, "missing argument \'"
                            + pName + "\'");
                }

                arguments[p] = parseArgument(argvals.get(0), argvals, pt, map, pName);
            }
        }

        RESTResult rres = new RESTResult();
        rres.setClassSelected(className);
        rres.setMethodInvoked(methodName);
        rres.setQuery(query);

        // decide to do async run or sync run
        String type = rma.type();
        if (StringUtils.equals(type, "async")) {// async run
            rres.setIsAsyncMethod(true);
            String mkeyParam = rma.mkey();
            if (StringUtils.isBlank(mkeyParam)) {
                throw new DispatcherException(RESTError.BadArgument,
                        "method must specify mkey for async run");
            }
            List<String> vList = args.get(mkeyParam);
            if ((vList == null) || (StringUtils.isBlank(vList.get(0)))) {
                throw new DispatcherException(RESTError.BadArgument, "mkey param" + mkeyParam
                        + "must be set for async run");
            }
            String mkey = vList.get(0);
            String runKey = path + "_" + mkey;
            rres.setAsycKey(runKey);

            if (args.containsKey("checkStatus")) {
                Status status = statusQueue.get(runKey);
                if (status == null) {
                    rres.setAsyncStatusCode(StatusCode.NO_JOB_RUNNING);
                } else if (status.isCompleted.get()) {
                    rres.setAsyncStatusCode(StatusCode.JOB_COMPLETE);
                } else {
                    rres.setAsyncStatusCode(StatusCode.JOB_RUNNING);
                }
                rres.setStatus(status);
                return rres;
            }

            try {

                Status newStatus = new Status();
                Status oldStatus = statusQueue.putIfAbsent(runKey, newStatus);
                if (oldStatus != null) {
                    if (!oldStatus.isCompleted.compareAndSet(true, false)) {
                        rres.setAsyncStatusCode(StatusCode.JOB_EXISTS);
                        rres.setStatus(oldStatus);
                        return rres;
                    } else {
                        newStatus = oldStatus;
                        oldStatus.clear();
                    }
                }
                final Status status = newStatus;
                rres.setAsyncStatusCode(StatusCode.NEW_JOB_START);
                rres.setStatus(status);

                final java.lang.reflect.Method fmethod = verbmethod;
                ExecutorService executor = Executors.newFixedThreadPool(1);
                // process in new thread
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        status.start();
                        try {
                            log.info("async call: " + path + " start");
                            monitor.start();
                            Object value = fmethod.invoke(null, arguments);
                            status.setMethodResult(value);
                        } catch (Exception e) {
                            e.printStackTrace();
                            status.setErrorMsg(ExceptionUtils.getFullStackTrace(e), null);
                        } finally {
                            status.isCompleted.set(true);
                            status.end();
                            log.info("async call: " + path + " end");
                            monitor.cancel();
                        }

                    }
                });
                executor.shutdown();
                return rres;
            } catch (Exception e) {
                throw new DispatcherException(RESTError.InternalError, e.getMessage());
            }

        } else {// sync call
            Status status = new Status();
            status.start();
            try {
                log.info("sync call: " + path + " start");
                Object value = verbmethod.invoke(null, arguments);
                status.setMethodResult(value);
                rres.setStatus(status);
                return rres;
            } catch (IllegalArgumentException e) {
                throw new DispatcherException(RESTError.BadArgument, "bad arguments "
                        + e.getMessage());
            } catch (IllegalAccessException e) {
                throw new DispatcherException(RESTError.ClassNotPublished,
                        "service method not accessible " + e.getMessage());
            } catch (InvocationTargetException e) {
                log.error("Exception in " + path, e);
                throw new DispatcherException(RESTError.InternalError,
                        "internal error invoking operation " + e.getTargetException().getMessage());
            } finally {
                status.end();
                log.info("sync call: " + path + " complete");
                monitor.cancel();
            }
        }

    }
}
