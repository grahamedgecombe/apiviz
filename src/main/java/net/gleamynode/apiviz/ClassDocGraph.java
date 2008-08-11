/*
 * Copyright (C) 2008  Trustin Heuiseung Lee
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, 5th Floor, Boston, MA 02110-1301 USA
 */
package net.gleamynode.apiviz;

import static net.gleamynode.apiviz.Constant.*;
import static net.gleamynode.apiviz.EdgeType.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Doc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.SeeTag;
import com.sun.javadoc.Tag;

/**
 * @author The APIviz Project (http://apiviz.googlecode.com/)
 * @author Trustin Lee (http://gleamynode.net/)
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

        // Apply custom doclet tags first.
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

    public String getOverviewSummaryDiagram() {
        Map<String, PackageDoc> packages = new TreeMap<String, PackageDoc>();
        Set<Edge> edgesToRender = new TreeSet<Edge>();
        addPackageDependencies(packages, edgesToRender);

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

        int prefixLen;
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

        StringBuilder buf = new StringBuilder(16384);
        buf.append(
                "digraph APIVIZ {" + NEWLINE +
                "rankdir=RL;" + NEWLINE +
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

    @SuppressWarnings("deprecation")
    private void addPackageDependencies(
            Map<String, PackageDoc> packages, Set<Edge> edgesToRender) {

        for (ClassDoc node: nodes.values()) {
            if (!node.isIncluded()) {
                continue;
            }

            PackageDoc pkg = node.containingPackage();
            packages.put(pkg.name(), pkg);

            // Generate dependency nodes from known relationships.
            addPackageDependency(edgesToRender, edges.get(node));
            addPackageDependency(edgesToRender, reversedEdges.get(node));

            // And then try all fields and parameter types.
            for (FieldDoc f: node.fields()) {
                if (f.type().asClassDoc() != null) {
                    addPackageDependency(edgesToRender, pkg, f.type().asClassDoc().containingPackage());
                }
            }

            // And all methods.
            for (MethodDoc m: node.methods()) {
                if (m.returnType().asClassDoc() != null) {
                    addPackageDependency(edgesToRender, pkg, m.returnType().asClassDoc().containingPackage());
                }
                for (Parameter p: m.parameters()) {
                    if (p.type().asClassDoc() != null) {
                        addPackageDependency(edgesToRender, pkg, p.type().asClassDoc().containingPackage());
                    }
                }
            }

            // This is likely to be removed in the future.. but this is the
            // most precise way to figure out the dependencies in JavaDoc.
            for (PackageDoc p: node.importedPackages()) {
                addPackageDependency(edgesToRender, pkg, p);
            }
            for (ClassDoc c: node.importedClasses()) {
                addPackageDependency(edgesToRender, pkg, c.containingPackage());
            }
        }
    }

    private static void addPackageDependency(
            Set<Edge> edgesToRender, Set<Edge> candidates) {
        if (candidates == null) {
            return;
        }
        for (Edge edge: candidates) {
            if (edge.getType() == SEE_ALSO) {
                continue;
            }
            PackageDoc source = ((ClassDoc) edge.getSource()).containingPackage();
            PackageDoc target = ((ClassDoc) edge.getTarget()).containingPackage();
            addPackageDependency(edgesToRender, source, target);
        }
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
                "rankdir=RL;" + NEWLINE +
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

        renderSubgraph(pkg, null, buf, nodesToRender, edgesToRender);

        buf.append("}" + NEWLINE);

        return buf.toString();
    }

    private void fetchSubgraph(
            PackageDoc pkg, ClassDoc cls,
            Map<String, ClassDoc> nodesToRender, Set<Edge> edgesToRender,
            boolean useHidden, boolean useSee, boolean forceInherit) {

        if (useHidden && cls.tags(TAG_HIDDEN).length > 0) {
            return;
        }

        for (Tag t: pkg.tags(TAG_EXCLUDE)) {
            if (Pattern.compile(t.text().trim()).matcher(cls.qualifiedName()).find()) {
                return;
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

                edgesToRender.add(edge);
                nodesToRender.put(source.qualifiedName(), source);
                nodesToRender.put(target.qualifiedName(), target);
            }

            Set<Edge> reversedDirectEdges = reversedEdges.get(cls);
            if (reversedDirectEdges != null) {
                for (Edge edge: reversedDirectEdges) {
                    if (!useSee && edge.getType() == SEE_ALSO) {
                        continue;
                    }

                    ClassDoc source = (ClassDoc) edge.getSource();
                    ClassDoc target = (ClassDoc) edge.getTarget();

                    boolean excluded = false;
                    if (forceInherit || cls.tags(TAG_INHERIT).length > 0) {
                        for (Tag t: pkg.tags(TAG_EXCLUDE)) {
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

                    edgesToRender.add(edge);
                    nodesToRender.put(source.qualifiedName(), source);
                    nodesToRender.put(target.qualifiedName(), target);
                }
            }
        }
    }

    public String getClassDiagram(ClassDoc cls) {
        PackageDoc pkg = cls.containingPackage();

        StringBuilder buf = new StringBuilder(16384);
        buf.append(
                "digraph APIVIZ {" + NEWLINE +
                "rankdir=BT;" + NEWLINE +
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

        Map<String, ClassDoc> nodesToRender = new TreeMap<String, ClassDoc>();
        Set<Edge> edgesToRender = new TreeSet<Edge>();

        fetchSubgraph(pkg, cls, nodesToRender, edgesToRender, false, true, false);
        renderSubgraph(pkg, cls, buf, nodesToRender, edgesToRender);

        buf.append("}" + NEWLINE);

        return buf.toString();
    }

    private static void renderSubgraph(PackageDoc pkg, ClassDoc cls,
            StringBuilder buf, Map<String, ClassDoc> nodesToRender,
            Set<Edge> edgesToRender) {
        for (ClassDoc node: nodesToRender.values()) {
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
        String stereotype = getStereotype(node);
        String fillColor = getFillColor(pkg, cls, node);
        String lineColor = getLineColor(pkg, node);
        String fontColor = getFontColor(pkg, node);
        String href = getPath(pkg, node);

        buf.append(getNodeId(node));
        buf.append(" [label=\"");
        if (stereotype != null) {
            buf.append("&#171;");
            buf.append(stereotype);
            buf.append("&#187;\\n");
        }
        buf.append(getNodeLabel(pkg, node));
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

    private static void renderEdge(PackageDoc pkg, StringBuilder buf, Edge edge) {
        EdgeType type = edge.getType();
        String lineColor = getLineColor(pkg, edge);
        String fontColor = getFontColor(pkg, edge);

        buf.append(getNodeId(edge.getSource()));
        buf.append(" -> ");
        buf.append(getNodeId(edge.getTarget()));
        buf.append(" [arrowhead=\"");
        buf.append(type.getArrowHead() == null? (edge.isOneway()? "open" : "none") : type.getArrowHead());
        buf.append("\", arrowTail=\"");
        buf.append(type.getArrowTail());
        buf.append("\", style=\"" + type.getStyle());
        buf.append("\", color=\"");
        buf.append(lineColor);
        buf.append("\", fontcolor=\"");
        buf.append(fontColor);
        buf.append("\", label=\"");
        buf.append(edge.getEdgeLabel());
        buf.append("\", headlabel=\"");
        buf.append(edge.getTargetLabel());
        buf.append("\", taillabel=\"");
        buf.append(edge.getSourceLabel());
        buf.append("\" ];");
        buf.append(NEWLINE);
    }

    private static String getStereotype(ClassDoc node) {
        String stereotype = node.isInterface()? "interface" : null;
        if (node.isException()) {
            stereotype = "exception";
        } else if (node.isAnnotationType()) {
            stereotype = "annotation";
        } else if (node.isEnum()) {
            stereotype = "enum";
        } else {
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
            if (staticType && methods > 0) {
                stereotype = "static";
            }
        }

        if (node.tags(TAG_STEREOTYPE).length > 0) {
            stereotype = node.tags(TAG_STEREOTYPE)[0].text();
        }

        return stereotype;
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
        if (node.containingPackage() == pkg) {
            return node.name();
        }

        String name = node.qualifiedName();
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex < 0) {
            return name;
        } else {
            return name.substring(dotIndex + 1) + "\\n(" +
                   name.substring(0, dotIndex) + ')';
        }
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
