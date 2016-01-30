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
 */
public enum EdgeType {
    GENERALIZATION("onormal", "solid", true),
    REALIZATION("onormal", "setlinewidth(1.5), dotted", true),
    DEPENDENCY("open", "setlinewidth(1.5), dotted", true),
    AGGREGATION("open", "solid", "odiamond", false),
    COMPOSITION("open", "solid", "diamond", false),
    NAVIGABILITY(null, "solid", false),
    SEE_ALSO("none", "solid", false);

    private final String arrowHead;
    private final String style;
    private final String arrowTail;
    private final boolean reversed;

    private EdgeType(String arrowHead, String style, boolean reversed) {
        this(arrowHead, style, "none", reversed);
    }

    private EdgeType(String arrowHead, String style, String arrowTail, boolean reversed) {
        this.arrowHead = arrowHead;
        this.style = style;
        this.arrowTail = arrowTail;
        this.reversed = reversed;
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

    public boolean isReversed() {
        return reversed;
    }
}
