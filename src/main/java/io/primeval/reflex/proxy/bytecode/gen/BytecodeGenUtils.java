package io.primeval.reflex.proxy.bytecode.gen;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.DRETURN;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.FRETURN;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Opcodes.LRETURN;
import static org.objectweb.asm.Opcodes.RETURN;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Type;

import io.primeval.reflex.proxy.handler.BooleanInterceptionHandler;
import io.primeval.reflex.proxy.handler.ByteInterceptionHandler;
import io.primeval.reflex.proxy.handler.CharInterceptionHandler;
import io.primeval.reflex.proxy.handler.DoubleInterceptionHandler;
import io.primeval.reflex.proxy.handler.FloatInterceptionHandler;
import io.primeval.reflex.proxy.handler.IntInterceptionHandler;
import io.primeval.reflex.proxy.handler.InterceptionHandler;
import io.primeval.reflex.proxy.handler.LongInterceptionHandler;
import io.primeval.reflex.proxy.handler.ShortInterceptionHandler;
import io.primeval.reflex.proxy.handler.VoidInterceptionHandler;

public final class BytecodeGenUtils {

    public static final Set<Class<?>> IRETURN_TYPES = new HashSet<>(
            Arrays.asList(new Class<?>[] { int.class, char.class, byte.class, short.class, boolean.class }));

    private BytecodeGenUtils() {
    }

    // We probably don't support everything, for instance nested types...
    // But it's simple enough to add here.
    public static String getTypeSignature(Class<?> clazzToProxy) {
        boolean isGeneric = false;
        isGeneric = clazzToProxy.getTypeParameters().length > 0;
        java.lang.reflect.Type genericSuperclass = clazzToProxy.getGenericSuperclass();
        if (!isGeneric) {
            isGeneric = genericSuperclass instanceof ParameterizedType;
            for (java.lang.reflect.Type t : clazzToProxy.getGenericInterfaces()) {
                if (!isGeneric) {
                    isGeneric = t instanceof ParameterizedType;
                }
            }
        }
        if (!isGeneric) {
            return null;
        }

        StringBuilder buf = new StringBuilder();
        if (clazzToProxy.getTypeParameters().length > 0) {
            buf.append('<');
            for (TypeVariable<?> t : clazzToProxy.getTypeParameters()) {
                buf.append(BytecodeGenUtils.getDescriptorForJavaType(t, true));
            }
            buf.append('>');
        }
        buf.append(getDescriptorForJavaType(genericSuperclass));

        for (java.lang.reflect.Type t : clazzToProxy.getGenericInterfaces()) {
            buf.append(getDescriptorForJavaType(t));
        }
        String typeSig = buf.toString();
        return typeSig;
    }

    public static String getMethodSignature(Method method) {
        boolean isGeneric = false;
        java.lang.reflect.Type genericReturnType = method.getGenericReturnType();
        boolean hasParameterizedReturnType = genericReturnType instanceof ParameterizedType;
        boolean hasTypeVarReturnType = genericReturnType instanceof TypeVariable<?>;
        isGeneric = hasParameterizedReturnType || hasTypeVarReturnType;
        Map<String, TypeVariable<?>> methodTypeVars = new LinkedHashMap<>();
        if (hasTypeVarReturnType) {
            TypeVariable<?> rType = (TypeVariable<?>) genericReturnType;
            methodTypeVars.put(rType.getName(), rType);
        }
        if (hasParameterizedReturnType) {
            ParameterizedType returnType = (ParameterizedType) genericReturnType;
            java.lang.reflect.Type[] actualTypeArguments = returnType.getActualTypeArguments();
            for (int i = 0; i < actualTypeArguments.length; i++) {
                java.lang.reflect.Type typeArg = actualTypeArguments[i];
                if (typeArg instanceof TypeVariable<?>) {
                    TypeVariable<?> typeVariable = (TypeVariable<?>) typeArg;
                    methodTypeVars.computeIfAbsent(typeVariable.getName(), k -> typeVariable);
                }
            }
        }
        for (java.lang.reflect.Type t : method.getGenericParameterTypes()) {
            boolean isTypeVar = t instanceof TypeVariable<?>;
            if (!isGeneric) {
                isGeneric = t instanceof ParameterizedType | isTypeVar;
            }
            if (isTypeVar) {
                TypeVariable<?> typeVariable = (TypeVariable<?>) t;
                methodTypeVars.computeIfAbsent(typeVariable.getName(), k -> typeVariable);
            }
        }
        java.lang.reflect.Type[] genericExceptionTypes = method.getGenericExceptionTypes();
        for (java.lang.reflect.Type t : genericExceptionTypes) {
            if (!isGeneric) {
                isGeneric = t instanceof ParameterizedType | t instanceof TypeVariable<?>;
            }
        }

        if (!isGeneric) {
            return null;
        }

        StringBuilder buf = new StringBuilder();

        if (!methodTypeVars.isEmpty()) {
            buf.append('<');
            for (TypeVariable<?> typeVar : methodTypeVars.values()) {
                buf.append(getDescriptorForJavaType(typeVar, true));
            }
            buf.append('>');
        }

        buf.append('(');
        for (java.lang.reflect.Type t : method.getGenericParameterTypes()) {
            buf.append(getDescriptorForJavaType(t));
        }
        buf.append(')');
        buf.append(getDescriptorForJavaType(genericReturnType));

        if (genericExceptionTypes.length > 0) {
            buf.append('^');
            for (java.lang.reflect.Type t : method.getGenericExceptionTypes()) {
                buf.append(BytecodeGenUtils.getDescriptorForJavaType(t, false));
            }
        }

        String typeSig = buf.toString();
        return typeSig;
    }

