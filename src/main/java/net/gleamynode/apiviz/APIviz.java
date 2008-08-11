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

import static net.gleamynode.apiviz.Constant.*;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.RootDoc;
import com.sun.tools.doclets.standard.Standard;

/**
 * @author The APIviz Project (netty@googlegroups.com)
 * @author Trustin Lee (trustin@gmail.com)
 *
 * @version $Rev$, $Date$
 *
 */
public class APIviz {

    private static final Pattern INSERTION_POINT_PATTERN = Pattern.compile(
            "((<\\/PRE>)(?=\\s*<P>)|(?=<TABLE BORDER=\"1\"))");

    public static boolean start(RootDoc root) {
        root = new APIvizRootDoc(root);
        if (!Standard.start(root)) {
            return false;
        }

        if (!Graphviz.isAvailable()) {
            root.printWarning("Graphviz is not found in the system path.");
            root.printWarning("Skipping diagram generation.");
            return true;
        }

        try {
            File outputDirectory = getOutputDirectory(root.options());
            ClassDocGraph graph = new ClassDocGraph(root);
            generateOverviewSummary(root, graph, outputDirectory);
            generatePackageSummaries(root, graph, outputDirectory);
            generateClassDiagrams(root, graph, outputDirectory);
        } catch(Throwable t) {
            root.printError(
                    "An error occurred during diagram generation: " +
                    t.toString());
            t.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean validOptions(String[][] options, DocErrorReporter errorReporter) {
        return Standard.validOptions(options, errorReporter);
    }

    public static int optionLength(String option) {
        return Standard.optionLength(option);
    }

    public static LanguageVersion languageVersion() {
        return Standard.languageVersion();
    }

    private static void generateOverviewSummary(RootDoc root, ClassDocGraph graph, File outputDirectory) throws IOException {
        instrumentDiagram(
                root, outputDirectory, "overview-summary",
                graph.getOverviewSummaryDiagram());
    }

    private static void generatePackageSummaries(RootDoc root, ClassDocGraph graph, File outputDirectory) throws IOException {
        for (PackageDoc p: getPackages(root).values()) {
            instrumentDiagram(
                    root, outputDirectory,
                    p.name().replace('.', File.separatorChar) +
                    File.separatorChar + "package-summary",
                    graph.getPackageSummaryDiagram(p));
        }
    }

    private static void generateClassDiagrams(RootDoc root, ClassDocGraph graph, File outputDirectory) throws IOException {
        for (ClassDoc c: root.classes()) {
            instrumentDiagram(
                    root, outputDirectory,
                    c.qualifiedName().replace('.', File.separatorChar),
                    graph.getClassDiagram(c));
        }
    }

    private static Map<String, PackageDoc> getPackages(RootDoc root) {
        Map<String, PackageDoc> packages = new TreeMap<String, PackageDoc>();
        for (ClassDoc c: root.classes()) {
            PackageDoc p = c.containingPackage();
            if(!packages.containsKey(p.name())) {
                packages.put(p.name(), p);
            }
        }

        return packages;
    }

    private static void instrumentDiagram(RootDoc root, File outputDirectory, String filename, String diagram) throws IOException {
        File htmlFile = new File(outputDirectory, filename + ".html");
        File pngFile = new File(outputDirectory, filename + ".png");
        File mapFile = new File(outputDirectory, filename + ".map");

        if (!htmlFile.exists()) {
            // May be an inner class?
            for (;;) {
                int idx = filename.lastIndexOf(File.separatorChar);
                if (idx > 0) {
                    filename = filename.substring(0, idx) + '.' +
                               filename.substring(idx + 1);
                } else {
                    // Give up (maybe missing)
                    return;
                }
                htmlFile = new File(outputDirectory, filename + ".html");
                if (htmlFile.exists()) {
                    pngFile = new File(outputDirectory, filename + ".png");
                    mapFile = new File(outputDirectory, filename + ".map");
                    break;
                }
            }
        }

        root.printNotice("Generating " + pngFile + "...");
        Graphviz.writeImageAndMap(diagram, outputDirectory, filename);

        try {
            String oldContent = FileUtil.readFile(htmlFile);
            String mapContent = FileUtil.readFile(mapFile);

            Matcher matcher = INSERTION_POINT_PATTERN.matcher(oldContent);
            if (!matcher.find()) {
                throw new IllegalStateException(
                        "Failed to find an insertion point.");
            }
            String newContent =
                oldContent.substring(0, matcher.end()) +
                mapContent + NEWLINE +
                "<CENTER><IMG SRC=\"" + pngFile.getName() +
                "\" USEMAP=\"#APIVIZ\" BORDER=\"0\"></CENTER>" +
                NEWLINE + NEWLINE +
                oldContent.substring(matcher.end());
            FileUtil.writeFile(htmlFile, newContent);
        } finally {
            mapFile.delete();
        }
    }

    private static File getOutputDirectory(String[][] options) {
        for (String[] o: options) {
            if (o[0].equals("-d")) {
                return new File(o[1]);
            }
        }

        // Fall back to the current working directory.
        return new File(System.getProperty("user.dir", "."));
    }
}
