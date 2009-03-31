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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.sun.javadoc.RootDoc;

/**
 * @author The APIviz Project (apiviz-dev@lists.jboss.org)
 * @author Trustin Lee (tlee@redhat.com)
 *
 * @version $Rev$, $Date$
 *
 */
public class Graphviz {

    private static File home;

    public static boolean isAvailable(RootDoc root) {
        String executable = Graphviz.getExecutable(root);
        File home = Graphviz.getHome(root);

        ProcessBuilder pb = new ProcessBuilder(executable, "-V");
        pb.redirectErrorStream(true);
        if (home != null) {
            root.printNotice("Graphviz Home: " + home);
            pb.directory(home);
        }
        root.printNotice("Graphviz Executable: " + executable);

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
                if (line.matches("^.*[Gg][Rr][Aa][Pp][Hh][Vv][Ii][Zz].*$")) {
                    root.printNotice("Found: " + line);
                    return true;
                } else {
                    root.printWarning("Unknown Graphviz output: " + line);
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
            RootDoc root,
            String diagram, File outputDirectory, String filename) throws IOException {

        File pngFile = new File(outputDirectory, filename + ".png");
        File mapFile = new File(outputDirectory, filename + ".map");

        pngFile.delete();
        mapFile.delete();

        ProcessBuilder pb = new ProcessBuilder(
                Graphviz.getExecutable(root),
                "-Tcmapx", "-o", mapFile.getAbsolutePath(),
                "-Tpng",   "-o", pngFile.getAbsolutePath());
        pb.redirectErrorStream(true);
        File home = Graphviz.getHome(root);
        if (home != null) {
            pb.directory(home);
        }

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

    private static String getExecutable(RootDoc root) {
        String command = "dot";

        try {
            String osName = System.getProperty("os.name");
            if (osName != null && osName.indexOf("Windows") >= 0) {
                File path = Graphviz.getHome(root);
                if (path != null) {
                    command = path.getAbsolutePath() + File.separator
                            + "dot.exe";
                } else {
                    command = "dot.exe";
                }
            }
        } catch (Exception e) {
            // ignore me!
        }
        return command;
    }

    private static File getHome(RootDoc root) {
        if (home != null) {
            return home;
        }

        File graphvizDir = null;
        try {
            String graphvizHome = System.getProperty("graphviz.home");
            if (graphvizHome != null) {
                root.printNotice(
                        "Using the 'graphviz.home' system property: " +
                        graphvizHome);
            } else {
                root.printNotice(
                        "The 'graphviz.home' system property was not specified.");

                graphvizHome = System.getenv("GRAPHVIZ_HOME");
                if (graphvizHome != null) {
                    root.printNotice(
                            "Using the 'GRAPHVIZ_HOME' environment variable: " +
                            graphvizHome);
                } else {
                    root.printNotice(
                            "The 'GRAPHVIZ_HOME' environment variable was not specified.");
                }
            }
            if (graphvizHome != null) {
                graphvizDir = new File(graphvizHome);
                if (!graphvizDir.exists() || !graphvizDir.isDirectory()) {
                    root.printWarning(
                            "The specified graphviz home directory does not exist: " +
                            graphvizDir.getPath());
                    return null;
                }
            } else {
                root.printNotice(
                        "System path will be used as graphviz home directory was not specified.");
            }
        } catch (Exception e) {
            // ignore...
        }
        return home = graphvizDir;
    }

    private Graphviz() {
        // Unused
    }
}
