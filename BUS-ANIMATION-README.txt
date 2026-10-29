TRACKIT BUS ANIMATION UPDATE (WINDOWS)

This package has the updated static/index.html with animated SVG bus markers and the Maven annotation processor fix that was needed for Java 25.

HOW TO APPLY:
1. Close TrackIt in the command window with Ctrl+C if it is running.
2. Extract TrackIt-Bus-Animation-Fix.zip into your Downloads folder.
3. When Windows asks whether to merge/replace files in the existing TrackIt-Windows-Fix folder, choose to merge and replace files. This updates the files in:
   Downloads\TrackIt-Windows-Fix\trackit
4. Double-click RUN-TRACKIT-WINDOWS.cmd inside that trackit folder.
5. Wait for Started TrackItApplication.
6. Open http://localhost:8080 and press Ctrl+F5.

If the .cmd file says it cannot find Maven, this package assumes Maven is extracted to:
Downloads\apache-maven-3.10.0-bin\apache-maven-3.10.0

Animated bus markers are inline SVG, so no separate image file is needed. The existing API, route filter, ETA, nearby buses, trip replay, and alerts are retained in the HTML.
