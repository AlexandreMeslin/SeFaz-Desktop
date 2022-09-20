package br.com.meslin.mapa;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.JMapViewerTree;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.MapPolygonImpl;
import org.openstreetmap.gui.jmapviewer.OsmTileLoader;
import org.openstreetmap.gui.jmapviewer.events.JMVCommandEvent;
import org.openstreetmap.gui.jmapviewer.interfaces.JMapViewerEventListener;
import org.openstreetmap.gui.jmapviewer.interfaces.MapPolygon;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.BingAerialTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.OsmTileSource;

import br.com.meslin.model.ColorSet;
import br.com.meslin.model.MobileNode;
import br.com.meslin.model.Region;

/**
 * Plots a map with regions and buses<br>
 * JavaDoc at https://josm.openstreetmap.de/doc<br>
 * @author meslin
 *
 */
@SuppressWarnings("serial")
public class GeographicMap extends JFrame implements JMapViewerEventListener
{
	private final JMapViewerTree treeMap;
	private JLabel metersPerPixelLabel;
	private JLabel metersPerPixelValue;
	private JLabel zoomLabel;
	private JLabel zoomValue;
	
	private List<MapMarkerDot> mapMarkerDotList;

	/**
	 * Constructs {@code Demo}.
	 * @param regionList 
	 */
	public GeographicMap(List<Region> regionList) {
		super("Map with Regions");
		mapMarkerDotList = new ArrayList<MapMarkerDot>();	// at first, there is no bus on map
		
        setSize(400, 400);
		
		treeMap = new JMapViewerTree("Regions");
		
		map().addJMVListener(this);
		
		setLayout(new BorderLayout());
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setExtendedState(JFrame.MAXIMIZED_BOTH);
		JPanel panel = new JPanel(new BorderLayout());
		JPanel panelTop = new JPanel();
		JPanel panelBottom = new JPanel();
		JPanel helpPanel = new JPanel(new BorderLayout());
		
		metersPerPixelLabel = new JLabel("Meters/Pixels:");
		metersPerPixelValue = new JLabel(String.format("%s", map().getMeterPerPixel()));
		
        zoomLabel = new JLabel("Zoom: ");
        zoomValue = new JLabel(String.format("%s", map().getZoom()));
		
        add(panel, BorderLayout.NORTH);
        add(helpPanel, BorderLayout.SOUTH);
        panel.add(panelTop, BorderLayout.NORTH);
        panel.add(panelBottom, BorderLayout.SOUTH);

        JLabel helpLabel = new JLabel("Use the mouse right button to move, double click or mouse wheel to zoom. © OpenStreetMap contributors");
        helpPanel.add(helpLabel, BorderLayout.NORTH);

        JButton marksBuyton = new JButton("Marks");
        marksBuyton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                map().setDisplayToFitMapMarkers();
            }
        });
        JButton regionButton = new JButton("Regions");
        regionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                map().setDisplayToFitMapPolygons();
            }
        });
        JComboBox<TileSource> tileSourceSelector = new JComboBox<>(new TileSource[] {
                new OsmTileSource.Mapnik(),
                new OsmTileSource.CycleMap(),
                new BingAerialTileSource()
        });
        tileSourceSelector.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                map().setTileSource((TileSource) e.getItem());
            }
        });

        JComboBox<TileLoader> tileLoaderSelector;
        tileLoaderSelector = new JComboBox<>(new TileLoader[] {
        		new OsmTileLoader(map())
        });
        tileLoaderSelector.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                map().setTileLoader((TileLoader) e.getItem());
            }
        });
        map().setTileLoader((TileLoader) tileLoaderSelector.getSelectedItem());
        panelTop.add(tileSourceSelector);
        panelTop.add(tileLoaderSelector);

        final JCheckBox showTileGrid = new JCheckBox("Grid visible");
        showTileGrid.setSelected(map().isTileGridVisible());
        showTileGrid.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                map().setTileGridVisible(showTileGrid.isSelected());
            }
        });
        panelBottom.add(showTileGrid);
        final JCheckBox showZoomControls = new JCheckBox("Zoom control visible");
        showZoomControls.setSelected(map().getZoomControlsVisible());
        showZoomControls.addActionListener(new ActionListener() {
            @SuppressWarnings("deprecation")
			@Override
            public void actionPerformed(ActionEvent e) {
                map().setZoomContolsVisible(showZoomControls.isSelected());
            }
        });
        panelBottom.add(showZoomControls);
        final JCheckBox scrollWrapEnabled = new JCheckBox("Scrollwrap enabled");
        scrollWrapEnabled.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                map().setScrollWrapEnabled(scrollWrapEnabled.isSelected());
            }
        });
        panelBottom.add(scrollWrapEnabled);
        panelBottom.add(marksBuyton);
        panelBottom.add(regionButton);

        panelTop.add(zoomLabel);
        panelTop.add(zoomValue);
        panelTop.add(metersPerPixelLabel);
        panelTop.add(metersPerPixelValue);

        add(treeMap, BorderLayout.CENTER);
        
        // adds regions
        int colorNumber = 0;
        for(Region region : regionList) {
	        MapPolygon polygon = new MapPolygonImpl(region.getPoints());
	        ((MapPolygonImpl) polygon).setBackColor(ColorSet.getColor(colorNumber++));
	        map().addMapPolygon(polygon);
	        map().addMapMarker(new MapMarkerDot(String.valueOf(region.getNumber()), region.getRegionCenter()));
        }

        /*
         * listener: click
         */
        map().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    map().getAttribution().handleAttribution(e.getPoint(), true);
                }
            }
        });

        /*
         * listerner: motion
         */
        map().addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point p = e.getPoint();
                boolean cursorHand = map().getAttribution().handleAttributionCursor(p);
                if (cursorHand) {
                    map().setCursor(new Cursor(Cursor.HAND_CURSOR));
                } else {
                    map().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
            }
        });        
	}

    @Override
	public void processCommand(JMVCommandEvent command)
	{
        if (command.getCommand().equals(JMVCommandEvent.COMMAND.ZOOM) ||
                command.getCommand().equals(JMVCommandEvent.COMMAND.MOVE)) {
            updateZoomParameters();
        }
	}

    private JMapViewer map() {
        return treeMap.getViewer();
    }
    
    private void updateZoomParameters() {
        if (metersPerPixelValue != null)
            metersPerPixelValue.setText(String.format("%s", map().getMeterPerPixel()));
        if (zoomValue != null)
            zoomValue.setText(String.format("%s", map().getZoom()));
    }
    
    /**
     * Adds a bus to the map using geographic coordinates and its label
     * @param label
     * @param coordinate
     */
    public void addBus(String label, Coordinate coordinate)
    {
    	MapMarkerDot avatar = new MapMarkerDot(coordinate);
    	avatar.setName(label);
    	map().addMapMarker(avatar);
    	this.mapMarkerDotList.add(avatar);
    }
    /**
     * Adds a bus to the map<br>
     * @param bus
     */
    /*
	public void addBus(Bus bus) {
		addBus(bus.getLinha() + "@" + bus.getOrdem(), new Coordinate(bus.getLatitude(), bus.getLongitude()));
	}
	*/
	/**
	 * Adds a passenger to the map
	 * @param passenger
	 */
    /*
	public void addPassenger(Passenger passenger) {
		MapMarkerDot avatar = new MapMarkerDot(new Coordinate(passenger.getLatitude(), passenger.getLongitude()));
		avatar.setName(passenger.getName());
		avatar.setBackColor(new Color(0, 0, 255));
		map().addMapMarker(avatar);
	}
	*/
	/**
	 * Removes a bus form the map<br>
	 * @param bus
	 */
    /*
	public void remove(Bus bus) {
		for(Iterator<MapMarkerDot> iterator = mapMarkerDotList.iterator(); iterator.hasNext();) {
			MapMarkerDot mapMarkerDot = iterator.next();
			if(mapMarkerDot.getName().equals(bus.getLinha() + "@" + bus.getOrdem())) {
				map().removeMapMarker(mapMarkerDot);
				iterator.remove();
			}
		}
/ *		for(int i=0; i<mapMarkerDotList.size(); i++) {
			if(mapMarkerDotList.get(i).getName().equals(bus.getLinha() + "@" + bus.getOrdem())) {
				map().removeMapMarker(mapMarkerDotList.get(i));
				mapMarkerDotList.remove(i);
			}
		}
* /	}
	*/
	/* (non-Javadoc)
	 * @see java.awt.Container#removeAll()
	 */
	public void removeAll() {
		for(Iterator<MapMarkerDot> iterator = mapMarkerDotList.iterator(); iterator.hasNext();) {
			map().removeMapMarker(iterator.next());
			iterator.remove();
		}
/*		for(MapMarkerDot mapMarkerDot: mapMarkerDotList) {
			map().removeMapMarker(mapMarkerDot);
			mapMarkerDotList.remove(mapMarkerDot);
		}
*/	}

	/**
	 * remove<br>
	 * Remove a inspector to map
	 * @param inspector
	 */
	public void remove(MobileNode inspector) {
		for(Iterator<MapMarkerDot> iterator = mapMarkerDotList.iterator(); iterator.hasNext();) {
			MapMarkerDot mapMarkerDot = iterator.next();
			if(mapMarkerDot.getName().equals(inspector.getUuid().toString())) {
				map().removeMapMarker(mapMarkerDot);
				iterator.remove();
			}
		}
	}

	/**
	 * addInspector<br>
	 * Adds a inspector to the map
	 * @param label
	 * @param coordinate
	 */
	public void addInspector(String label, Coordinate coordinate) {
    	MapMarkerDot avatar = new MapMarkerDot(coordinate);
    	avatar.setName(label);
    	Color color = new Color(255, 0, 0);
    	avatar.setColor(color);
    	avatar.setBackColor(color);
    	map().addMapMarker(avatar);
    	this.mapMarkerDotList.add(avatar);
	}
	
	/**
	 * addInspector<br>
	 * Adds a inspector to the map
	 * @param inspector
	 */
	public void addInspector(MobileNode inspector) {
		addInspector(inspector.getUuid().toString(), new Coordinate(inspector.getLatitude(), inspector.getLongitude()));
	}
}
