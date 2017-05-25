package org.baderlab.csplugins.enrichmentmap.task.postanalysis;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.baderlab.csplugins.enrichmentmap.model.EMDataSet;
import org.baderlab.csplugins.enrichmentmap.model.EMSignatureDataSet;
import org.baderlab.csplugins.enrichmentmap.model.EnrichmentMap;
import org.baderlab.csplugins.enrichmentmap.model.GeneSet;
import org.baderlab.csplugins.enrichmentmap.model.GenesetSimilarity;
import org.baderlab.csplugins.enrichmentmap.model.PostAnalysisFilterType;
import org.baderlab.csplugins.enrichmentmap.model.PostAnalysisParameters;
import org.baderlab.csplugins.enrichmentmap.model.Ranking;
import org.baderlab.csplugins.enrichmentmap.model.SimilarityKey;
import org.baderlab.csplugins.enrichmentmap.task.ComputeSimilarityTaskParallel;
import org.baderlab.csplugins.enrichmentmap.util.DiscreteTaskMonitor;
import org.baderlab.csplugins.enrichmentmap.util.NamingUtil;
import org.baderlab.csplugins.mannwhit.MannWhitneyMemoized;
import org.baderlab.csplugins.mannwhit.MannWhitneyTestResult;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskMonitor;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * Post Analysis.
 * Computes similarities between the signature gene sets that are being added and the 
 * existing gene sets in the enrichment map network.
 * 
 * Note: This task does not have side effects and is safely cancellable.
 * This task creates the EMSignatureDataSet and GenesetSimilarity objects, the next
 * task, CreateDiseaseSignatureNetworkTask has all the side effects of adding
 * nodes/edges to the network.
 */
public class CreateDiseaseSignatureTaskParallel extends AbstractTask {

	public static final String INTERACTION = PostAnalysisParameters.SIGNATURE_INTERACTION_TYPE;

	
	@Inject private CreateDiseaseSignatureNetworkTask.Factory networkTaskFactory;
	
	private final EnrichmentMap map;
	private final PostAnalysisParameters params;
	private final List<EMDataSet> dataSets;
	
	private final MannWhitneyMemoized mannWhitneyCache = new MannWhitneyMemoized();
	
	
	public static interface Factory {
		CreateDiseaseSignatureTaskParallel create(PostAnalysisParameters params, EnrichmentMap map);
		CreateDiseaseSignatureTaskParallel create(PostAnalysisParameters params, EnrichmentMap map, List<EMDataSet> dataSets);
	}
	
	@AssistedInject
	public CreateDiseaseSignatureTaskParallel(@Assisted PostAnalysisParameters params, @Assisted EnrichmentMap map) {
		this(params, map, map.getDataSetList());
	}
	
