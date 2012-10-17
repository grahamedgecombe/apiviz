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

import com.sun.tools.doclets.standard.Standard;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author bsneade
 */
public class APIVizTest {

    public APIVizTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Test
    public void testOptionLength_OPTION_CATEGORY_FILL_COLOR() {
        assertEquals(2, APIviz.optionLength(Constant.OPTION_CATEGORY));
    }

    @Test
    public void testOptionLength_OPTION_SOURCE_CLASS_PATH() {
        assertEquals(2, APIviz.optionLength(Constant.OPTION_SOURCE_CLASS_PATH));
    }

    @Test
    public void testOptionLength_OPTION_NO_PACKAGE_DIAGRAM() {
        assertEquals(1, APIviz.optionLength(Constant.OPTION_NO_PACKAGE_DIAGRAM));
    }

    @Test
    public void testOptionLength_Not_Specified() {
        assertEquals(Standard.optionLength("Bleh"), APIviz.optionLength("Bleh"));
    }

    @Test
    public void testOptionLength_OPTION_HELP() {
        //not really a test, but we get to see the output
        APIviz.optionLength(Constant.OPTION_HELP);
    }
}
