package org.baderlab.csplugins.enrichmentmap.model;

import java.text.Collator;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractDataSet implements Comparable<AbstractDataSet> {

	private final String name;
	private final Map<String, Long> nodeSuids = new HashMap<>();
	/** EnrichmentMap only creates nodes for these genes. */
	private SetOfGeneSets geneSetsOfInterest = new SetOfGeneSets();
	
	private static final Collator collator = Collator.getInstance();
	private final Object lock = new Object();
	
	protected AbstractDataSet(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public Map<String, Long> getNodeSuids() {
		synchronized (lock) {
			return new HashMap<>(nodeSuids);
		}
	}
	
	public void setNodeSuids(Map<String, Long> newValue) {
		synchronized (lock) {
			nodeSuids.clear();
			nodeSuids.putAll(newValue);
		}
	}
	
	public void addNodeSuid(String key, Long suid) {
		synchronized (lock) {
			nodeSuids.put(key, suid);
		}
	}
	
	public void clearNodeSuids() {
		synchronized (lock) {
			nodeSuids.clear();
		}
	}
	
	public SetOfGeneSets getGeneSetsOfInterest() {
		return geneSetsOfInterest;
	}

	public void setGeneSetsOfInterest(SetOfGeneSets geneSetsOfInterest) {
		this.geneSetsOfInterest = geneSetsOfInterest;
	}
		
	@Override
	public int compareTo(AbstractDataSet other) {
		return collator.compare(getName(), other.getName());
	}
}
