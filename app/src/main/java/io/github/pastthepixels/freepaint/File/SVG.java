package io.github.pastthepixels.freepaint.File;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;

import io.github.pastthepixels.freepaint.DrawAppearance;
import io.github.pastthepixels.freepaint.DrawCanvas;
import io.github.pastthepixels.freepaint.DrawPath;
import io.github.pastthepixels.freepaint.Point;

public class SVG {
    private final DrawCanvas canvas;

    private final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    private String data = "";

    private Point.COMMANDS command = Point.COMMANDS.none;

    private boolean isCommandRelative = false;

    private Point previousPoint = new Point(0, 0);

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
        for (DrawPath path : canvas.paths) {
            if (path.points.size() > 1) addPath(path);
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
     * Converts a DrawPath to SVG data as a string (using the <code>path</code> element)
     * and concatenates it to <code>SVG.data</code>.
     *
     * @param path The DrawPath to convert to SVG data.
     */
    @SuppressLint("DefaultLocale")
    public void addPath(DrawPath path) {
        StringBuilder data = new StringBuilder("\n<path d=\"");
        // Step 1. Add points.
        for (Point point : path.points) {
            String command = point.command == Point.COMMANDS.move ? "M" : "L";
            if (point == path.points.get(0)) command = "M";
            data.append(String.format("%s%.2f %.2f ", command, point.x, point.y));
        }
        if (path.isClosed) {
            data.append("Z");
        }
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
     *
     * @param data The SVG as a String (ex. <code>"\<svg\>\<path\/\>\<\/\svg\>"</code>
     */
    public void parseFile(String data) {
        System.out.println("**READING**");
        System.out.println(data);
        try {
            Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(data.getBytes()));
            // Set size/color
            canvas.documentSize.set(
                    parseFloat(document.getDocumentElement().getAttribute("width")),
                    parseFloat(document.getDocumentElement().getAttribute("height"))
            );
            if (document.getDocumentElement().hasAttribute("viewport-fill")) {
                canvas.documentColor = Color.parseColor(document.getDocumentElement().getAttribute("viewport-fill"));
            }
            // Add paths
            canvas.paths.clear();
            NodeList nodes = document.getElementsByTagName("path");
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE && ((Element) node).getTagName().equals("path")) {
                    Element element = (Element) node;
                    DrawPath path = new DrawPath(null);
                    path.isClosed = element.getAttribute("d").toUpperCase().contains("Z");
                    // Points
                    command = Point.COMMANDS.none;
                    path.points = svgToPointList(element.getAttribute("d"), path.points, 0);
                    // Fill/stroke
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
                    path.finalise();
                    canvas.paths.add(path);
                }
            }
            // Invalidate!
            canvas.invalidate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Recursive function that reads the <code>d</code> attribute on a <code>path</code> element,
     * as a string, and iterates through that to create a list of <code>io.github.pastthepixels.freepaint.Point</code>
     * instances.
     * TODO: Account for cases where there's no spacing, like in Material Design icons (material.io/icons)
     *
     * @param attribute     The full value of the <code>d</code> attribute of an SVG
     * @param currentPoints Current list of points generated from the function (pass an empty list when calling it)
     * @param index         Index of where the function is in reading the String <code>attribute</code>. (pass 0 when calling this function)
     * @return <code>currentPoints</code>.
     */
    public LinkedList<Point> svgToPointList(String attribute, LinkedList<Point> currentPoints, int index) {
        System.out.println(attribute);
        if (index + 1 >= attribute.length()) {
            return currentPoints;
        }
        // Pull the selected character to a variable
        String character = attribute.substring(index, index + 1);
        // Commands (can't use a switch statement because STRINGS!!!
        if (character.equalsIgnoreCase("M")) {
            command = Point.COMMANDS.move;
            isCommandRelative = character.equals("m");
        }
        if (character.equalsIgnoreCase("L") || (previousPoint != null && previousPoint.command == Point.COMMANDS.move && command == Point.COMMANDS.none && character.equals(" "))) {
            // Apparently a set of coords with no command with them *after* an M
            // is interpreted as an L
            command = Point.COMMANDS.line;
            isCommandRelative = character.equals("l");
        }
        if (character.equalsIgnoreCase("H")) {
            command = Point.COMMANDS.horizontal;
            isCommandRelative = character.equals("h");
        }
        if (character.equalsIgnoreCase("V")) {
            command = Point.COMMANDS.vertical;
            isCommandRelative = character.equals("v");
        }
        if (character.equals("-") || isFloat(character)) {
            // Iterates through the rest of the string until we have two numbers
            String[] point = {"", ""};
            int pointIndex = 0;
            while (
                    index + 1 < attribute.length() &&
                            !(!point[1].equals("") && (character.equals(" ") || character.equals(",")))
            ) {
                character = attribute.substring(index, index + 1);
                // If the character's not a dot (decimal), not a negative symbol, and not a float...
                if (character.equals(" ") || character.equals(",")) {
                    if (command == Point.COMMANDS.vertical || command == Point.COMMANDS.horizontal) {
                        // Vertical line == Y value, no X value
                        if (command == Point.COMMANDS.vertical) {
                            point[1] = point[0];
                            point[0] = "";
                        }
                        // If we have a command that's just supposed to take in one float, end this here
                        command = Point.COMMANDS.line;
                        break;
                    }
                    pointIndex++;
                    index++;
                    continue;
                }
                point[pointIndex] += character;
                index++;
            }
            index--;
            System.out.println(point[0] + " " + point[1]);
            // Then creates a point to add
            Point toAdd = new Point(
                    isFloat(point[0]) ? parseFloat(point[0]) : previousPoint.x,
                    isFloat(point[1]) ? parseFloat(point[1]) : previousPoint.y
            );
            if (isCommandRelative) toAdd.add(previousPoint);
            System.out.println(toAdd.x + " " + toAdd.y);
            toAdd.command = command;
            previousPoint = toAdd;
            currentPoints.add(toAdd);
            command = Point.COMMANDS.none;
        }
        return svgToPointList(attribute, currentPoints, index + 1);
    }

    /**
     * Parses a floating point string, with decimals and negative values, to a <code>float</code>.
     * Uses DecimalFormat.
     *
     * @param input The String to parse (ex. <code>"-100.54313"</code>
     * @return The String, converted to a number (<code>Float</code>)
     */
    private float parseFloat(String input) {
        DecimalFormat df = new DecimalFormat();
        try {
            return df.parse(input).floatValue();
        } catch (Exception e) {
            Log.e("Warning:", "There was an issue parsing a float. It's been replaced with 0, but here's the error:");
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Checks if a number (as a String) is able to be parsed from <code>parseFloat</code>, or in other words, if it is a float
     *
     * @param input The number, as a String, to check.
     * @return Whether or not parseFloat can parse the number
     */
    private boolean isFloat(String input) {
        return !input.matches("[^\\d.]") && !input.equals("");
    }

}
