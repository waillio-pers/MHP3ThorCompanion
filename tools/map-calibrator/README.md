# Map calibrator

This small desktop helper lets maintainers adjust gathering-node positions against the map images shipped by the Android app.

## Run

Install Python 3 and Pillow, then double-click `START_MAP_CALIBRATOR.bat` or run:

```powershell
python map_calibrator.py
```

Use the mouse to select and drag nodes, the wheel to zoom, and the right or middle mouse button to pan. The editor can add or remove nodes and edit their IDs, types, zones, names, and coordinates. `Ctrl+S` saves a local workspace; the JSON export can be reviewed before integrating changes into the app.

The workspace file is local generated state and is ignored by Git. The tool can reload the current nodes from the Android project.
