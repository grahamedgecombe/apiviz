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

import static org.jboss.apiviz.Constant.*;
import static org.jboss.apiviz.EdgeType.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import jdepend.framework.JDepend;
import jdepend.framework.JavaPackage;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Doc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.SeeTag;
import com.sun.javadoc.Tag;

/**
 * @author The APIviz Project (apiviz-dev@lists.jboss.org)
 * @author Trustin Lee (tlee@redhat.com)
 *
 * @version $Rev$, $Date$
 *
 */
public class ClassDocGraph {

    private final RootDoc root;
    private final Map<String, ClassDoc> nodes = new TreeMap<String, ClassDoc>();
    private final Map<ClassDoc, Set<Edge>> edges = new HashMap<ClassDoc, Set<Edge>>();
    private final Map<ClassDoc, Set<Edge>> reversedEdges = new HashMap<ClassDoc, Set<Edge>>();

    public ClassDocGraph(RootDoc root) {
        this.root = root;

        root.printNotice("Building graph for all classes...");
        for (ClassDoc node: root.classes()) {
            addNode(node, true);
        }
    }

    private void addNode(ClassDoc node, boolean addRelatedClasses) {
        String key = node.qualifiedName();
        if (!nodes.containsKey(key)) {
            nodes.put(key, node);
            edges.put(node, new TreeSet<Edge>());
        }

        if (addRelatedClasses) {
            addRelatedClasses(node);
        }
    }

    private void addRelatedClasses(ClassDoc type) {
        // Generalization
        ClassDoc superType = type.superclass();
        if (superType != null &&
            !superType.qualifiedName().equals("java.lang.Object") &&
            !superType.qualifiedName().equals("java.lang.Annotation") &&
            !superType.qualifiedName().equals("java.lang.Enum")) {
            addNode(superType, false);
            addEdge(new Edge(GENERALIZATION, type, superType));
        }

        // Realization
        for (ClassDoc i: type.interfaces()) {
            if (i.qualifiedName().equals("java.lang.annotation.Annotation")) {
                continue;
            }

            addNode(i, false);
            addEdge(new Edge(REALIZATION, type, i));
        }

        // Apply custom doclet tags.
        for (Tag t: type.tags()) {
            if (t.name().equals(TAG_USES)) {
                addEdge(new Edge(root, DEPENDENCY, type, t.text()));
            } else if (t.name().equals(TAG_HAS)) {
                addEdge(new Edge(root, NAVIGABILITY, type, t.text()));
            } else if (t.name().equals(TAG_OWNS)) {
                addEdge(new Edge(root, AGGREGATION, type, t.text()));
            } else if (t.name().equals(TAG_COMPOSED_OF)) {
                addEdge(new Edge(root, COMPOSITION, type, t.text()));
            }
        }

        // Add an edge with '<<see also>>' label for the classes with @see
        // tags, but avoid duplication.
        for (SeeTag t: type.seeTags()) {
            try {
                if (t.referencedClass() == null) {
                    continue;
                }
            } catch (Exception e) {
                continue;
            }

            String a = type.qualifiedName();
            String b = t.referencedClass().qualifiedName();
            addNode(t.referencedClass(), false);
            if (a.compareTo(b) != 0) {
                if (a.compareTo(b) < 0) {
                    addEdge(new Edge(
                            root, SEE_ALSO, type,
                            b + " - - &#171;see also&#187;"));
                } else {
                    addEdge(new Edge(
                            root, SEE_ALSO, t.referencedClass(),
                            a + " - - &#171;see also&#187;"));
                }
            }
        }
    }

    private void addEdge(Edge edge) {
        edges.get(edge.getSource()).add(edge);

        Set<Edge> reversedEdgeSubset = reversedEdges.get(edge.getTarget());
        if (reversedEdgeSubset == null) {
            reversedEdgeSubset = new TreeSet<Edge>();
            reversedEdges.put((ClassDoc) edge.getTarget(), reversedEdgeSubset);
        }
        reversedEdgeSubset.add(edge);
    }

