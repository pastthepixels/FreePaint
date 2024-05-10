package io.github.pastthepixels.freepaint.File;

import android.annotation.SuppressLint;
import android.graphics.Color;

import androidx.annotation.NonNull;

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
import java.util.ArrayList;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import dev.romainguy.graphics.path.Svg;
import io.github.pastthepixels.freepaint.Graphics.DrawAppearance;
import io.github.pastthepixels.freepaint.Graphics.DrawCanvas;
import io.github.pastthepixels.freepaint.Graphics.DrawPath;
import io.github.pastthepixels.freepaint.Graphics.Point;

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
        data.append(Svg.toSvg(path.generatePath(), false));
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
                path.cachePath();
                canvas.paths.add(path);
            }
        }
        // Invalidate!
        canvas.invalidate();
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
    public ArrayList<Point> parsePath(@NonNull String d) {
        // We start with a "pen" that we set the position of (capital letters) or move around by an amount (lowercase letters)
        Point penCoords = new Point(0, 0);
        ArrayList<Point> points = new ArrayList<>();

        // 1. Separate the string into 1 string per command (ex. "M 12 240 10 4" (multiple points with same command), "L 4")
        ArrayList<String> commands = new ArrayList<>();
        for (int i = 0; i < d.length(); i++) {
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
        for (int i = 0; i < commands.size(); i++) {
            System.out.println(commands.get(i));
            // Take the first letter out; that's a command, not a number.
            Point.COMMANDS command = svgToPointCommand(commands.get(i).charAt(0));
            boolean isCommandRelative = Character.isLowerCase(commands.get(i).charAt(0));
            // Note: commas are ignored (as per W3 spec) and replaced with spaces
            String[] numbers = commands.get(i).replace(",", " ").replace("-", " -").replace("  ", " ").substring(1).strip().split(" ");
            //TODO remove
            for (String num : numbers) {
                System.out.print(num + " ");
            }
            System.out.println();

            Point originalPenCoords; // Used for bezier curves

            switch (command) {
                // Horizontal lines ("H" command)
                case horizontal:
                    penCoords.x = Float.parseFloat(numbers[0]) + (isCommandRelative ? penCoords.x : 0);
                    points.add(new Point(penCoords.x, penCoords.y, Point.COMMANDS.line));
                    break;

                // Vertical lines ("V" command)
                case vertical:
                    penCoords.y = Float.parseFloat(numbers[0]) + (isCommandRelative ? penCoords.y : 0);
                    points.add(new Point(penCoords.x, penCoords.y, Point.COMMANDS.line));
                    break;

                // Moveto command ("M")
                case move:
                    for (int j = 0; j < numbers.length; j++) {
                        // Even index: likely x coordinate
                        if (j % 2 == 0) {
                            penCoords.x = Float.parseFloat(numbers[j]) + (isCommandRelative ? penCoords.x : 0);
                        } else {
                            penCoords.y = Float.parseFloat(numbers[j]) + (isCommandRelative ? penCoords.y : 0);
                        }
                    }
                    points.add(new Point(penCoords.x, penCoords.y, Point.COMMANDS.move));
                    break;

                // Lineto command ("L")
                case line:
                    for (int j = 0; j < numbers.length; j++) {
                        // Even index: likely x coordinate
                        if (j % 2 == 0) {
                            penCoords.x = Float.parseFloat(numbers[j]) + (isCommandRelative ? penCoords.x : 0);
                        } else {
                            // Odd index: y component of a coordinate, completes
                            // a coordinate which we add as a Point.
                            penCoords.y = Float.parseFloat(numbers[j]) + (isCommandRelative ? penCoords.y : 0);
                            points.add(new Point(penCoords.x, penCoords.y, Point.COMMANDS.line));
                        }
                    }
                    break;

                // Cubic bezier curve command ("C")
                case cubicBezier:
                    originalPenCoords = points.get(points.size() - 1).clone();
                    for (int j = 0; j < numbers.length - 5; j += 6) {
                        // Sets the right handle of the last point.
                        penCoords.x = Float.parseFloat(numbers[j]) + (isCommandRelative ? originalPenCoords.x : 0);
                        penCoords.y = Float.parseFloat(numbers[j + 1]) + (isCommandRelative ? originalPenCoords.y : 0);
                        points.get(points.size() - 1).setRightHandle(new Point(
                                penCoords.x - points.get(points.size() - 1).x,
                                penCoords.y - points.get(points.size() - 1).y
                        ));
                        // Defines the handle for a new point.
                        penCoords.x = Float.parseFloat(numbers[j + 2]) + (isCommandRelative ? originalPenCoords.x : 0);
                        penCoords.y = Float.parseFloat(numbers[j + 3]) + (isCommandRelative ? originalPenCoords.y : 0);
                        Point leftHandle = new Point(
                                penCoords.x,
                                penCoords.y
                        );
                        // Makes a new point.
                        penCoords.x = Float.parseFloat(numbers[j + 4]) + (isCommandRelative ? originalPenCoords.x : 0);
                        penCoords.y = Float.parseFloat(numbers[j + 5]) + (isCommandRelative ? originalPenCoords.y : 0);
                        Point newPoint = new Point(penCoords.x, penCoords.y, Point.COMMANDS.line);
                        leftHandle.applySubtract(newPoint);
                        newPoint.setLeftHandle(leftHandle);
                        points.add(newPoint);
                        // update originalPenCoords
                        originalPenCoords = newPoint.clone();
                    }
                    break;

                case smoothCubicBezier:
                    originalPenCoords = points.get(points.size() - 1).clone();
                    for (int j = 0; j < numbers.length - 3; j += 4) {
                        // Reflect the left handle of the last control point.
                        Point leftHandle = points.get(points.size() - 1).getLeftHandle();
                        leftHandle.applySubtract(points.get(points.size() - 1));
                        points.get(points.size() - 1).setRightHandle(new Point(
                                -leftHandle.x,
                                -leftHandle.y
                        ));
                        // Defines the handle for a new point.
                        penCoords.x = Float.parseFloat(numbers[j]) + (isCommandRelative ? originalPenCoords.x : 0);
                        penCoords.y = Float.parseFloat(numbers[j + 1]) + (isCommandRelative ? originalPenCoords.y : 0);
                        leftHandle = new Point(
                                penCoords.x,
                                penCoords.y
                        );
                        // Makes a new point.
                        penCoords.x = Float.parseFloat(numbers[j + 2]) + (isCommandRelative ? originalPenCoords.x : 0);
                        penCoords.y = Float.parseFloat(numbers[j + 3]) + (isCommandRelative ? originalPenCoords.y : 0);
                        Point newPoint = new Point(penCoords.x, penCoords.y, Point.COMMANDS.line);
                        leftHandle.applySubtract(newPoint);
                        newPoint.setLeftHandle(leftHandle);
                        points.add(newPoint);
                        // update originalPenCoords
                        originalPenCoords = newPoint.clone();
                    }
                    break;
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
        switch (Character.toLowerCase(svgCommand)) {
            case 'm':
                return Point.COMMANDS.move;
            case 'l':
                return Point.COMMANDS.line;
            case 'h':
                return Point.COMMANDS.horizontal;
            case 'v':
                return Point.COMMANDS.vertical;
            case 'c':
                return Point.COMMANDS.cubicBezier;
            case 's':
                return Point.COMMANDS.smoothCubicBezier;
            default:
                return Point.COMMANDS.none;
        }
    }

}
