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

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author bsneade
 */
public class GraphvizTest {

    public GraphvizTest() {
    }

    @Test
    public void testExecutableRegularExpression() {
        //on my macbook
        assertTrue("dot - graphviz version 2.22.2 (20090313.1817)"
                .matches(Graphviz.GRAPHVIZ_EXECUTABLE_FIRST_LINE_CHECK));

        //on my linux desktop
        assertTrue("dot - Graphviz version 2.16 (Fri Feb  8 12:52:03 UTC 2008)"
                .matches(Graphviz.GRAPHVIZ_EXECUTABLE_FIRST_LINE_CHECK));
    }

}