package org.baderlab.csplugins.enrichmentmap.integration.task;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.baderlab.csplugins.enrichmentmap.AfterInjectionModule;
import org.baderlab.csplugins.enrichmentmap.ApplicationModule;
import org.baderlab.csplugins.enrichmentmap.CytoscapeServiceModule;
import org.baderlab.csplugins.enrichmentmap.integration.BaseIntegrationTest;
import org.baderlab.csplugins.enrichmentmap.integration.EdgeSimilarities;
import org.baderlab.csplugins.enrichmentmap.integration.SerialTestTaskManager;
import org.baderlab.csplugins.enrichmentmap.integration.TestUtils;
import org.baderlab.csplugins.enrichmentmap.model.DataSetFiles;
import org.baderlab.csplugins.enrichmentmap.model.EnrichmentMap;
import org.baderlab.csplugins.enrichmentmap.model.EnrichmentMapParameters;
import org.baderlab.csplugins.enrichmentmap.task.EnrichmentMapBuildMapTaskFactory;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.io.util.StreamUtil;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.session.CySessionManager;
import org.cytoscape.work.TaskIterator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.peaberry.osgi.OSGiModule;
import org.osgi.framework.BundleContext;

import com.google.inject.Guice;
import com.google.inject.Injector;

@RunWith(PaxExam.class)
public class EnrichmentMapTaskTest extends BaseIntegrationTest {
	
	private static final String PATH = "/EnrichmentMapTaskTest/";
	
	@Inject private CyNetworkManager networkManager;
	@Inject private CyApplicationManager applicationManager;
	@Inject private CySessionManager sessionManager;
	@Inject private StreamUtil streamUtil;
	@Inject private BundleContext bc;
	
	
	protected void buildEnrichmentMap(EnrichmentMapParameters emParams) {
		EnrichmentMap map = new EnrichmentMap(emParams);
		
		Injector injector = Guice.createInjector(new OSGiModule(bc), new AfterInjectionModule(), new CytoscapeServiceModule(), ApplicationModule.headless());
		EnrichmentMapBuildMapTaskFactory.Factory factory = injector.getInstance(EnrichmentMapBuildMapTaskFactory.Factory.class);
		
	   	EnrichmentMapBuildMapTaskFactory buildmap = factory.create(map);
	    
	   	TaskIterator taskIterator = buildmap.createTaskIterator();
	   	SerialTestTaskManager taskManager = new SerialTestTaskManager();
	   	taskManager.execute(taskIterator);
	}
	
	
	@Test
	public void testEnrichmentMapBuildMapTask() throws Exception {
		String geneSetsFile   = createTempFile(PATH, "gene_sets.gmt").getAbsolutePath();
		String expressionFile = createTempFile(PATH, "FakeExpression.txt").getAbsolutePath();
		String enrichmentFile = createTempFile(PATH, "fakeEnrichments.txt").getAbsolutePath();
		String rankFile       = createTempFile(PATH, "FakeRank.rnk").getAbsolutePath();
		
		EnrichmentMapParameters emParams = new EnrichmentMapParameters(sessionManager, streamUtil, applicationManager);
		emParams.setMethod(EnrichmentMapParameters.method_generic);
		DataSetFiles dataset1files = new DataSetFiles();
		dataset1files.setGMTFileName(geneSetsFile);  
		dataset1files.setExpressionFileName(expressionFile);
		dataset1files.setEnrichmentFileName1(enrichmentFile);
		dataset1files.setRankedFile(rankFile);  
		emParams.addFiles(EnrichmentMap.DATASET1, dataset1files);
		
	    buildEnrichmentMap(emParams);
	   	
	   	// Assert the network is as expected
	   	Set<CyNetwork> networks = networkManager.getNetworkSet();
	   	assertEquals(1, networks.size());
	   	CyNetwork network = networks.iterator().next();
	   	
	   	Map<String,CyNode> nodes = TestUtils.getNodes(network);
	   	assertEquals(4, nodes.size());
	   	assertTrue(nodes.containsKey("BOTTOM8_PLUS100"));
	   	assertTrue(nodes.containsKey("MIDDLE8_PLUS100"));
	   	assertTrue(nodes.containsKey("TOP8_PLUS100"));
	   	assertTrue(nodes.containsKey("TOP1_PLUS100"));
	   	
	   	EdgeSimilarities edges = TestUtils.getEdgeSimilarities(network);
	   	
	   	assertEquals(6, edges.size());
	   	assertTrue(edges.containsEdge("MIDDLE8_PLUS100", "Geneset_Overlap", "BOTTOM8_PLUS100"));
	   	assertTrue(edges.containsEdge("TOP8_PLUS100", "Geneset_Overlap", "MIDDLE8_PLUS100"));
	   	assertTrue(edges.containsEdge("TOP8_PLUS100", "Geneset_Overlap", "BOTTOM8_PLUS100"));
	   	assertTrue(edges.containsEdge("TOP1_PLUS100", "Geneset_Overlap", "TOP8_PLUS100"));
	   	assertTrue(edges.containsEdge("TOP1_PLUS100", "Geneset_Overlap", "MIDDLE8_PLUS100"));
	   	assertTrue(edges.containsEdge("TOP1_PLUS100", "Geneset_Overlap" ,"BOTTOM8_PLUS100"));
	}
	

}