    public static String getDescriptorForJavaType(java.lang.reflect.Type type) {
        return getDescriptorForJavaType(type, false);
    }

    public static String getDescriptorForJavaType(java.lang.reflect.Type type, boolean expandRefs) {

        if (type instanceof Class<?>) {
            Class<?> cls = (Class<?>) type;
            return Type.getDescriptor(cls);
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Class<?> rawType = (Class<?>) parameterizedType.getRawType();
            StringBuilder buf = new StringBuilder();
            String descriptor = Type.getDescriptor(rawType);
            buf.append(descriptor, 0, descriptor.length() - 1); // omit ";"
            buf.append('<');
            java.lang.reflect.Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            for (int i = 0; i < actualTypeArguments.length; i++) {
                buf.append(getDescriptorForJavaType(actualTypeArguments[i], false));
            }
            buf.append(">;");
            return buf.toString();
        } else if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) type;
            java.lang.reflect.Type genericComponentType = genericArrayType.getGenericComponentType();
            return '[' + getDescriptorForJavaType(genericComponentType);

        } else if (type instanceof TypeVariable<?>) {
            TypeVariable<?> typeVariable = (TypeVariable<?>) type;
            String name = typeVariable.getName();
            if (!expandRefs) {
                return "T" + name + ";";
            }
            java.lang.reflect.Type[] bounds = typeVariable.getBounds();
            StringBuilder buf = new StringBuilder();
            buf.append(name);
            for (java.lang.reflect.Type t : bounds) {
                buf.append(':');
                buf.append(getDescriptorForJavaType(t));
            }
            return buf.toString();
        } else if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;
            java.lang.reflect.Type[] lowerBounds = wildcardType.getLowerBounds();
            java.lang.reflect.Type[] upperBounds = wildcardType.getUpperBounds();
            if (lowerBounds.length > 1) {
                throw new IllegalArgumentException("Must have at most one lower bound.");
            }
            java.lang.reflect.Type lowerBound = lowerBounds.length == 1 ? lowerBounds[0] : null;
            java.lang.reflect.Type upperBound = lowerBounds.length == 1 ? Object.class : upperBounds[0];

            if (lowerBound != null) {
                return "-" + getDescriptorForJavaType(lowerBound, false);
            } else if (upperBound == Object.class) {
                return "*";
            } else {
                return "+" + getDescriptorForJavaType(upperBound, false);
            }

        }
        throw new UnsupportedOperationException("unsupported reflection type: " + type.getClass());

    }

    public static Class<?> getBoxed(Class<?> type) {
        if (type == int.class) {
            return Integer.class;
        } else if (type == short.class) {
            return Short.class;
        } else if (type == boolean.class) {
            return Boolean.class;
        } else if (type == char.class) {
            return Character.class;
        } else if (type == byte.class) {
            return Byte.class;
        } else if (type == long.class) {
            return Long.class;
        } else if (type == double.class) {
            return Double.class;
        } else if (type == float.class) {
            return Float.class;
        } else if (type == void.class) {
            return Void.class;
        } else {
            return type;
        }
    }

    public static int getTypeSize(Class<?> type) {
        if (type == void.class) {
            return 0;
        } else if (type == double.class || type == long.class) {
            return 2;
        }
        return 1;
    }

    static int getReturnCode(Class<?> returnType) {
        if (returnType == void.class) {
            return RETURN;
        } else if (IRETURN_TYPES.contains(returnType)) {
            return IRETURN;
        } else if (returnType == long.class) {
            return LRETURN;
        } else if (returnType == double.class) {
            return DRETURN;
        } else if (returnType == float.class) {
            return FRETURN;
        } else {
            return ARETURN;
        }
    }

    public static int getLoadCode(Class<?> type) {
        if (type == void.class) {
            throw new IllegalArgumentException("No load code for void!");
        } else if (IRETURN_TYPES.contains(type)) {
            return ILOAD;
        } else if (type == long.class) {
            return LLOAD;
        } else if (type == double.class) {
            return DLOAD;
        } else if (type == float.class) {
            return FLOAD;
        } else {
            return ALOAD;
        }
    }

    static Class<?> getInterceptionHandlerClass(Class<?> returnType) {
        if (returnType == void.class) {
            return VoidInterceptionHandler.class;
        } else if (returnType == int.class) {
            return IntInterceptionHandler.class;
        } else if (returnType == short.class) {
            return ShortInterceptionHandler.class;
        } else if (returnType == double.class) {
            return DoubleInterceptionHandler.class;
        } else if (returnType == float.class) {
            return FloatInterceptionHandler.class;
        } else if (returnType == char.class) {
            return CharInterceptionHandler.class;
        } else if (returnType == long.class) {
            return LongInterceptionHandler.class;
        } else if (returnType == byte.class) {
            return ByteInterceptionHandler.class;
        } else if (returnType == boolean.class) {
            return BooleanInterceptionHandler.class;
        } else {
            return InterceptionHandler.class;
        }
    }

    static String makeSuffixClassDescriptor(String classToProxyDescriptor, String suffix) {
        StringBuilder buf = new StringBuilder();
        buf.append(classToProxyDescriptor, 0, classToProxyDescriptor.length() - 1); // omit
        // ';'
        buf.append(suffix);
        buf.append(';');
        return buf.toString();
    }

    static String nullToEmpty(String argsClassDescriptor) {
        if (argsClassDescriptor == null)
            return "";
        return argsClassDescriptor;
    }
}
