package io.github.pastthepixels.freepaint.File;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Path;

import androidx.core.graphics.PathParser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import io.github.pastthepixels.freepaint.Graphics.DrawAppearance;
import io.github.pastthepixels.freepaint.Graphics.DrawCanvas;
import io.github.pastthepixels.freepaint.Graphics.ExtendedPath;

import androidx.graphics.path.PathIterator;
import androidx.graphics.path.PathSegment;

public class SVG {
    private final DrawCanvas canvas;

    private final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    private String data = "";

    /**
     * Creates a new SVG instance, but we need a canvas with paths to export/import to.
     *
     * @param canvas A DrawCanvas instance
     */
    public SVG(DrawCanvas canvas) {
        this.canvas = canvas;
    }

    /**
     * Creates an SVG as a String from <code>DrawCanvas.paths</code> and stores it as <code>SVG.data</code>
     */
    @SuppressLint("DefaultLocale")
    public void createSVG() {
        this.data = "";
        this.data += "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n";
        this.data += String.format(
                // https://www.w3.org/TR/SVGTiny12/painting.html#viewport-fill-property
                // should work, but not supported by most browsers so we use a style attribute as well.
                // Note that we only read viewport-fill
                // TODO: Instead of this, create/read a rect for maximum compatibility
                "<svg width=\"%d\" height=\"%d\" viewport-fill=\"%s\" style=\"background-color: %<s; stroke-width: 0px;\" xmlns=\"http://www.w3.org/2000/svg\">",
                (int) canvas.documentSize.x,
                (int) canvas.documentSize.y,
                DrawAppearance.colorToHex(canvas.documentColor)
        );
        for (ExtendedPath path : canvas.paths) {
            addPath(path);
        }
        // Closes the tag. We are done.
        this.data += "\n</svg>";
        System.out.println(this.data);
    }

    /**
     * Writes <code>SVG.data</code> to a file using an OutputStream.
     * Intended to be called by a DrawCanvas where the OutputStream is passed to it from <code>DrawCanvas.saveFile()</code>
     *
     * @param stream OutputStream passed to the SVG instance
     * @throws IOException In case something goes wrong from <code>OutputStream.write()</code>
     */
    public void writeFile(OutputStream stream) throws IOException {
        stream.write(data.getBytes());
        stream.close();
    }

