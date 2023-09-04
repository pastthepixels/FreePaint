<!-- Using HTML on markdown is just cursed... -->
<h1 align="center"> <img height="32px" src="fastlane/metadata/android/en-US/images/icon.png"/> FreePaint </h1>

<p align="center">
  <a href="https://f-droid.org/en/packages/io.github.pastthepixels.freepaint"><img src="https://img.shields.io/f-droid/v/io.github.pastthepixels.freepaint" /></a>
  <a href="https://github.com/pastthepixels/FreePaint/releases/latest"><img src="https://img.shields.io/github/downloads/pastthepixels/FreePaint/latest/total" /></a>
  <a href="https://github.com/pastthepixels/FreePaint/issues"><img src="https://img.shields.io/github/issues/pastthepixels/FreePaint" /></a>
  <a href="https://github.com/pastthepixels/FreePaint/graphs/contributors"><img src="https://img.shields.io/github/contributors/pastthepixels/FreePaint" /></a>
</p>

A vector graphics drawing app for Android.

<a href="https://f-droid.org/en/packages/io.github.pastthepixels.freepaint/">Get it on F-Droid!</a>

# Screenshots
<img width="200px" src="https://github.com/pastthepixels/FreePaint/assets/52388215/665d393b-f937-4263-bac5-8cf5b0239257" />
<img width="200px" src="https://github.com/pastthepixels/FreePaint/assets/52388215/58481eb2-5b47-4b1c-ab8c-16e4331d4df5" />


# FEATURING
- Material Design 3 with dynamic colors
- Custom document sizes, fill/stroke colors, and stroke sizes
- Eraser tool erases shapes from closed paths, or points from open paths
- Saving/loading SVG files

# Using FreePaint
## Paint tool
Using this tool, you can either draw open or closed paths. Simply go to the overflow menu > Settings to access this and more options.
## Eraser tool
When you select this tool, you can draw a shape to define the area you wish to erase. One of two things can happen depending on which path you want to erase from:
For closed paths, the entire path will become green, and you can draw a shape to erase from the path.
For open paths, individual points will become highlighted, and if those points are included in your eraser path, they will be deleted. Erasing a point inside a path will create 2 new paths.
## Pan/zoom tool
With one finger, you can pan across the document. If you pinch the screen with two, it'll zoom in and out from the center.
## Selection tool
You can draw a rectangle for an area you wish to select (marquee select). If any paths overlap that area, they will become "selected" and the rectangle will resize to include the selected paths.
You can drag on that rectangle to move those paths, or draw on an empty space to create a new selection.

# Notes

- Uses https://github.com/Gericop/Android-Support-Preference-V7-Fix/tree/androidx to counter a bug
  with preferences

# Potential future updates?
- Automatically smoothing lines with Bezier curves
- Layers!
- Pen tool (like Illustrator/Inkscape!)
- **Undo/Redo operations**