    public String getOverviewSummaryDiagram(JDepend jdepend) {
        Map<String, PackageDoc> packages = new TreeMap<String, PackageDoc>(new Comparator<String>() {
            public int compare(String o1, String o2) {
                return o2.compareTo(o1);
            }
        });

        Set<Edge> edgesToRender = new TreeSet<Edge>();

        addPackageDependencies(jdepend, packages, edgesToRender);

        // Replace direct dependencies with transitive dependencies
        // if possible to simplify the diagram.

        //// Build the matrix first.
        Map<Doc, Set<Doc>> dependencies = new HashMap<Doc, Set<Doc>>();
        for (Edge edge: edgesToRender) {
            Set<Doc> nextDependencies = dependencies.get(edge.getSource());
            if (nextDependencies == null) {
                nextDependencies = new HashSet<Doc>();
                dependencies.put(edge.getSource(), nextDependencies);
            }
            nextDependencies.add(edge.getTarget());
        }

        //// Remove the edges which doesn't change the effective relationship
        //// which can be calculated by indirect (transitive) dependency resolution.
        for (int i = edgesToRender.size(); i > 0 ; i --) {
            for (Edge edge: edgesToRender) {
                if (isIndirectlyReachable(dependencies, edge.getSource(), edge.getTarget())) {
                    edgesToRender.remove(edge);
                    Set<Doc> targets = dependencies.get(edge.getSource());
                    if (targets != null) {
                        targets.remove(edge.getTarget());
                    }
                    break;
                }
            }
        }

        // Get the least common prefix to compact the diagram even further.
        int minPackageNameLen = Integer.MAX_VALUE;
        int maxPackageNameLen = Integer.MIN_VALUE;
        for (String pname: packages.keySet()) {
            if (pname.length() > maxPackageNameLen) {
                maxPackageNameLen = pname.length();
            }
            if (pname.length() < minPackageNameLen) {
                minPackageNameLen = pname.length();
            }
        }

        if (minPackageNameLen == 0) {
            throw new IllegalStateException("Unexpected empty package name");
        }

        int prefixLen = 0;
        if (!packages.keySet().isEmpty()) {
            String firstPackageName = packages.keySet().iterator().next();
            for (prefixLen = minPackageNameLen; prefixLen > 0; prefixLen --) {
                if (firstPackageName.charAt(prefixLen - 1) != '.') {
                    continue;
                }

                String candidatePrefix = firstPackageName.substring(0, prefixLen);
                boolean found = true;
                for (String pname: packages.keySet()) {
                    if (!pname.startsWith(candidatePrefix)) {
                        found = false;
                        break;
                    }
                }

                if (found) {
                    break;
                }
            }
        }

        StringBuilder buf = new StringBuilder(16384);
        buf.append(
                "digraph APIVIZ {" + NEWLINE +
                "rankdir=LR;" + NEWLINE +
                "ranksep=0.3;" + NEWLINE +
                "nodesep=0.3;" + NEWLINE +
                "mclimit=128;" + NEWLINE +
                "outputorder=edgesfirst;" + NEWLINE +
                "center=1;" + NEWLINE +
                "remincross=true;" + NEWLINE +
                "searchsize=65536;" + NEWLINE +
                "edge [fontsize=10, fontname=\"" + NORMAL_FONT + "\", " +
                "style=\"setlinewidth(0.6)\"]; " + NEWLINE +
                "node [shape=box, fontsize=10, fontname=\"" + NORMAL_FONT + "\", " +
                "width=0.1, height=0.1, style=\"setlinewidth(0.6)\"]; " + NEWLINE);

        for (PackageDoc pkg: packages.values()) {
            renderPackage(buf, pkg, prefixLen);
        }

        for (Edge edge: edgesToRender) {
            renderEdge(null, buf, edge);
        }

        buf.append("}" + NEWLINE);

        return buf.toString();
    }

