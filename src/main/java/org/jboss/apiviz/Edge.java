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

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Doc;
import com.sun.javadoc.RootDoc;

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

    private int calculateHashCode(){
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

    public Edge(RootDoc rootDoc, EdgeType type, Doc source, String spec) {
        if (spec == null) {
            spec = "";
        }

        this.type = type;
        this.source = source;

        String[] args = spec.replaceAll("\\s+", " ").trim().split(" ");
        for (int i = 1; i < Math.min(4, args.length); i ++) {
            if (args[i].equals("-")) {
                args[i] = "";
            }
        }

        if (args.length == 1) {
            target = rootDoc.classNamed(args[0]);
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
                for (int i = startIndex; i < args.length; i ++) {
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
}
