/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * by the @author tags. See the COPYRIGHT.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.apiviz;

import com.sun.javadoc.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author The APIviz Project (apiviz-dev@lists.jboss.org)
 * @author Trustin Lee (tlee@redhat.com)
 *
 * @version $Rev$, $Date$
 *
 */
public class Edge implements Comparable<Edge> {

    private final EdgeType type;
    private final Doc source;
    private final Doc target;
    private final String sourceLabel;
    private final String targetLabel;
    private final String edgeLabel;
    private final boolean oneway;
    private final int hashCode;

    public Edge(EdgeType type, Doc source, Doc target) {
        this.type = type;
        this.source = source;
        this.target = target;
        sourceLabel = "";
        targetLabel = "";
        edgeLabel = "";
        oneway = true;
        hashCode = calculateHashCode();
    }

    private int calculateHashCode() {
        return ((((((oneway ? 31 : 0) + type.hashCode()) * 31 + getSourceName().hashCode()) * 31 + getTargetName().hashCode()) * 31 + sourceLabel.hashCode()) * 31 + targetLabel.hashCode()) * 31 + edgeLabel.hashCode();
    }

    private String getSourceName() {
        if (source instanceof ClassDoc) {
            return ((ClassDoc) source).qualifiedName();
        } else {
            return source.name();
        }
    }

    private String getTargetName() {
        if (target instanceof ClassDoc) {
            return ((ClassDoc) target).qualifiedName();
        } else {
            return target.name();
        }
    }

    public Edge(final RootDoc rootDoc, EdgeType type, Doc source, String spec) {
        if (spec == null) {
            spec = "";
        }

        this.type = type;
        this.source = source;

        final String[] args = spec.replaceAll("\\s+", " ").trim().split(" ");
        for (int i = 1; i < Math.min(4, args.length); i++) {
            if (args[i].equals("-")) {
                args[i] = "";
            }
        }

        if (args.length == 1) {
            if (rootDoc.classNamed(args[0]) == null) {
                //Set up a mock ClassDoc incase we can not find the referenced class in the classpath.
                // - This was needed because when doing a non-agrigating javadoc in maven the sibling
                //   project's packages are not included.
                // - Also, I apologize for this nested class mess
                final InvocationHandler handler = new InvocationHandler() {

                    public Object invoke(Object proxy, Method method, Object[] argz) throws Throwable {
                        final String methodName = method.getName();
                        if ("name".equals(methodName) || "qualifiedName".equals(methodName)) {
                            //FIXME - this should be stereotyped as <<NotFound>>, but I can't figure out the encoding
                            return args[0].replaceAll(".*\\.", "") + ".NotFound";
                        }
                        if ("hashCode".equals(methodName)) {
                            return args[0].hashCode();
                        }
                        if ("equals".equals(methodName)) {
                            return args[0].equals(argz[0]);
                        }
                        if ("tags".equals(methodName)) {
                            return new Tag[]{};
                        }
                        if ("methods".equals(methodName)) {
                            return new MethodDoc[]{};
                        }
                        if ("containingPackage".equals(methodName)) {
                            //do this wonkiness so the diagram will show the package as not found
                            return Proxy.newProxyInstance(PackageDoc.class.getClassLoader(),
                                    new Class[]{PackageDoc.class},
                                    new InvocationHandler() {

                                        public Object invoke(Object proxy, Method method, Object[] argzz) throws Throwable {
                                            if ("name".equals(method.getName())) {
                                                return args[0].replaceAll("\\.[a-zA-Z0-9_]*$", "");
                                            }
                                            return null;
                                        }
                                    });
                        }
                        if ("fields".equals(methodName)) {
                            return new FieldDoc[]{};
                        }
                        if (Boolean.TYPE.equals(method.getReturnType())) {
                            return false;
                        }
                        throw new UnsupportedOperationException(methodName);
                    }
                };
                target = (ClassDoc) Proxy.newProxyInstance(ClassDoc.class.getClassLoader(),
                        new Class[]{ClassDoc.class},
                        handler);
            } else {
                target = rootDoc.classNamed(args[0]);
            }
            sourceLabel = "";
            targetLabel = "";
            edgeLabel = "";
            oneway = true;
        } else if (args.length >= 3) {
            target = rootDoc.classNamed(args[0]);
            if (args.length > 3) {
                int startIndex;
                if (args[1].equalsIgnoreCase("oneway")) {
                    oneway = true;
                    sourceLabel = args[2];
                    targetLabel = args[3];
                    startIndex = 4;
                } else {
                    oneway = false;
                    sourceLabel = args[1];
                    targetLabel = args[2];
                    startIndex = 3;
                }

                StringBuilder buf = new StringBuilder();
                for (int i = startIndex; i < args.length; i++) {
                    buf.append(' ');
                    buf.append(args[i]);
                }
                if (buf.length() == 0) {
                    edgeLabel = "";
                } else {
                    edgeLabel = buf.substring(1);
                }
            } else {
                oneway = false;
                sourceLabel = args[1];
                targetLabel = args[2];
                edgeLabel = "";
            }
        } else {
            throw new IllegalArgumentException("Invalid relationship syntax: " + spec);
        }

        if (target == null) {
            throw new IllegalArgumentException(
                    "Invalid relationship syntax: " + spec +
                    " (Unknown package or class name)");
        }

        hashCode = calculateHashCode();
    }