	@AssistedInject
	public CreateDiseaseSignatureTaskParallel(@Assisted PostAnalysisParameters params, @Assisted EnrichmentMap map, @Assisted List<EMDataSet> dataSets) {
		this.map = map;
		this.dataSets = dataSets;
		
		// Make sure there's a prefix
		if(params.getAttributePrefix() == null)
			this.params = PostAnalysisParameters.Builder.from(params).setAttributePrefix("EM1_").build();
		else
			this.params = params;
	}
	
	
	@Override
	public void run(TaskMonitor tm) throws InterruptedException {
		int cpus = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(cpus);
        
        // Compare enrichment gene sets to signature gene sets
 		Set<String> enrichmentGeneSetNames = getEnrichmentGeneSetNames();
 		Map<String,GeneSet> signatureGeneSets = getSignatureGeneSets();
 		handleDuplicateNames(enrichmentGeneSetNames, signatureGeneSets);
     		
 		EMSignatureDataSet sigDataSet = getOrCreateSignatureDataSet();
		signatureGeneSets.forEach(sigDataSet.getGeneSetsOfInterest()::addGeneSet);
		
        Map<SimilarityKey,GenesetSimilarity> geneSetSimilarities = startBuildDiseaseSignatureParallel(tm, executor, enrichmentGeneSetNames, signatureGeneSets);

        // Support cancellation
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				if(cancelled) {
					executor.shutdownNow();
				}
			}
		}, 0, 1000);
			
		executor.shutdown();
		executor.awaitTermination(3, TimeUnit.HOURS);
		timer.cancel();
		
		// create the network here
		if(!cancelled) {
			Task networkTask = networkTaskFactory.create(map, params, sigDataSet, geneSetSimilarities);
			insertTasksAfterCurrentTask(networkTask);
		}
	}


	/**
	 * Returns immediately, need to wait on the executor to join all threads.
	 */
	private Map<SimilarityKey,GenesetSimilarity> startBuildDiseaseSignatureParallel(TaskMonitor tm, 
			ExecutorService executor, Set<String> enrichmentGeneSetNames, Map<String,GeneSet> signatureGeneSets) {
		
		DiscreteTaskMonitor taskMonitor = discreteTaskMonitor(tm, signatureGeneSets.size());
		
		Set<Integer> geneUniverse = map.getAllEnrichmentGenes(); // Gene universe is all enrichment genes in the map
		
		Map<SimilarityKey, GenesetSimilarity> geneSetSimilarities = new ConcurrentHashMap<>();
		
		for(String hubName : signatureGeneSets.keySet()) {
			GeneSet sigGeneSet = signatureGeneSets.get(hubName);
			Set<Integer> sigGenesInUniverse = Sets.intersection(sigGeneSet.getGenes(), geneUniverse);
			
			// Compute similarities in batches
			executor.execute(() -> {
				loop:
				for(String geneSetName : enrichmentGeneSetNames) {
					for(EMDataSet dataSet : dataSets) {
						if(Thread.interrupted())
							break loop;
						
						GeneSet enrGeneSet = dataSet.getSetOfGeneSets().getGeneSetByName(geneSetName);
						if(enrGeneSet != null) {
							// restrict to a common gene universe
							Set<Integer> enrGenes = Sets.intersection(enrGeneSet.getGenes(), geneUniverse);
							Set<Integer> union = Sets.union(sigGeneSet.getGenes(), enrGenes);
							Set<Integer> intersection = Sets.intersection(sigGenesInUniverse, enrGenes);

							if(!intersection.isEmpty()) {
								double coeffecient = ComputeSimilarityTaskParallel.computeSimilarityCoeffecient(map.getParams(), intersection, union, sigGeneSet.getGenes(), enrGenes);
								GenesetSimilarity comparison = new GenesetSimilarity(hubName, geneSetName, coeffecient, INTERACTION, intersection);

								PostAnalysisFilterType filterType = params.getRankTestParameters().getType();
								switch (filterType) {
									case HYPERGEOM:
										int hyperUniverseSize1 = getHypergeometricUniverseSize(dataSet);
										hypergeometric(hyperUniverseSize1, sigGenesInUniverse, enrGenes, intersection, comparison);
										break;
									case MANN_WHIT_TWO_SIDED:
									case MANN_WHIT_GREATER:
									case MANN_WHIT_LESS:
										mannWhitney(intersection, comparison, dataSet);
									default: // want mann-whit to fall through
										int hyperUniverseSize2 = map.getNumberOfGenes(); // #70 calculate hypergeometric also
										hypergeometric(hyperUniverseSize2, sigGenesInUniverse, enrGenes, intersection, comparison);
										break;
								}

								SimilarityKey key = new SimilarityKey(hubName, geneSetName, INTERACTION, dataSet.getName());
								geneSetSimilarities.put(key, comparison);
							}
							
						}
					}
				}
				taskMonitor.inc();
			});
		}
		
		return geneSetSimilarities;
	}
	
	
	/**
	 * Note: This method does not add the data set to the map. This task can be cancelled so it should be added at the end.
	 */
	private EMSignatureDataSet getOrCreateSignatureDataSet() {
		String name = params.getName();
		if(name == null || name.trim().isEmpty())
			name = NamingUtil.getUniqueName(params.getLoadedGMTGeneSets().getName(), map.getSignatureDataSets().keySet());
		
		// To emulate EM2 behaviour there can only be one signature data set
		if(map.hasSignatureDataSets())
			return map.getSignatureDataSets().values().iterator().next();
		else
			return new EMSignatureDataSet(name);
	}
	
	
	private int getHypergeometricUniverseSize(EMDataSet dataSet) {
		switch(params.getUniverseType()) {
			default:
			case GMT:
				return map.getNumberOfGenes();
			case EXPRESSION_SET:
				return dataSet.getExpressionSets().getExpressionUniverse();
			case INTERSECTION:
				return dataSet.getExpressionSets().getExpressionMatrix().size();
			case USER_DEFINED:
				return params.getUserDefinedUniverseSize();
		}
	}
	
	private void hypergeometric(int universeSize, Set<Integer> sigGenesInUniverse, Set<Integer> enrGenes,
			Set<Integer> intersection, GenesetSimilarity comparison) {
		// Calculate Hypergeometric pValue for Overlap
		int u = universeSize; //number of total genes (size of population / total number of balls)
		int n = sigGenesInUniverse.size(); //size of signature geneset (sample size / number of extracted balls)
		int m = enrGenes.size(); //size of enrichment geneset (success Items / number of white balls in population)
		int k = intersection.size(); //size of intersection (successes /number of extracted white balls)
		double hyperPval;

		if (k > 0)
			hyperPval = Hypergeometric.hyperGeomPvalueSum(u, n, m, k, 0);
		else // Correct p-value of empty intersections to 1 (i.e. not significant)
			hyperPval = 1.0;

		comparison.setHypergeomPValue(hyperPval);
		comparison.setHypergeomU(u);
		comparison.setHypergeomN(n);
		comparison.setHypergeomM(m);
		comparison.setHypergeomK(k);
	}
	

	private void mannWhitney(Set<Integer> intersection, GenesetSimilarity comparison, EMDataSet dataSet) {
		String rankFile = params.getDataSetToRankFile().get(dataSet.getName());
		Ranking ranks = dataSet.getExpressionSets().getRanks().get(rankFile);

		// Calculate Mann-Whitney U pValue for Overlap
		Integer[] overlapGeneIds = intersection.toArray(new Integer[intersection.size()]);

		double[] overlapGeneScores = new double[overlapGeneIds.length];
		int j = 0;
		
		for (Integer geneId : overlapGeneIds) {
			Double score = ranks.getScore(geneId);
			if (score != null) {
				overlapGeneScores[j++] = score; // unbox
			}
		}

		overlapGeneScores = Arrays.copyOf(overlapGeneScores, j);
		
		if (ranks.isEmpty()) {
			comparison.setMannWhitPValueTwoSided(1.5); // avoid NoDataException
			comparison.setMannWhitPValueGreater(1.5);
			comparison.setMannWhitPValueLess(1.5);
			comparison.setMannWhitMissingRanks(true);
		} else {
			double[] scores = ranks.getScores();
			MannWhitneyTestResult result = mannWhitneyCache.mannWhitneyUTestBatch(overlapGeneScores, scores);
			comparison.setMannWhitPValueTwoSided(result.twoSided);
			comparison.setMannWhitPValueGreater(result.greater);
			comparison.setMannWhitPValueLess(result.less);
		}
	}
	
	/**
	 * Return the signature gene sets that the user want's to add.
	 * Take all the gene sets that were loaded and filter out ones that were not selected.
	 */
	private Map<String,GeneSet> getSignatureGeneSets() {
		Map<String,GeneSet> loadedGeneSets = params.getLoadedGMTGeneSets().getGeneSets();
		Map<String,GeneSet> selectedGeneSets = new HashMap<>();
		for(String geneset : params.getSelectedGeneSetNames()) {
			selectedGeneSets.put(geneset, loadedGeneSets.get(geneset));
		}
		return selectedGeneSets;
	}
	
	
	private static void handleDuplicateNames(Set<String> enrichmentGeneSetNames, Map<String,GeneSet> signatureGeneSets) {
		Set<String> duplicates = signatureGeneSets.keySet().stream().filter(enrichmentGeneSetNames::contains).collect(Collectors.toSet());
		for(String name : duplicates) {
			signatureGeneSets.put("PA_" + name, signatureGeneSets.remove(name));
		}
	}
	
	/**
	 * Return the names of all the non-signature gene sets in the EnrichmentMap.
	 */
	private Set<String> getEnrichmentGeneSetNames() {
		Set<String> names = map.getAllGeneSetOfInterestNames();
		return names;
	}
	
	
	private static DiscreteTaskMonitor discreteTaskMonitor(TaskMonitor tm, int size) {
		DiscreteTaskMonitor taskMonitor = new DiscreteTaskMonitor(tm, size);
        taskMonitor.setTitle("Post Analysis Geneset Similarities...");
        taskMonitor.setStatusMessageTemplate("Computing Geneset Similarity: {0} of {1} tasks");
        return taskMonitor;
	}
	
}