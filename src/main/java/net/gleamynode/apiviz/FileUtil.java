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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author The APIviz Project (http://apiviz.googlecode.com/)
 * @author Trustin Lee (http://gleamynode.net/)
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
