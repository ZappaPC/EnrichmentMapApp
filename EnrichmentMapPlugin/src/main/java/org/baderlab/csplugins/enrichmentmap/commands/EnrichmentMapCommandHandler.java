package org.baderlab.csplugins.enrichmentmap.commands;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import org.baderlab.csplugins.enrichmentmap.EnrichmentMapParameters;
import org.baderlab.csplugins.enrichmentmap.model.DataSetFiles;
import org.baderlab.csplugins.enrichmentmap.model.EnrichmentMap;
import org.baderlab.csplugins.enrichmentmap.task.BuildEnrichmentMapTask;

import cytoscape.command.AbstractCommandHandler;
import cytoscape.command.CyCommandException;
import cytoscape.command.CyCommandManager;
import cytoscape.command.CyCommandResult;
import cytoscape.layout.Tunable;
import cytoscape.task.ui.JTaskConfig;
import cytoscape.task.util.TaskManager;

public class EnrichmentMapCommandHandler extends AbstractCommandHandler {
		
		private static String command = "build";
		private static String edbdir = "edbdir";
		private static String edbdir2 = "edbdir2";
		private static String pvalue = "pvalue";
		private static String qvalue = "qvalue";
		private static String overlap = "overlap";
		private static String expressionfile = "expressionfile";
		private static String expressionfile2 = "expressionfile2";
		private static String similaritymetric = "similaritymetric";
		private static String combinedconstant = "combinedconstant";
	
		public EnrichmentMapCommandHandler(String namespace) {
			super(CyCommandManager.reserveNamespace(namespace));
			
			addDescription(command,"Build an enrichmentmap from GSEA results (in an edb directory)");
			addArgument(command, edbdir);
			addArgument(command, edbdir2);
			addArgument(command, pvalue);
			addArgument(command, qvalue);
			addArgument(command, overlap);
			addArgument(command, similaritymetric);
			addArgument(command, combinedconstant);
			addArgument(command, expressionfile);
			addArgument(command, expressionfile2);
			
		}
		
