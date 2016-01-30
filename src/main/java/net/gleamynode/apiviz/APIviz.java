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
package net.gleamynode.apiviz;

import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.RootDoc;

/**
 * @author The APIviz Project (netty@googlegroups.com)
 * @author Trustin Lee (trustin@gmail.com)
 *
 */
public class APIviz {

    public static boolean start(RootDoc root) {
        return org.jboss.apiviz.APIviz.start(root);
    }

    public static boolean validOptions(String[][] options, DocErrorReporter errorReporter) {
        return org.jboss.apiviz.APIviz.validOptions(options, errorReporter);
    }

    public static int optionLength(String option) {
        return org.jboss.apiviz.APIviz.optionLength(option);
    }

    public static LanguageVersion languageVersion() {
        return org.jboss.apiviz.APIviz.languageVersion();
    }
}
