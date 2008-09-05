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

/**
 * @author The APIviz Project (apiviz-dev@lists.jboss.org)
 * @author Trustin Lee (tlee@redhat.com)
 *
 * @version $Rev$, $Date$
 *
 */
public enum EdgeType {
    GENERALIZATION("enormal", "solid"),
    REALIZATION("enormal", "dashed"),
    DEPENDENCY("open", "dashed"),
    NAVIGABILITY(null, "solid"),
    AGGREGATION("open", "solid", "ediamond"),
    COMPOSITION("open", "solid", "diamond"),
    SEE_ALSO("none", "solid");

    private final String arrowHead;
    private final String style;
    private final String arrowTail;

    private EdgeType(String arrowHead, String style) {
        this(arrowHead, style, "none");
    }

    private EdgeType(String arrowHead, String style, String arrowTail) {
        this.arrowHead = arrowHead;
        this.style = style;
        this.arrowTail = arrowTail;
    }

    public String getArrowHead() {
        return arrowHead;
    }

    public String getStyle() {
        return style;
    }

    public String getArrowTail() {
        return arrowTail;
    }
}