    @SuppressWarnings("unchecked")
    private void addPackageDependencies(
            JDepend jdepend, Map<String, PackageDoc> packages, Set<Edge> edgesToRender) {

        Map<String, PackageDoc> allPackages = APIviz.getPackages(root);
        for (String pname: allPackages.keySet()) {
            if (isHidden(allPackages.get(pname))) {
                continue;
            }

            JavaPackage pkg = jdepend.getPackage(pname);
            if (pkg == null) {
                continue;
            }

            packages.put(pname, allPackages.get(pname));

            Collection<JavaPackage> epkgs = pkg.getEfferents();
            if (epkgs == null) {
                continue;
            }

            for (JavaPackage epkg: epkgs) {
                if (isHidden(allPackages.get(epkg.getName()))) {
                    continue;
                }
                addPackageDependency(edgesToRender, allPackages.get(pname), allPackages.get(epkg.getName()));
            }
        }
    }

    static boolean isHidden(Doc node) {
        if (node.tags(TAG_HIDDEN).length > 0) {
            return true;
        }

        Tag[] tags = node.tags(TAG_EXCLUDE);
        if (tags == null) {
            return false;
        }

        for (Tag t: tags) {
            if (t.text() == null || t.text().trim().length() == 0) {
                return true;
            }
        }

        return false;
    }

    private static void addPackageDependency(
            Set<Edge> edgesToRender, PackageDoc source, PackageDoc target) {
        if (source != target && source.isIncluded() && target.isIncluded()) {
            edgesToRender.add(
                    new Edge(EdgeType.DEPENDENCY, source, target));
        }
    }

