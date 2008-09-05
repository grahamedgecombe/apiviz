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

import java.util.Comparator;

import com.sun.javadoc.ClassDoc;

/**
 * @author The APIviz Project (apiviz-dev@lists.jboss.org)
 * @author Trustin Lee (tlee@redhat.com)
 *
 * @version $Rev$, $Date$
 *
 */
class ClassDocComparator implements Comparator<ClassDoc> {

    private final boolean portrait;

    ClassDocComparator(boolean portrait) {
        this.portrait = portrait;
    }

    public int compare(ClassDoc a, ClassDoc b) {
        int precedenceDiff = getPrecedence(a) - getPrecedence(b);
        if (precedenceDiff != 0) {
            if (portrait) {
                return -precedenceDiff;
            } else {
                return precedenceDiff;
            }
        }

        return a.name().compareTo(b.name());
    }

    private static int getPrecedence(ClassDoc c) {
        if (c.isAnnotationType()) {
            return 0;
        }

        if (c.isEnum()) {
            return 1;
        }

        if (ClassDocGraph.isStaticType(c)) {
            return 2;
        }

        if (c.isInterface()) {
            return 3;
        }

        if (c.isAbstract()) {
            return 4;
        }

        if (c.isError() || c.isException()) {
            return 100;
        }

        return 50;
    }
}
