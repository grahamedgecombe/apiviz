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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * @author The APIviz Project (http://apiviz.googlecode.com/)
 * @author Trustin Lee (http://gleamynode.net/)
 *
 * @version $Rev$, $Date$
 *
 */
public class Graphviz {

    public static boolean isAvailable() {
        ProcessBuilder pb = new ProcessBuilder("dot", "-V");
        pb.redirectErrorStream(true);

        Process p;
        try {
            p = pb.start();
        } catch (IOException e) {
            return false;
        }

        BufferedReader in = new BufferedReader(
                new InputStreamReader(p.getInputStream()));
        OutputStream out = p.getOutputStream();
        try {
            out.close();

            String line = null;
            while((line = in.readLine()) != null) {
                if (line.indexOf("Graphviz") >= 0) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            return false;
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                // Shouldn't happen.
            }

            try {
                in.close();
            } catch (IOException e) {
                // Shouldn't happen.
            }

            for (;;) {
                try {
                    p.waitFor();
                    break;
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
        }
    }

    public static void writeImageAndMap(
            String diagram, File outputDirectory, String filename) throws IOException {

        File pngFile = new File(outputDirectory, filename + ".png");
        File mapFile = new File(outputDirectory, filename + ".map");

        pngFile.delete();
        mapFile.delete();

        ProcessBuilder pb = new ProcessBuilder(
                "dot",
                "-Tcmapx", "-o", mapFile.getAbsolutePath(),
                "-Tpng",   "-o", pngFile.getAbsolutePath());
        pb.redirectErrorStream(true);

        Process p = pb.start();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(p.getInputStream()));
        Writer out = new OutputStreamWriter(p.getOutputStream(), "UTF-8");
        try {
            out.write(diagram);
            out.close();

            String line = null;
            while((line = in.readLine()) != null) {
                System.err.println(line);
            }
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                // Shouldn't happen.
            }

            try {
                in.close();
            } catch (IOException e) {
                // Shouldn't happen.
            }

            for (;;) {
                try {
                    int result = p.waitFor();
                    if (result != 0) {
                        throw new IllegalStateException("Graphviz exited with a non-zero return value: " + result);
                    }
                    break;
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
        }
    }

    private Graphviz() {
        // Unused
    }
}
