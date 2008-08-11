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

/**
 * @author The APIviz Project (http://apiviz.googlecode.com/)
 * @author Trustin Lee (http://gleamynode.net/)
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
