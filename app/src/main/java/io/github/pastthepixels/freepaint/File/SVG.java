package io.github.pastthepixels.freepaint.File;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.util.Log;

import androidx.annotation.NonNull;

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
import java.util.Objects;
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
                    path.appearance.stroke = path.appearance.fill = -1;
                    path.isClosed = element.getAttribute("d").toUpperCase().contains("Z");
                    // Points
                    path.points = parsePath(element.getAttribute("d"));
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
     * Parses a path's <code>d</code> parameter (as a String) into a linked list of points.
     * <b>If you want to add support for an SVG command</b>, add a value for Point.COMMANDS,
     * then update SVG.svgToPointCommand to convert an SVG command to a Point command (case
     * insensitive). Then, make your implementation by adding a case in the switch statement
     * in this function.
     * <p>
     * Note the boolean isCommandRelative and how other statements set the x and y of penCoords.
     * SVG spec means that each point with an absolute command sets the position of the "pen"
     * and each point with a relative command moves the pen by some amount.
     *
     * @param d The "d" attribute of an SVG path element.
     * @return A linked list of points, with proper commands, that you can use in a DrawPath.
     */
    public LinkedList<Point> parsePath(@NonNull String d) {
        // We start with a "pen" that we set the position of (capital letters) or move around by an amount (lowercase letters)
        Point penCoords = new Point(0, 0);
        LinkedList<Point> points = new LinkedList<>();

        // 1. Separate the string into 1 string per command (ex. "M 12 240 10 4" (multiple points with same command), "L 4")
        LinkedList<String> commands = new LinkedList<>();
        for(int i = 0; i < d.length(); i ++) {
            if (Character.isLetter(d.charAt(i))) {
                commands.add(String.valueOf(d.charAt(i)));
            } else {
                commands.set(
                        commands.size() - 1,
                        commands.get(commands.size() - 1).concat(String.valueOf(d.charAt(i)))
                );
            }
        }

        // 2. Separate each string into points with the command of the letter at the start of the string.
        for(int i = 0; i < commands.size(); i++) {
            System.out.println(commands.get(i));
            // Take the first letter out; that's a command, not a number.
            Point.COMMANDS command = svgToPointCommand(commands.get(i).charAt(0));
            boolean isCommandRelative = Character.isLowerCase(commands.get(i).charAt(0));
            // Note: commas are ignored (as per W3 spec) and replaced with spaces
            String[] numbers = commands.get(i).replace(",", " ").substring(1).strip().split(" ");
            switch(command) {
                // Horizontal lines ("H" command)
                case horizontal:
                    penCoords.x = isCommandRelative? penCoords.x + parseFloat(numbers[0]) : parseFloat(numbers[0]);
                    points.add(new Point(penCoords.x, penCoords.y, Point.COMMANDS.line));

                // Vertical lines ("V" command)
                case vertical:
                    penCoords.y = isCommandRelative? penCoords.y + parseFloat(numbers[0]) : parseFloat(numbers[0]);
                    points.add(new Point(penCoords.x, penCoords.y, Point.COMMANDS.line));

                // Moveto command ("M")
                case move:
                    for(int j = 0; j < numbers.length; j ++) {
                        // Even index: likely x coordinate
                        if(j % 2 == 0) {
                            penCoords.x = isCommandRelative? penCoords.x + parseFloat(numbers[j]) : parseFloat(numbers[j]);
                        } else {
                            penCoords.y = isCommandRelative? penCoords.y + parseFloat(numbers[j]) : parseFloat(numbers[j]);
                        }
                    }
                    points.add(new Point(penCoords.x, penCoords.y, Point.COMMANDS.move));

                // Lineto command ("L")
                case line:
                    for(int j = 0; j < numbers.length; j ++) {
                        // Even index: likely x coordinate
                        if(j % 2 == 0) {
                            penCoords.x = isCommandRelative? penCoords.x + parseFloat(numbers[j]) : parseFloat(numbers[j]);
                        } else {
                            // Odd index: y component of a coordinate, completes
                            // a coordinate which we add as a Point.
                            penCoords.y = isCommandRelative? penCoords.y + parseFloat(numbers[j]) : parseFloat(numbers[j]);
                            points.add(new Point(penCoords.x, penCoords.y, Point.COMMANDS.line));
                        }
                    }
            }
        }

        return points;
    }

    /**
     * Converts an SVG command, case insensitive (ex. moveto "M", lineto "L") to a corresponding Point.COMMANDS command.
     *
     * @param svgCommand Char containing SVG path letter command
     * @return an equivalent Path.COMMANDS commnd
     */
    public Point.COMMANDS svgToPointCommand(char svgCommand) {
        // Only moveto and line commands are supported--SVGs with a "c/s" command won't load
        // as FreePaint doesnt' support Bezier curves right now
        switch(Character.toLowerCase(svgCommand)) {
            case 'm':
                return Point.COMMANDS.move;
            case 'l':
                return Point.COMMANDS.line;
            case 'h':
                return Point.COMMANDS.horizontal;
            case 'v':
                return Point.COMMANDS.vertical;
            default:
                return Point.COMMANDS.none;
        }
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
            return Objects.requireNonNull(df.parse(input.trim())).floatValue();
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