		public CyCommandResult execute(String arg0, Map<String, Object> arg1)
				throws CyCommandException {
			
			//create a results object
			CyCommandResult results = new CyCommandResult();
			String file = "";
			String file2 = "";
			Double pvalue_loaded = 0.05;
			Double qvalue_loaded = 0.25;
			Double overlap_loaded = 0.5;
			String expression_loaded = "";
			String expression2_loaded = "";
			Double combinedconstant_loaded = 0.5;
			String similaritymetric_loaded = EnrichmentMapParameters.SM_OVERLAP;
			
			//get the edb file to run enrichment maps on
			//If working on windows and user cuts and copies path there is 
			//no way to correct forward slashes.  User needs to use double forward slashes
			//or backward slashes.
			if(arg1.containsKey(edbdir))
				file = (String)arg1.get(edbdir);
			
			if(arg1.containsKey(edbdir2))
				file2 = (String)arg1.get(edbdir2);
				
			//get the expression file if it is specified
			if(arg1.containsKey(expressionfile))
				expression_loaded = (String)arg1.get(expressionfile);
			if(arg1.containsKey(expressionfile2))
				expression2_loaded = (String)arg1.get(expressionfile2);
			
			//get other parameters if they are present:
			if(arg1.containsKey(pvalue)){
				try{
					pvalue_loaded = Double.parseDouble((String)arg1.get(pvalue));
				}catch(NumberFormatException e){
					System.out.println("the pvalue can only be a number (i.e. pvalue=0.05).  Ignored pvalue setting by user and using default.");
					pvalue_loaded = 0.05;
				}
			}
			
			if(arg1.containsKey(qvalue)){
				try{
					qvalue_loaded = Double.parseDouble((String)arg1.get(qvalue));
				}catch(NumberFormatException e){
					System.out.println("the qvalue can only be a number (i.e. qvalue=0.25).  Ignored qvalue setting by user and using default.");
					qvalue_loaded = 0.25;
				}
			}
			
			if(arg1.containsKey(overlap)){
				try{
					overlap_loaded = Double.parseDouble((String)arg1.get(overlap));
				}catch(NumberFormatException e){
					System.out.println("the overlap can only be a number (i.e. overlap=0.50).  Ignored overlap setting by user and using default.");
					overlap_loaded = 0.50;
				}
			}
			
			//get other parameters if they are present:
			if(arg1.containsKey(combinedconstant)){
				try{
					combinedconstant_loaded = Double.parseDouble((String)arg1.get(combinedconstant));
				}catch(NumberFormatException e){
					System.out.println("the combinedconstant can only be a number (i.e. combinedconstant=0.5).  Ignored combinedconstant setting by user and using default.");
					combinedconstant_loaded = 0.05;
				}
			}
			//get the expression file if it is specified
			if(arg1.containsKey(similaritymetric)){
				similaritymetric_loaded = (String)arg1.get(similaritymetric);
				if(similaritymetric_loaded.equalsIgnoreCase(EnrichmentMapParameters.SM_OVERLAP))
					similaritymetric_loaded = EnrichmentMapParameters.SM_OVERLAP;
				else if(similaritymetric_loaded.equalsIgnoreCase(EnrichmentMapParameters.SM_JACCARD))
					similaritymetric_loaded = EnrichmentMapParameters.SM_JACCARD;
				else if(similaritymetric_loaded.equalsIgnoreCase(EnrichmentMapParameters.SM_COMBINED))
					similaritymetric_loaded = EnrichmentMapParameters.SM_COMBINED;
				//if it doesn't match any of the presets then use the default
				else 
					similaritymetric_loaded = EnrichmentMapParameters.SM_OVERLAP;
			}
			EnrichmentMapParameters params = new EnrichmentMapParameters();
			
			//set all files as extracted from the edb directory
			DataSetFiles files = this.InitializeFiles(file, expression_loaded);
			if(!expression_loaded.equals("")){
				params.setData(true);
			}
			params.addFiles(EnrichmentMap.DATASET1, files);
			//only add second dataset if there is a second edb directory.
			if(!file2.equalsIgnoreCase("")){
				params.setTwoDatasets(true);
				DataSetFiles files2 = this.InitializeFiles(file2, expression2_loaded);
				if(!expression2_loaded.equals("")){
					params.setData2(true);
				}
				params.addFiles(EnrichmentMap.DATASET2, files2);
			}
			
			//set the method to gsea
			params.setMethod(EnrichmentMapParameters.method_GSEA);
			params.setSimilarityMetric(similaritymetric_loaded);
			params.setSimilarityCutOff(overlap_loaded);
			params.setPvalue(pvalue_loaded);
			params.setQvalue(qvalue_loaded);
			params.setFDR(true);
			params.setCombinedConstant(combinedconstant_loaded);
			
			JTaskConfig config = new JTaskConfig();
	        config.displayCancelButton(true);
	        config.displayCloseButton(true);
	        config.displayStatus(true);
			
			//Build EnrichmentMap
	        BuildEnrichmentMapTask new_map = new BuildEnrichmentMapTask(params);
            boolean success = TaskManager.executeTask(new_map,config);
			
			return results;
		}

		public CyCommandResult execute(String arg0, Collection<Tunable> arg1)
				throws CyCommandException {
			return execute(command, createKVMap(arg1));
		}

		private DataSetFiles InitializeFiles(String edb, String exp){

			//for a dataset we require genesets, an expression file (optional), enrichment results
			String file_sep = System.getProperty("file.separator");
			String testEdbResultsFileName = edb + file_sep + "results.edb";
			String testgmtFileName = edb + file_sep + "gene_sets.gmt";
			
			//the rank file does not have a set name.  We need to figure out the name
			//of the rank file
			String testrnkFileName = "";	
			File directory = new File(edb);
			String[] dir_listing = directory.list();
			if(dir_listing.length > 0){
				for(int i = 0 ; i < dir_listing.length;i++){
					if(dir_listing[i].endsWith("rnk") && testrnkFileName.equals(""))
						testrnkFileName = edb + file_sep + dir_listing[i];
					else if(dir_listing[i].endsWith("rnk") && !testrnkFileName.equals(""))
						System.out.println("There are two rnk files in the edb directory.  Using the first one found");
				}
			}
			
			DataSetFiles files = new DataSetFiles();		
			files.setEnrichmentFileName1(testEdbResultsFileName);
			files.setGMTFileName(testgmtFileName);
			if(!testrnkFileName.equals(""))
				files.setRankedFile(testrnkFileName);
			if(!exp.equals("")){
				files.setExpressionFileName(exp);
			}
			return files;
		}
}