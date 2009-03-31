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
    /* apiviz.excludeSubtypes */
    public static final String TAG_EXCLUDE_SUBTYPES = TAG_PREFIX + "excludeSubtypes";
    /* apiviz.inherit */
    public static final String TAG_INHERIT = TAG_PREFIX + "inherit";

    /* apiviz.category <categoryname>*/
    public static final String TAG_CATEGORY = TAG_PREFIX + "category";

    // Options

    public static final String OPTION_NO_PACKAGE_DIAGRAM  = "-nopackagediagram";
    public static final String OPTION_SOURCE_CLASS_PATH   = "-sourceclasspath";
    public static final String OPTION_CATEGORY = "-category";

    private Constant() {
        // Unused
    }
}
