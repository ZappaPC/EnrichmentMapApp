/**
 **                       EnrichmentMap Cytoscape Plugin
 **
 ** Copyright (c) 2008-2009 Bader Lab, Donnelly Centre for Cellular and Biomolecular 
 ** Research, University of Toronto
 **
 ** Contact: http://www.baderlab.org
 **
 ** Code written by: Ruth Isserlin
 ** Authors: Daniele Merico, Ruth Isserlin, Oliver Stueker, Gary D. Bader
 **
 ** This library is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** (at your option) any later version.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** University of Toronto
 ** has no obligations to provide maintenance, support, updates, 
 ** enhancements or modifications.  In no event shall the
 ** University of Toronto
 ** be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** University of Toronto
 ** has been advised of the possibility of such damage.  
 ** See the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this library; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 **
 **/

// $Id$
// $LastChangedDate$
// $LastChangedRevision$
// $LastChangedBy$
// $HeadURL$

package org.baderlab.csplugins.enrichmentmap.task;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.baderlab.csplugins.enrichmentmap.model.EMCreationParameters;
import org.baderlab.csplugins.enrichmentmap.model.EMCreationParameters.SimilarityMetric;
import org.baderlab.csplugins.enrichmentmap.model.EnrichmentMap;
import org.baderlab.csplugins.enrichmentmap.model.EnrichmentMapParameters;
import org.baderlab.csplugins.enrichmentmap.model.EnrichmentResult;
import org.baderlab.csplugins.enrichmentmap.model.GeneSet;
import org.baderlab.csplugins.enrichmentmap.model.GenesetSimilarity;
import org.baderlab.csplugins.enrichmentmap.model.LegacySupport;
import org.baderlab.csplugins.enrichmentmap.model.PostAnalysisParameters;
import org.baderlab.csplugins.enrichmentmap.model.SimilarityKey;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import com.google.common.collect.Sets;

/**
 * Created by
 * User: risserlin
 * Date: Jan 9, 2009
 * Time: 2:14:52 PM
 * <p>
 * Goes through all the gene sets and computes the jaccard or overlap coeffecient
 * (depends on what the user specified in the input panel) for each
 * pair of gene sets.  (all pairwise comparisons are performed but only those passing
 * the user specified are stored in the hash map of gene set similarityes)
*/
public class ComputeSimilarityTask extends AbstractTask {
	public static final int ENRICHMENT = 0, SIGNATURE = 1;

	private EnrichmentMap map;
	private int type;


	/**
	 * Constructor for Compute Similarity task
	 */
	public ComputeSimilarityTask(EnrichmentMap map) {
		this.map = map;
		this.type = 0;
	}

