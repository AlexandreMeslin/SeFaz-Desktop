package br.com.meslin.main;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import br.com.meslin.auxiliar.StaticLibrary;
import br.com.meslin.mapa.GeographicMap;
import br.com.meslin.model.MobileNode;
import br.com.meslin.model.Region;
import br.com.meslin.model.SamplePredicate;
import ckafka.data.Swap;
import main.java.ckafka.GroupDefiner;
import main.java.ckafka.GroupSelection;

public class MainGD implements GroupSelection {
    /** Logger */
    final Logger logger = LoggerFactory.getLogger(GroupDefiner.class);
	/** Array of regions */
	private ArrayList<Region> regionList;
	/** JMapViewer-based map */
	private GeographicMap map;
	/** A list of inspector from SeFaz */
	private List<MobileNode> inspectorList;

	/**
     * Constructor
     */
    public MainGD() {
    	/*
    	 * Getting regions
    	 */
		String workDir = System.getProperty("user.dir");
		System.out.println("Working Directory = " + workDir);
		String fullFilename = workDir + "/Bairros/RioDeJaneiro.txt";
		List<String> lines = StaticLibrary.readFilenamesFile(fullFilename);
		// reads each region file
		this.regionList = new ArrayList<Region>();	// region list
		for(String line : lines) {
			int regionNumber = Integer.parseInt(line.substring(0, line.indexOf(",")).trim());
			String filename = line.substring(line.indexOf(",")+1).trim();
			Region region = StaticLibrary.readRegion(filename, regionNumber);
			this.regionList.add(region);
		}

		/*
		 * Building the map
		 */
		// HTTP agent to request map tiles
		String httpAgent = System.getProperty("http.agent");
		if (httpAgent == null) {
		    httpAgent = "(" + System.getProperty("os.name") + " / " + System.getProperty("os.version") + " / " + System.getProperty("os.arch") + ")";
		}
		System.setProperty("http.agent", "GroupDefiner/1.0 " + httpAgent);
		// create the map
		this.map = new GeographicMap(this.regionList);
		this.map.setVisible(true);
		
		// create an empty inspector list
		this.inspectorList = new ArrayList<MobileNode>();

		/*
		 * Create GroupDefiner 
		 */
        ObjectMapper objectMapper = new ObjectMapper();
        Swap swap = new Swap(objectMapper);
        new GroupDefiner(this, swap);
    }

	/**
	 * Main
	 * @param args command line arguments
	 */
    public static void main(String[] args) {
    	// creating missing environment variable
		Map<String,String> env = new HashMap<String, String>();
		env.putAll(System.getenv());
		if(System.getenv("gd.one.consumer.topics") == null) 			env.put("gd.one.consumer.topics", "GroupReportTopic");
		if(System.getenv("gd.one.consumer.auto.offset.reset") == null) 	env.put("gd.one.consumer.auto.offset.reset", "latest");
		if(System.getenv("gd.one.consumer.bootstrap.servers") == null) 	env.put("gd.one.consumer.bootstrap.servers", "127.0.0.1:9092");
		if(System.getenv("gd.one.consumer.group.id") == null) 			env.put("gd.one.consumer.group.id", "gw-gd");
		if(System.getenv("gd.one.producer.bootstrap.servers") == null) 	env.put("gd.one.producer.bootstrap.servers", "127.0.0.1:9092");
		if(System.getenv("gd.one.producer.retries") == null) 			env.put("gd.one.producer.retries", "3");
		if(System.getenv("gd.one.producer.enable.idempotence") == null)	env.put("gd.one.producer.enable.idempotence", "true");
		if(System.getenv("gd.one.producer.linger.ms") == null) 			env.put("gd.one.producer.linger.ms", "1");
		try {
			StaticLibrary.setEnv(env);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// creating new GroupDefiner
        new MainGD();
    }

    /**
     * groupsIdentification<br>
     * @return a set containing all groups (a group is an Integer)
     */
    public Set<Integer> groupsIdentification() {
        Set<Integer> setOfGroups = new HashSet<Integer>();
        setOfGroups.add(1000);	// Mobile Node default group
        for (Region region : regionList) {
			setOfGroups.add(region.getNumber());
		}
        return setOfGroups;
    }

    /**
     * getNodesGroupByContext<br>
     * @return a set of groups representing the node groups
     */
    public Set<Integer> getNodesGroupByContext(ObjectNode contextInfo) {
    	MobileNode inspector = null;
        Set<Integer> setOfGroups = new HashSet<Integer>();
        double latitude = Double.parseDouble(String.valueOf(contextInfo.get("latitude")));
        double longitude = Double.parseDouble(String.valueOf(contextInfo.get("longitude")));

        // update inspector position on the map
        try {
			inspector = new MobileNode(String.valueOf(contextInfo.get("date")), latitude, longitude,
					String.valueOf(contextInfo.get("ID")));
		} catch (NumberFormatException | ParseException e) {
			e.printStackTrace();
		}
        inspectorList.removeIf(new SamplePredicate(inspector.getUuid()));
        inspectorList.add(inspector);
        map.remove(inspector);
        map.addInspector(inspector);
        
        setOfGroups.add(1000);	// Mobile Node default group
        Coordinate coordinate = new Coordinate(latitude, longitude);
        for (Region region : regionList) {
			if(region.contains(coordinate)) {
				setOfGroups.add(region.getNumber());
			}
		}
        logger.info(String.format("[MainGD] lista de grupos para %s = %s.", String.valueOf(contextInfo.get("ID")), setOfGroups));
        return setOfGroups;
    }

    public String kafkaConsumerPrefix() {
        return "gd.one.consumer";
    }

    public String kafkaProducerPrefix() {
        return "gd.one.producer";
    }
}
