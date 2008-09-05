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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author The APIviz Project (apiviz-dev@lists.jboss.org)
 * @author Trustin Lee (tlee@redhat.com)
 *
 * @version $Rev$, $Date$
 *
 */
public class FileUtil {

    public static String readFile(File file) throws IOException {
        byte[] byteContent;
        RandomAccessFile in = new RandomAccessFile(file, "r");
        try {
            byteContent = new byte[(int) in.length()];
            in.readFully(byteContent);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                // Ignore.
            }
        }

        return new String(byteContent, "ISO-8859-1");
    }

    public static void writeFile(File file, String content) throws IOException {
        FileOutputStream out = new FileOutputStream(file);
        try {
            out.write(content.getBytes("ISO-8859-1"));
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                // Ignore.
            }
        }
    }

    private FileUtil() {
        // Unused
    }
}