	public ComputeSimilarityTask(EnrichmentMap map, int type) {
		this.map = map;
		this.type = type;
	}
	
	
	@Override
	public void run(TaskMonitor taskMonitor) {
		Map<String,GenesetSimilarity> similarities = computeGenesetSimilarities(taskMonitor);
		if(!cancelled)
			map.getGenesetSimilarity().putAll(similarities);
	}

	
	public Map<String,GenesetSimilarity> computeGenesetSimilarities(TaskMonitor taskMonitor) {
		if(!LegacySupport.isLegacyEnrichmentMap(map)) {
			throw new IllegalArgumentException("This task only works for legacy enrichment maps");
		}
		
		Map<SimilarityKey,GenesetSimilarity> similarities = new HashMap<>();
		
		Map<String, GeneSet> genesetsOfInterest;
		if (map.getParams().isDistinctExpressionSets())
			genesetsOfInterest = map.getDataset(LegacySupport.DATASET1).getGenesetsOfInterest().getGenesets();
		else
			genesetsOfInterest = map.getAllGenesetsOfInterest();
		
		Map<String,GeneSet> genesetsInnerLoop;
		String edgeType;
		if (type == ENRICHMENT) {
			genesetsInnerLoop = genesetsOfInterest;
			edgeType = map.getParams().getEnrichmentEdgeType();
		} else if (type == SIGNATURE) {
			//TODO refactor signature sets.
			genesetsInnerLoop = map.getSignatureGenesets();
			edgeType = PostAnalysisParameters.SIGNATURE_INTERACTION_TYPE;
		} else {
			genesetsInnerLoop = genesetsOfInterest;
			edgeType = "pp";
		}

		int total = genesetsOfInterest.size();
		if(map.getParams().isDistinctExpressionSets())
			total += genesetSize(LegacySupport.DATASET1) + genesetSize(LegacySupport.DATASET2);
		
		ProgressMonitor progress = new ProgressMonitor(taskMonitor, total);
		System.out.println("ComputeSimilarityTask.computeGenesetSimilarities() " + total);
		

		//figure out if we need to compute edges for two different expression sets or one.
		int enrichment_set = 0;
		if (map.getParams().isDistinctExpressionSets()) {
			//TODO if there are multiple species or different expression we need to loop through the datasets instead of treating all genesets as the same.
			enrichment_set = 1;
		}

		//iterate through the each of the GSEA Results of interest
		for(String geneset1_name : genesetsOfInterest.keySet()) {
			if(cancelled)
				return null;
			progress.inc();

			//for each individual geneset compute its jaccard index with all other genesets
			for(String geneset2_name : genesetsInnerLoop.keySet()) {
				if (geneset1_name.equalsIgnoreCase(geneset2_name))
					continue; //don't compare two identical genesets
				
				//Check to see if this comparison has been done
				//The key for the set of geneset similarities is the combination of the two names.  Check for either variation name1_name2 or name2_name1
				SimilarityKey similarity_key;
				if (enrichment_set == 0) {
					similarity_key = new SimilarityKey(geneset1_name, edgeType, geneset2_name);
				} else {
					similarity_key = new SimilarityKey(geneset1_name, EnrichmentMapParameters.ENRICHMENT_INTERACTION_TYPE_SET1, geneset2_name);
				}
				
				if (!similarities.containsKey(similarity_key) ) { // Skip this geneset comparison if it has already been done.
					
					GeneSet geneset1 = genesetsOfInterest.get(geneset1_name);
					GeneSet geneset2 = genesetsOfInterest.get(geneset2_name);

					//if the geneset1 is null, check to see if it is post analysis node
					if (geneset1 == null) {
						geneset1 = genesetsInnerLoop.get(geneset1_name);
					}
					//if the geneset1 is null, check to see if it is post analysis node
					if (geneset2 == null) {
						geneset2 = genesetsInnerLoop.get(geneset2_name);
					}
					
					GenesetSimilarity comparison = computeGenesetSimilarity(map.getParams(), geneset1_name, geneset2_name, geneset1, geneset2, enrichment_set);

					// MKTODO using swap() won't work
					// the reason it previoulsy used a different key was so that the enrichment and signature sets could live side-by side for the same pair of gene-sets
					
					if (type == SIGNATURE) {// as we iterate over the signature nodes in the inner loop, we have to switch the nodes in the edge name
						similarities.put(similarity_key.swap(), comparison);
					} else {
						similarities.put(similarity_key, comparison);
					}
				}
			}
		}
		
		//need to go through the second set of genesets in order to calculate the additional similarities
		//TODO:add two species support
		if (map.getParams().isDistinctExpressionSets()) {

			Map<String, GeneSet> sig_genesets_set1 = map.getDataset(LegacySupport.DATASET1).getGenesetsOfInterest().getGenesets();
			Map<String, GeneSet> sig_genesets_set2 = map.getDataset(LegacySupport.DATASET2).getGenesetsOfInterest().getGenesets();
			
			enrichment_set = 2;
			
			//iterate through the each of the GSEA Results of interest - for the second set.
			for (String geneset1_name : sig_genesets_set2.keySet()) {
				if(cancelled)
					return null;
				progress.inc();

				//for each individual geneset compute its jaccard index with all other genesets
				for (String geneset2_name : sig_genesets_set2.keySet()) {
					if (geneset1_name.equalsIgnoreCase(geneset2_name))
						continue; //don't compare two identical genesets
					
					//Check to see if this comparison has been done
					//The key for the set of geneset similarities is the combination of the two names.  Check for either variation name1_name2 or name2_name1
					SimilarityKey similarity_key = new SimilarityKey(geneset1_name, EnrichmentMapParameters.ENRICHMENT_INTERACTION_TYPE_SET2, geneset2_name);

					if (!similarities.containsKey(similarity_key)) { //skip this geneset comparison if it has already been done.
						GeneSet geneset1 = sig_genesets_set2.get(geneset1_name);
						GeneSet geneset2 = sig_genesets_set2.get(geneset2_name);

						GenesetSimilarity comparison = computeGenesetSimilarity(map.getParams(), geneset1_name, geneset2_name, geneset1, geneset2, enrichment_set);
						
						if (type == SIGNATURE) {// as we iterate over the signature nodes in the inner loop, we have to switch the nodes in the edge name
							similarities.put(similarity_key.swap(), comparison);
						} else {
							similarities.put(similarity_key, comparison);
						}
					}
				}
			}
			
			//We need to also compute the edges between the two different groups.
			Map<String, GeneSet> genesetsOfInterest_missingedges = map.getAllGenesets();
			Map<String, EnrichmentResult> dataset1_results = map.getDataset(LegacySupport.DATASET1).getEnrichments().getEnrichments();

			//iterate through the each of the GSEA Results of interest - for the second set on the outer loop and the first set on the inner loop
			for (String geneset1_name : sig_genesets_set2.keySet()) {
				if(cancelled)
					return null;
				progress.inc();
				
				enrichment_set = 1;

				//only look at this geneset if it is in dataset1
				if (!dataset1_results.containsKey(geneset1_name))
					continue;

				//for each individual geneset compute its jaccard index with all other genesets
				for (String geneset2_name : sig_genesets_set1.keySet()) {

					//only look at this geneset if it is in dataset1
					if (!dataset1_results.containsKey(geneset2_name))
						continue;
					
					if (geneset1_name.equalsIgnoreCase(geneset2_name))
						continue; //don't compare two identical genesets

					//Check to see if this comparison has been done
					//The key for the set of geneset similarities is the combination of the two names.  Check for either variation name1_name2 or name2_name1
					SimilarityKey similarity_key = new SimilarityKey(geneset1_name, EnrichmentMapParameters.ENRICHMENT_INTERACTION_TYPE_SET1, geneset2_name);

					if (!similarities.containsKey(similarity_key)) {
						boolean s1g1 = sig_genesets_set1.containsKey(geneset1_name);
						boolean s2g1 = sig_genesets_set2.containsKey(geneset1_name);
						boolean s1g2 = sig_genesets_set1.containsKey(geneset2_name);
						boolean s2g2 = sig_genesets_set2.containsKey(geneset2_name);
						
						// both genesets must be significant in both datasets
						if(s1g1 && s2g1 && s1g2 && s2g2) {
							GeneSet geneset1 = genesetsOfInterest_missingedges.get(geneset1_name);
							GeneSet geneset2 = genesetsOfInterest_missingedges.get(geneset2_name);

							GenesetSimilarity comparison = computeGenesetSimilarity(map.getParams(), geneset1_name, geneset2_name, geneset1, geneset2, enrichment_set);

							if (type == SIGNATURE) {// as we iterate over the signature nodes in the inner loop, we have to switch the nodes in the edge name
								similarities.put(similarity_key.swap(), comparison);
							} else {
								similarities.put(similarity_key, comparison);
							}
						}
					}
				}
			}
		}
		
		Map<String,GenesetSimilarity> genesetSimilarities = new HashMap<>();
		for(Map.Entry<SimilarityKey,GenesetSimilarity> entry : similarities.entrySet()) {
			genesetSimilarities.put(entry.getKey().toString(), entry.getValue());
		}
		return genesetSimilarities;
	}
	
	
	private int genesetSize(String dataSet) {
		return map.getDataset(dataSet).getGenesetsOfInterest().getGenesets().size();
	}
	
	
	public static double computeSimilarityCoeffecient(EMCreationParameters params, Set<?> intersection, Set<?> union, Set<?> genes1, Set<?> genes2) {
		// Note: Do not call intersection.size() or union.size() more than once on a Guava SetView! 
		// It is a potentially slow operation that needs to be recalcuated each time it is called.
		
		if (params.getSimilarityMetric() == SimilarityMetric.JACCARD) {
			return (double) intersection.size() / (double) union.size();
		} 
		else if (params.getSimilarityMetric() == SimilarityMetric.OVERLAP) {
			return (double) intersection.size() / Math.min((double) genes1.size(), (double) genes2.size());
		} 
		else { 
			// It must be combined. Compute a combination of the overlap and jaccard coefecient. We need both the Jaccard and the Overlap.
			double intersectionSize = (double) intersection.size(); // do not call size() more than once on the same SetView
			
			double jaccard = intersectionSize / (double) union.size();
			double overlap = intersectionSize / Math.min((double) genes1.size(), (double) genes2.size());

			double k = params.getCombinedConstant();

			return (k * overlap) + ((1 - k) * jaccard);
		}
	}

	
	private static GenesetSimilarity computeGenesetSimilarity(EMCreationParameters params, String geneset1Name, String geneset2Name, GeneSet geneset1, GeneSet geneset2, int enrichment_set) {
		// MKTODO: Should not need to pass in the geneset names, should just use geneset.getName(), but I'm nervous I might break something.
		
		Set<Integer> genes1 = geneset1.getGenes();
		Set<Integer> genes2 = geneset2.getGenes();

		Set<Integer> intersection = Sets.intersection(genes1, genes2);
		Set<Integer> union = Sets.union(genes1, genes2);

		double coeffecient = computeSimilarityCoeffecient(params, intersection, union, genes1, genes2);
		
		String edgeType = params.getEnrichmentEdgeType();
		GenesetSimilarity similarity = new GenesetSimilarity(geneset1Name, geneset2Name, coeffecient, edgeType, intersection, enrichment_set);
		return similarity;
	}
	
	
	
	private class ProgressMonitor {
		private int currentProgress = 0;
		private int total;
		private TaskMonitor taskMonitor;
		
		public ProgressMonitor(TaskMonitor tm, int total) {
			this.taskMonitor = tm == null ? new NullTaskMonitor() : tm;
			this.total = total;
			taskMonitor.setTitle("Computing geneset similarities");
		}
		
		public void inc() {
			currentProgress++;
			taskMonitor.setProgress((double) currentProgress / (double) total);
			taskMonitor.setStatusMessage("Computing Geneset similarity: " + currentProgress + " of " + total + " similarities");
		}
	}
	

	/**
	 * Gets the Task Title.
	 *
	 * @return human readable task title.
	 */
	public String getTitle() {
		return "Computing geneset similarities";
	}

	
}