    /**
     * Loads an SVG file from an InputStream and then parses it by calling <code>SVG.parseFile()</code>
     *
     * @param stream The InputStream to parse. The method is designed such that it would be passed from a DrawCanvas with <code>DrawCanvas.loadFile()</code>
     */
    public void loadFile(InputStream stream) {
        parseFile(
                (new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)))
                        .lines().collect(Collectors.joining("\n"))
        );
    }

    /**
     * Implementation of https://github.com/romainguy/pathway/blob/main/pathway/src/main/java/dev/romainguy/graphics/path/Svg.kt
     * with androidx.graphics.path and Java 8
     */
    public String toSvg(Path path) {
        PathIterator iterator = new PathIterator(path, PathIterator.ConicEvaluation.AsQuadratics, 1.0f);
        StringBuilder builder = new StringBuilder();
        PathSegment.Type lastType = PathSegment.Type.Done;
        float[] points = new float[8];

        while(iterator.hasNext()) {
            PathSegment.Type type = iterator.next(points);
            switch(type) {
                case Move:
                    builder.append(toSvgCommand(PathSegment.Type.Move, lastType)).append(points[0]).append(" ").append(points[1]);
                    break;

                case Line:
                    builder.append(toSvgCommand(PathSegment.Type.Line, lastType)).append(points[2]).append(" ").append(points[3]);
                    break;

                case Quadratic:
                    builder.append(toSvgCommand(PathSegment.Type.Quadratic, lastType)).append(points[2]).append(" ").append(points[3]).append(" ").append(points[4]).append(" ").append(points[5]);
                    break;

                case Cubic:
                    builder.append(toSvgCommand(PathSegment.Type.Cubic, lastType));
                    builder.append(points[2]).append(" ").append(points[3]).append(" ");
                    builder.append(points[4]).append(" ").append(points[5]).append(" ");
                    builder.append(points[6]).append(" ").append(points[7]);
                    break;

                case Close:
                    builder.append(toSvgCommand(PathSegment.Type.Close, lastType));
                    break;

                case Conic:
                case Done:
                    continue;
            }
            lastType = type;
            builder.append(" ");
        }

        return builder.toString();
    }

    /**
     * Implementation of https://github.com/romainguy/pathway/blob/main/pathway/src/main/java/dev/romainguy/graphics/path/Svg.kt
     * with androidx.graphics.path and Java 8
     */
    public String toSvgCommand(PathSegment.Type type, PathSegment.Type lastType) {
        if (type != lastType) {
            switch(type) {
                case Move:
                    return "M";

                case Line:
                    return "L";

                case Quadratic:
                    return "Q";

                case Cubic:
                    return "C";

                case Close:
                    return "Z";
            }
        }
        return "";
    }

    /**
     * Converts a DrawPath to SVG data as a string (using the <code>path</code> element)
     * and concatenates it to <code>SVG.data</code>.
     *
     * @param path The DrawPath to convert to SVG data.
     */
    @SuppressLint("DefaultLocale")
    public void addPath(ExtendedPath path) {
        StringBuilder data = new StringBuilder("\n<path d=\"");
        // Step 1. Add points.
        // TODO: pathway is broken, use a androidx.graphics.path.PathIterator and do it yourself :(
        data.append(toSvg(path));
        data.append("\" ");
        // Step 2. Set the appearance of the path.
        // Inkscape only accepts hex colors, I don't know why
        String fillValue = path.appearance.fill == -1 ? "none" : DrawAppearance.colorToHex(path.appearance.fill);
        String strokeValue = path.appearance.stroke == -1 ? "none" : DrawAppearance.colorToHex(path.appearance.stroke);
        data.append(String.format("fill=\"%s\" ", fillValue));
        data.append(String.format("stroke=\"%s\" ", strokeValue));
        if (path.appearance.fill != -1)
            data.append(String.format("fill-opacity=\"%f\" ", DrawAppearance.getColorAlpha(path.appearance.fill)));
        if (path.appearance.stroke != -1)
            data.append(String.format("stroke-opacity=\"%f\" ", DrawAppearance.getColorAlpha(path.appearance.stroke)));
        data.append(String.format("stroke-width=\"%s\" ", path.appearance.strokeSize));
        data.append("stroke-linecap=\"round\" ");
        // Done
        this.data += data + "/>";
    }


    /**
     * Parses an SVG, as a String, directly affecting the canvas of which it is bound to.
     * <p>
     * Note that FreePaint right now only supports Path elements with certain path commands
     * (see SVG.parsePath).
     * In the future, getting more coverage of the SVG spec shouldn't be too hard (ex.
     * polylines are really similar to paths).
     *
     * @param data The SVG as a String (ex. <code>"\<svg\>\<path\/\>\<\/\svg\>"</code>
     */
    public void parseFile(String data) {
        System.out.println("**READING**");
        System.out.println(data);

        // Loads the document.
        Document document;
        try {
            document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(data.getBytes()));
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new RuntimeException(e);
        }

        // Set size/color
        canvas.documentSize.set(
                Float.parseFloat(document.getDocumentElement().getAttribute("width").replace("px", "")),
                Float.parseFloat(document.getDocumentElement().getAttribute("height").replace("px", ""))
        );
        if (document.getDocumentElement().hasAttribute("viewport-fill")) {
            canvas.documentColor = Color.parseColor(document.getDocumentElement().getAttribute("viewport-fill"));
        }
        // Add paths
        canvas.paths.clear();
        NodeList nodes = document.getElementsByTagName("path");
        for (int i = 0; i < nodes.getLength(); i++) {
            // TODO: use androidx.graphics.path.PathParser instead of doing it myself
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && ((Element) node).getTagName().equals("path")) {
                Element element = (Element) node;
                String pathData = element.getAttribute("d");
                ExtendedPath path = new ExtendedPath(PathParser.createPathFromPathData(pathData));
                if (pathData.contains("z") || pathData.contains("Z")) path.setIsClosed(true);
                // Fill/stroke
                path.appearance.stroke = path.appearance.fill = -1;
                float fillOpacity = element.hasAttribute("fill-opacity") ? Float.parseFloat(element.getAttribute("fill-opacity")) : 1;
                float strokeOpacity = element.hasAttribute("stroke-opacity") ? Float.parseFloat(element.getAttribute("stroke-opacity")) : 1;
                if (element.hasAttribute("fill") && !element.getAttribute("fill").equals("none")) {
                    int fill = Color.parseColor(element.getAttribute("fill"));
                    path.appearance.fill = Color.argb((int) (fillOpacity * 255), Color.red(fill), Color.green(fill), Color.blue(fill));
                }
                if (element.hasAttribute("stroke") && !element.getAttribute("stroke").equals("none")) {
                    int stroke = Color.parseColor(element.getAttribute("stroke"));
                    path.appearance.stroke = Color.argb((int) (strokeOpacity * 255), Color.red(stroke), Color.green(stroke), Color.blue(stroke));
                }
                // Stroke width
                if (element.hasAttribute("stroke-width")) {
                    path.appearance.strokeSize = Integer.parseInt(element.getAttribute("stroke-width"));
                }
                // Done!!
                canvas.paths.add(path);
            }
        }
        // Invalidate!
        canvas.invalidate();
    }

}