    public EdgeType getType() {
        return type;
    }

    public Doc getSource() {
        return source;
    }

    public Doc getTarget() {
        return target;
    }

    public String getSourceLabel() {
        return sourceLabel;
    }

    public String getTargetLabel() {
        return targetLabel;
    }

    public String getEdgeLabel() {
        return edgeLabel;
    }

    public boolean isOneway() {
        return oneway;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof Edge)) {
            return false;
        }

        Edge that = (Edge) o;
        return type == that.type && oneway == that.oneway &&
                source == that.source && target == that.target &&
                edgeLabel.equals(that.edgeLabel) &&
                sourceLabel.equals(that.sourceLabel) &&
                targetLabel.equals(that.targetLabel);
    }

    public int compareTo(Edge that) {
        int v;

        v = type.compareTo(that.type);
        if (v != 0) {
            return v;
        }

        v = getSourceName().compareTo(that.getSourceName());
        if (v != 0) {
            return v;
        }

        v = getTargetName().compareTo(that.getTargetName());
        if (v != 0) {
            return v;
        }

        v = Boolean.valueOf(oneway).compareTo(Boolean.valueOf(that.oneway));
        if (v != 0) {
            return v;
        }

        v = edgeLabel.compareTo(that.edgeLabel);
        if (v != 0) {
            return v;
        }

        v = sourceLabel.compareTo(that.sourceLabel);
        if (v != 0) {
            return v;
        }

        v = targetLabel.compareTo(that.targetLabel);
        return v;
    }

    protected class MockClassDoc implements ClassDoc {

        private String name;

        public MockClassDoc() {
        }

        public MockClassDoc(final String name) {
            this.name = name;
        }

        public boolean isAbstract() {
            return false;
        }

        public boolean isSerializable() {
            return false;
        }

        public boolean isExternalizable() {
            return false;
        }

        public MethodDoc[] serializationMethods() {
            return new MethodDoc[]{};
        }

        public FieldDoc[] serializableFields() {
            return new FieldDoc[]{};
        }

        public boolean definesSerializableFields() {
            return false;
        }

        public ClassDoc superclass() {
            return null;
        }

        public Type superclassType() {
            return null;
        }

        public boolean subclassOf(ClassDoc cd) {
            return false;
        }

        public ClassDoc[] interfaces() {
            return new ClassDoc[]{};
        }

        public Type[] interfaceTypes() {
            return new Type[]{};
        }

        public TypeVariable[] typeParameters() {
            return new TypeVariable[]{};
        }

        public ParamTag[] typeParamTags() {
            return new ParamTag[]{};
        }

        public FieldDoc[] fields() {
            return new FieldDoc[]{};
        }

        public FieldDoc[] fields(boolean filter) {
            return new FieldDoc[]{};
        }

        public FieldDoc[] enumConstants() {
            return new FieldDoc[]{};
        }

        public MethodDoc[] methods() {
            return new MethodDoc[]{};
        }

        public MethodDoc[] methods(boolean filter) {
            return new MethodDoc[]{};
        }

        public ConstructorDoc[] constructors() {
            return new ConstructorDoc[]{};
        }

        public ConstructorDoc[] constructors(boolean filter) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public ClassDoc[] innerClasses() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public ClassDoc[] innerClasses(boolean filter) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public ClassDoc findClass(String className) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Deprecated
        public ClassDoc[] importedClasses() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Deprecated
        public PackageDoc[] importedPackages() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public ClassDoc containingClass() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public PackageDoc containingPackage() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String qualifiedName() {
            return this.name + ": notfound";
        }

        public int modifierSpecifier() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String modifiers() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public AnnotationDesc[] annotations() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isPublic() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isProtected() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isPrivate() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isPackagePrivate() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isStatic() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isFinal() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String commentText() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Tag[] tags() {
            return new Tag[]{};
        }

        public Tag[] tags(String tagname) {
            return new Tag[]{};
        }

        public SeeTag[] seeTags() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Tag[] inlineTags() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Tag[] firstSentenceTags() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getRawCommentText() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void setRawCommentText(String rawDocumentation) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String name() {
            return this.name + ": notfound";
        }

        public int compareTo(Object obj) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isField() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isEnumConstant() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isConstructor() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isMethod() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isAnnotationTypeElement() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isInterface() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isException() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isError() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isEnum() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isAnnotationType() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isOrdinaryClass() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isClass() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isIncluded() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public SourcePosition position() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String typeName() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String qualifiedTypeName() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String simpleTypeName() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String dimension() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isPrimitive() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public ClassDoc asClassDoc() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public ParameterizedType asParameterizedType() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public TypeVariable asTypeVariable() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public WildcardType asWildcardType() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public AnnotationTypeDoc asAnnotationTypeDoc() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