    private static boolean isIndirectlyReachable(Map<Doc, Set<Doc>> dependencyGraph, Doc source, Doc target) {
        Set<Doc> intermediaryTargets = dependencyGraph.get(source);
        if (intermediaryTargets == null || intermediaryTargets.isEmpty()) {
            return false;
        }

        Set<Doc> visited = new HashSet<Doc>();
        visited.add(source);

        for (Doc t: intermediaryTargets) {
            if (t == target) {
                continue;
            }
            if (isIndirectlyReachable(dependencyGraph, t, target, visited)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isIndirectlyReachable(Map<Doc, Set<Doc>> dependencyGraph, Doc source, Doc target, Set<Doc> visited) {
        if (visited.contains(source)) {
            // Evade cyclic dependency.
            return false;
        }
        visited.add(source);

        Set<Doc> intermediaryTargets = dependencyGraph.get(source);
        if (intermediaryTargets == null || intermediaryTargets.isEmpty()) {
            return false;
        }

        for (Doc t: intermediaryTargets) {
            if (t == target) {
                return true;
            }

            if (isIndirectlyReachable(dependencyGraph, t, target, visited)) {
                return true;
            }
        }
        return false;
    }

    public String getPackageSummaryDiagram(PackageDoc pkg) {
        StringBuilder buf = new StringBuilder(16384);
        buf.append(
                "digraph APIVIZ {" + NEWLINE +
                "rankdir=LR;" + NEWLINE +
                "ranksep=0.3;" + NEWLINE +
                "nodesep=0.3;" + NEWLINE +
                "mclimit=1024;" + NEWLINE +
                "outputorder=edgesfirst;" + NEWLINE +
                "center=1;" + NEWLINE +
                "remincross=true;" + NEWLINE +
                "searchsize=65536;" + NEWLINE +
                "edge [fontsize=10, fontname=\"" + NORMAL_FONT + "\", " +
                "style=\"setlinewidth(0.6)\"]; " + NEWLINE +
                "node [shape=box, fontsize=10, fontname=\"" + NORMAL_FONT + "\", " +
                "width=0.1, height=0.1, style=\"setlinewidth(0.6)\"]; " + NEWLINE);

        Map<String, ClassDoc> nodesToRender = new TreeMap<String, ClassDoc>();

        Set<Edge> edgesToRender = new TreeSet<Edge>();

        for (ClassDoc node: nodes.values()) {
            fetchSubgraph(pkg, node, nodesToRender, edgesToRender, true, false, true);
        }

        renderSubgraph(pkg, null, buf, nodesToRender, edgesToRender, true);

        buf.append("}" + NEWLINE);

        return buf.toString();
    }

    private void fetchSubgraph(
            PackageDoc pkg, ClassDoc cls,
            Map<String, ClassDoc> nodesToRender, Set<Edge> edgesToRender,
            boolean useHidden, boolean useSee, boolean forceInherit) {

        if (useHidden && isHidden(cls)) {
            return;
        }

        if (forceInherit) {
            for (Tag t: pkg.tags(TAG_EXCLUDE)) {
                if (t.text() == null || t.text().trim().length() == 0) {
                    continue;
                }

                if (Pattern.compile(t.text().trim()).matcher(cls.qualifiedName()).find()) {
                    return;
                }
            }
        }

        if (cls.containingPackage() == pkg) {
            Set<Edge> directEdges = edges.get(cls);
            nodesToRender.put(cls.qualifiedName(), cls);
            for (Edge edge: directEdges) {
                if (!useSee && edge.getType() == SEE_ALSO) {
                    continue;
                }

                ClassDoc source = (ClassDoc) edge.getSource();
                ClassDoc target = (ClassDoc) edge.getTarget();

                boolean excluded = false;
                if (forceInherit || cls.tags(TAG_INHERIT).length > 0) {
                    for (Tag t: pkg.tags(TAG_EXCLUDE)) {
                        if (t.text() == null || t.text().trim().length() == 0) {
                            continue;
                        }

                        Pattern p = Pattern.compile(t.text().trim());

                        if (p.matcher(source.qualifiedName()).find()) {
                            excluded = true;
                            break;
                        }
                        if (p.matcher(target.qualifiedName()).find()) {
                            excluded = true;
                            break;
                        }
                    }
                    if (excluded) {
                        continue;
                    }
                }

                for (Tag t: cls.tags(TAG_EXCLUDE)) {
                    if (t.text() == null || t.text().trim().length() == 0) {
                        continue;
                    }

                    Pattern p = Pattern.compile(t.text().trim());

                    if (p.matcher(source.qualifiedName()).find()) {
                        excluded = true;
                        break;
                    }
                    if (p.matcher(target.qualifiedName()).find()) {
                        excluded = true;
                        break;
                    }
                }
                if (excluded) {
                    continue;
                }

                if (!useHidden || !isHidden(source) && !isHidden(target)) {
                    edgesToRender.add(edge);
                }
                if (!useHidden || !isHidden(source)) {
                    nodesToRender.put(source.qualifiedName(), source);
                }
                if (!useHidden || !isHidden(target)) {
                    nodesToRender.put(target.qualifiedName(), target);
                }
            }

            Set<Edge> reversedDirectEdges = reversedEdges.get(cls);
            if (reversedDirectEdges != null) {
                for (Edge edge: reversedDirectEdges) {
                    if (!useSee && edge.getType() == SEE_ALSO) {
                        continue;
                    }

                    if (cls.tags(TAG_EXCLUDE_SUBTYPES).length > 0 &&
                            (edge.getType() == EdgeType.GENERALIZATION ||
                             edge.getType() == EdgeType.REALIZATION)) {
                        continue;
                    }

                    ClassDoc source = (ClassDoc) edge.getSource();
                    ClassDoc target = (ClassDoc) edge.getTarget();

                    boolean excluded = false;
                    if (forceInherit || cls.tags(TAG_INHERIT).length > 0) {
                        for (Tag t: pkg.tags(TAG_EXCLUDE)) {
                            if (t.text() == null || t.text().trim().length() == 0) {
                                continue;
                            }

                            Pattern p = Pattern.compile(t.text().trim());

                            if (p.matcher(source.qualifiedName()).find()) {
                                excluded = true;
                                break;
                            }
                            if (p.matcher(target.qualifiedName()).find()) {
                                excluded = true;
                                break;
                            }
                        }
                        if (excluded) {
                            continue;
                        }
                    }

                    for (Tag t: cls.tags(TAG_EXCLUDE)) {
                        if (t.text() == null || t.text().trim().length() == 0) {
                            continue;
                        }

                        Pattern p = Pattern.compile(t.text().trim());

                        if (p.matcher(source.qualifiedName()).find()) {
                            excluded = true;
                            break;
                        }
                        if (p.matcher(target.qualifiedName()).find()) {
                            excluded = true;
                            break;
                        }
                    }
                    if (excluded) {
                        continue;
                    }

                    if (!useHidden || !isHidden(source) && !isHidden(target)) {
                        edgesToRender.add(edge);
                    }
                    if (!useHidden || !isHidden(source)) {
                        nodesToRender.put(source.qualifiedName(), source);
                    }
                    if (!useHidden || !isHidden(target)) {
                        nodesToRender.put(target.qualifiedName(), target);
                    }
                }
            }
        }
    }

    public String getClassDiagram(ClassDoc cls) {
        PackageDoc pkg = cls.containingPackage();

        StringBuilder buf = new StringBuilder(16384);
        Map<String, ClassDoc> nodesToRender = new TreeMap<String, ClassDoc>();
        Set<Edge> edgesToRender = new TreeSet<Edge>();

        fetchSubgraph(pkg, cls, nodesToRender, edgesToRender, false, true, false);

        buf.append("digraph APIVIZ {" + NEWLINE);

        // Determine the graph orientation automatically.
        int nodesAbove = 0;
        int nodesBelow = 0;
        for (Edge e: edgesToRender) {
            if (e.getType().isReversed()) {
                if (e.getSource() == cls) {
                    nodesAbove ++;
                } else {
                    nodesBelow ++;
                }
            } else {
                if (e.getSource() == cls) {
                    nodesBelow ++;
                } else {
                    nodesAbove ++;
                }
            }
        }

        boolean portrait;
        if (Math.max(nodesAbove, nodesBelow) <= 5) {
            // Landscape looks better usually up to 5.
            // There are just a few subtypes and supertypes.
            buf.append(
                    "rankdir=TB;" + NEWLINE +
                    "ranksep=0.4;" + NEWLINE +
                    "nodesep=0.3;" + NEWLINE);
            portrait = false;
        } else {
            // Portrait looks better.
            // There are too many subtypes or supertypes.
            buf.append(
                    "rankdir=LR;" + NEWLINE +
                    "ranksep=1.0;" + NEWLINE +
                    "nodesep=0.2;" + NEWLINE);
            portrait = true;
        }

        buf.append(
                "mclimit=128;" + NEWLINE +
                "outputorder=edgesfirst;" + NEWLINE +
                "center=1;" + NEWLINE +
                "remincross=true;" + NEWLINE +
                "searchsize=65536;" + NEWLINE +
                "edge [fontsize=10, fontname=\"" + NORMAL_FONT + "\", " +
                "style=\"setlinewidth(0.6)\"]; " + NEWLINE +
                "node [shape=box, fontsize=10, fontname=\"" + NORMAL_FONT + "\", " +
                "width=0.1, height=0.1, style=\"setlinewidth(0.6)\"]; " + NEWLINE);

        renderSubgraph(pkg, cls, buf, nodesToRender, edgesToRender, portrait);

        buf.append("}" + NEWLINE);

        return buf.toString();
    }

    private void renderSubgraph(PackageDoc pkg, ClassDoc cls,
            StringBuilder buf, Map<String, ClassDoc> nodesToRender,
            Set<Edge> edgesToRender, boolean portrait) {

        List<ClassDoc> nodesToRenderCopy = new ArrayList<ClassDoc>(nodesToRender.values());
        Collections.sort(nodesToRenderCopy, new ClassDocComparator(portrait));

        for (ClassDoc node: nodesToRenderCopy) {
            renderClass(pkg, cls, buf, node);
        }

        for (Edge edge: edgesToRender) {
            renderEdge(pkg, buf, edge);
        }
    }

    private static void renderPackage(
            StringBuilder buf, PackageDoc pkg, int prefixLen) {

        String href = pkg.name().replace('.', '/') + "/package-summary.html";
        buf.append(getNodeId(pkg));
        buf.append(" [label=\"");
        buf.append(pkg.name().substring(prefixLen));
        buf.append("\", style=\"filled");
        if (pkg.tags("@deprecated").length > 0) {
            buf.append(",dotted");
        }
        buf.append("\", fillcolor=\"");
        buf.append(getFillColor(pkg));
        buf.append("\", href=\"");
        buf.append(href);
        buf.append("\"];");
        buf.append(NEWLINE);
    }

    private static void renderClass(PackageDoc pkg, ClassDoc cls, StringBuilder buf, ClassDoc node) {
        String fillColor = getFillColor(pkg, cls, node);
        String lineColor = getLineColor(pkg, node);
        String fontColor = getFontColor(pkg, node);
        String href = getPath(pkg, node);

        buf.append(getNodeId(node));
        buf.append(" [label=\"");
        buf.append(getNodeLabel(pkg, node));
        buf.append("\", tooltip=\"");
        buf.append(escape(getNodeLabel(pkg, node)));
        buf.append("\"");
        if (node.isAbstract() && !node.isInterface()) {
            buf.append(", fontname=\"");
            buf.append(ITALIC_FONT);
            buf.append("\"");
        }
        buf.append(", style=\"filled");
        if (node.tags("@deprecated").length > 0) {
            buf.append(",dotted");
        }
        buf.append("\", color=\"");
        buf.append(lineColor);
        buf.append("\", fontcolor=\"");
        buf.append(fontColor);
        buf.append("\", fillcolor=\"");
        buf.append(fillColor);

        if (href != null) {
            buf.append("\", href=\"");
            buf.append(href);
        }

        buf.append("\"];");
        buf.append(NEWLINE);
    }

    private void renderEdge(PackageDoc pkg, StringBuilder buf, Edge edge) {
        EdgeType type = edge.getType();
        String lineColor = getLineColor(pkg, edge);
        String fontColor = getFontColor(pkg, edge);

        // Graphviz lays out nodes upside down - adjust for
        // important relationships.
        boolean reverse = edge.getType().isReversed();

        if (reverse) {
            buf.append(getNodeId(edge.getTarget()));
            buf.append(" -> ");
            buf.append(getNodeId(edge.getSource()));
            buf.append(" [arrowhead=\"");
            buf.append(type.getArrowTail());
            buf.append("\", arrowtail=\"");
            buf.append(type.getArrowHead() == null? (edge.isOneway()? "open" : "none") : type.getArrowHead());
        } else {
            buf.append(getNodeId(edge.getSource()));
            buf.append(" -> ");
            buf.append(getNodeId(edge.getTarget()));
            buf.append(" [arrowhead=\"");
            buf.append(type.getArrowHead() == null? (edge.isOneway()? "open" : "none") : type.getArrowHead());
            buf.append("\", arrowtail=\"");
            buf.append(type.getArrowTail());
        }

        buf.append("\", style=\"" + type.getStyle());
        buf.append("\", color=\"");
        buf.append(lineColor);
        buf.append("\", fontcolor=\"");
        buf.append(fontColor);
        buf.append("\", label=\"");
        buf.append(escape(edge.getEdgeLabel()));
        buf.append("\", headlabel=\"");
        buf.append(escape(edge.getTargetLabel()));
        buf.append("\", taillabel=\"");
        buf.append(escape(edge.getSourceLabel()));
        buf.append("\" ];");
        buf.append(NEWLINE);
    }

    private static String getStereotype(ClassDoc node) {
        String stereotype = node.isInterface()? "interface" : null;
        if (node.isException() || node.isError()) {
            stereotype = "exception";
        } else if (node.isAnnotationType()) {
            stereotype = "annotation";
        } else if (node.isEnum()) {
            stereotype = "enum";
        } else if (isStaticType(node)) {
            stereotype = "static";
        }

        if (node.tags(TAG_STEREOTYPE).length > 0) {
            stereotype = node.tags(TAG_STEREOTYPE)[0].text();
        }

        return escape(stereotype);
    }

    static boolean isStaticType(ClassDoc node) {
        boolean staticType = true;
        int methods = 0;
        for (MethodDoc m: node.methods()) {
            if (m.isConstructor()) {
                continue;
            }
            methods ++;
            if (!m.isStatic()) {
                staticType = false;
                break;
            }
        }

        return staticType && methods > 0;
    }

    private static String getFillColor(PackageDoc pkg) {
        String color = "white";
        if (pkg.tags(TAG_LANDMARK).length > 0) {
            color = "khaki1";
        }
        return color;
    }

    private static String getFillColor(PackageDoc pkg, ClassDoc cls, ClassDoc node) {
        String color = "white";
        if (cls == null) {
            if (node.containingPackage() == pkg && node.tags(TAG_LANDMARK).length > 0) {
                color = "khaki1";
            }
        } else if (cls == node) {
            color = "khaki1";
        }
        return color;
    }

    private static String getLineColor(PackageDoc pkg, ClassDoc doc) {
        String color = "black";
        if (!(doc.containingPackage() == pkg)) {
            color = "gray";
        }
        return color;
    }

    private static String getLineColor(PackageDoc pkg, Edge edge) {
        if (edge.getTarget() instanceof ClassDoc) {
            return getLineColor(pkg, (ClassDoc) edge.getTarget());
        } else {
            return "black";
        }
    }

    private static String getFontColor(PackageDoc pkg, ClassDoc doc) {
        String color = "black";
        if (!(doc.containingPackage() == pkg)) {
            color = "gray30";
        }
        return color;
    }

    private static String getFontColor(PackageDoc pkg, Edge edge) {
        if (edge.getTarget() instanceof ClassDoc) {
            return getFontColor(pkg, (ClassDoc) edge.getTarget());
        } else {
            return "black";
        }
    }

    private static String getNodeId(Doc node) {
        String name;
        if (node instanceof ClassDoc) {
            name = ((ClassDoc) node).qualifiedName();
        } else {
            name = node.name();
        }
        return name.replace('.', '_');
    }

    private static String getNodeLabel(PackageDoc pkg, ClassDoc node) {
        StringBuilder buf = new StringBuilder(256);
        String stereotype = getStereotype(node);
        if (stereotype != null) {
            buf.append("&#171;");
            buf.append(stereotype);
            buf.append("&#187;\\n");
        }

        if (node.containingPackage() == pkg) {
            buf.append(node.name());
        } else {
            String name = node.qualifiedName();
            int dotIndex = name.lastIndexOf('.');
            if (dotIndex < 0) {
                buf.append(name);
            } else {
                buf.append(name.substring(dotIndex + 1));
                buf.append("\\n(");
                buf.append(name.substring(0, dotIndex));
                buf.append(')');
            }
        }
        return buf.toString();
    }

    private static String escape(String text) {
        // Escape some characters to prevent syntax errors.
        if (text != null) {
            text = text.replaceAll("" +
            		"(\"|'|\\\\.?|\\s)+", " ");
        }
        return text;
    }

    private static String getPath(PackageDoc pkg, ClassDoc node) {
        if (!node.isIncluded()) {
            return null;
        }

        String sourcePath = pkg.name().replace('.', '/');
        String targetPath = node.qualifiedName().replace('.', '/') + ".html";
        String[] sourcePathElements = sourcePath.split("[\\/\\\\]+");
        String[] targetPathElements = targetPath.split("[\\/\\\\]+");

        int maxCommonLength = Math.min(sourcePathElements.length, targetPathElements.length);
        int commonLength;
        for (commonLength = 0; commonLength < maxCommonLength; commonLength ++) {
            if (!sourcePathElements[commonLength].equals(targetPathElements[commonLength])) {
                break;
            }
        }

        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < sourcePathElements.length - commonLength; i ++) {
            buf.append("/..");
        }

        for (int i = commonLength; i < targetPathElements.length; i ++) {
            buf.append('/');
            buf.append(targetPathElements[i]);
        }
        return buf.substring(1);
    }
}
