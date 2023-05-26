package io.github.pastthepixels.freepaint.File;

import static java.util.Arrays.asList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.stream.Collectors;

import io.github.pastthepixels.freepaint.DrawAppearance;
import io.github.pastthepixels.freepaint.DrawCanvas;
import io.github.pastthepixels.freepaint.DrawPath;
import io.github.pastthepixels.freepaint.Point;

import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class SVG {
    private String data = "";
    private DrawCanvas canvas;

    private DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    /*
     * Creates a new SVG instance, but we need a canvas with paths.
     * @param canvas A DrawCanvas instance
     */
    public SVG(DrawCanvas canvas) {
        this.canvas = canvas;
    }

    @SuppressLint("DefaultLocale")
    public void createSVG() {
        this.data = "";
        this.data += String.format("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
        this.data += String.format(
                // https://www.w3.org/TR/SVGTiny12/painting.html#viewport-fill-property
                // should work, but not supported by most browsers so we use a style attribute as well.
                // Note that we only read viewport-fill
                // TODO: Instead of this, create/read a rect for maximum compatibility
                "<svg width=\"%d\" height=\"%d\" viewport-fill=\"%s\" style=\"background-color: %<s; stroke-width: 0px;\" xmlns=\"http://www.w3.org/2000/svg\">",
                (int)canvas.documentSize.x,
                (int)canvas.documentSize.y,
                DrawAppearance.colorToHex(canvas.documentColor)
        );
        for(DrawPath path : canvas.paths) {
            if (path.points.size() > 1) addPath(path);
        }
        // Closes the tag. We are done.
        this.data += "\n</svg>";
        System.out.println(this.data);
    }

    public void writeFile(OutputStream stream) throws IOException {
        stream.write(data.getBytes());
        stream.close();
    }

    public void loadFile(InputStream stream) {
        parseFile(
                (new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)))
                        .lines().collect(Collectors.joining("\n"))
        );
    }

    @SuppressLint("DefaultLocale")
    public void addPath(DrawPath path) {
        StringBuilder data = new StringBuilder("\n<path d=\"");
        // Step 1. Add points.
        for(Point point : path.points) {
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
        String fillValue = path.appearance.fill == -1? "none" : DrawAppearance.colorToHex(path.appearance.fill);
        String strokeValue = path.appearance.stroke == -1? "none" : DrawAppearance.colorToHex(path.appearance.stroke);
        data.append(String.format("fill=\"%s\" ", fillValue));
        data.append(String.format("stroke=\"%s\" ", strokeValue));
        if (path.appearance.fill != -1) data.append(String.format("fill-opacity=\"%f\" ", DrawAppearance.getColorAlpha(path.appearance.fill)));
        if (path.appearance.stroke != -1) data.append(String.format("stroke-opacity=\"%f\" ", DrawAppearance.getColorAlpha(path.appearance.stroke)));
        data.append(String.format("stroke-width=\"%s\" ", path.appearance.strokeSize));
        data.append("stroke-linecap=\"round\" ");
        // Done
        this.data += data + "/>";
    }

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
            if(document.getDocumentElement().hasAttribute("viewport-fill")) {
                canvas.documentColor = Color.parseColor(document.getDocumentElement().getAttribute("viewport-fill"));
            }
            // Add paths
            canvas.paths.clear();
            NodeList nodes = document.getElementsByTagName("path");
            for(int i = 0; i < nodes.getLength(); i ++) {
                Node node = nodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    DrawPath path = new DrawPath();
                    // Points
                    path.points = svgToPointList(element.getAttribute("d"), path.points, 0);
                    // Fill/stroke
                    // TODO: account for opacity w/ fill-opacity and stroke-opacity
                    if(element.hasAttribute("fill") && !element.getAttribute("fill").equals("none")) {
                        path.appearance.fill = Color.parseColor(element.getAttribute("fill"));
                    }
                    if(element.hasAttribute("stroke") && !element.getAttribute("stroke").equals("none")) {
                        path.appearance.stroke = Color.parseColor(element.getAttribute("stroke"));
                    }
                    // Stroke width
                    if(element.hasAttribute("stroke-width")) {
                        path.appearance.strokeSize = Integer.parseInt(element.getAttribute("stroke-width"));
                    }
                    // Done!!
                    path.finalise();
                    canvas.paths.add(path);
                }
            }
            // Invalidate!
            canvas.invalidate();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    public LinkedList<Point> svgToPointList(String attribute, LinkedList<Point> currentPoints, int index) {
        if (index + 1 > attribute.length()) {
            return currentPoints;
        }
        // Pull the selected character to a variable
        String character = attribute.substring(index, index + 1);
        // Two commands we support: M (move) and L (line).
        if(character.equals("M") || character.equals("L")) {
            index ++;
            // Iterates through the rest of the string until we have two numbers
            String[] point = {"", ""};
            int pointIndex = 0;
            while(!(!point[1].equals("") && character.equals(" "))) {
                character = attribute.substring(index, index + 1);
                if(character.equals(" ")) {
                    pointIndex ++;
                    index ++;
                    continue;
                }
                point[pointIndex] += character;
                index ++;
            }
            index --;
            System.out.println(point[0] + " " + point[1]);
            // Then creates a point to add
            Point toAdd = new Point(parseFloat(point[0]), parseFloat(point[1]));
            toAdd.command = character.equals("M")? Point.COMMANDS.move : Point.COMMANDS.line;
            currentPoints.add(toAdd);
        }
        return svgToPointList(attribute, currentPoints, index + 1);
    }

    private float parseFloat(String input) {
        return Float.parseFloat(input.replaceAll("[^\\d.]", ""));
    }

}
