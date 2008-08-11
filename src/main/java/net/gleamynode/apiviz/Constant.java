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
public class Constant {

    public static final String NEWLINE = System.getProperty("line.separator", "\n");
    public static final String NORMAL_FONT = "Helvetica";
    public static final String ITALIC_FONT = "Helvetica-Oblique";

    public static final String TAG_PREFIX = "@apiviz.";

    // TODO change relationship spec to edgelabel sourcelabel targetlabel
    // TODO Split apiviz.has into two tags

    /* apiviz.stereotype <name> */
    public static final String TAG_STEREOTYPE = TAG_PREFIX + "stereotype";
    /* apiviz.uses       <FQCN> [<sourceLabel> <targetLabel> [<edgeLabel>]] */
    public static final String TAG_USES = TAG_PREFIX + "uses";
    /* apiviz.has        <FQCN> [oneway] [<sourceLabel> <targetLabel> [<edgeLabel>]] */
    public static final String TAG_HAS = TAG_PREFIX + "has";
    /* apiviz.owns       <FQCN> [<sourceLabel> <targetLabel> [<edgeLabel>]] */
    public static final String TAG_OWNS = TAG_PREFIX + "owns";
    /* apiviz.composedOf <FQCN> [<sourceLabel> <targetLabel> [<edgeLabel>]] */
    public static final String TAG_COMPOSED_OF = TAG_PREFIX + "composedOf";
    /* apiviz.landmark */
    public static final String TAG_LANDMARK = TAG_PREFIX + "landmark";
    /* apiviz.hidden */
    public static final String TAG_HIDDEN = TAG_PREFIX + "hidden";
    /* apiviz.exclude <regex> */
    public static final String TAG_EXCLUDE = TAG_PREFIX + "exclude";
    /* apiviz.inherit */
    public static final String TAG_INHERIT = TAG_PREFIX + "inherit";

    private Constant() {
        // Unused
    }
}
