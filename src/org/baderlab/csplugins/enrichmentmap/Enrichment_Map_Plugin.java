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

package org.baderlab.csplugins.enrichmentmap;
import cytoscape.plugin.CytoscapePlugin;
import cytoscape.Cytoscape;
import cytoscape.CytoscapeInit;
import cytoscape.task.ui.JTaskConfig;
import cytoscape.task.util.TaskManager;
import cytoscape.task.TaskMonitor;
import cytoscape.view.CyNetworkView;
import cytoscape.data.readers.TextFileReader;


import javax.swing.*;

import java.io.File;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;


public class Enrichment_Map_Plugin extends CytoscapePlugin {
    static Properties build_props = new Properties();
    static Properties plugin_props = new Properties();
    static String buildId ;

    /*--------------------------------------------------------------
      CONSTRUCTOR.
      --------------------------------------------------------------*/
    public Enrichment_Map_Plugin(){

        //set-up menu options in plugins menu
        JMenu menu = Cytoscape.getDesktop().getCyMenus().getOperationsMenu();
        JMenuItem item;

        //Enrichment map submenu
        JMenu submenu = new JMenu("Enrichment Maps");

        //GSEA results panel panel
        item = new JMenuItem("Load Enrichment Results");
        item.addActionListener(new LoadGSEAPanelAction());
        submenu.add(item);

        //Post Analysis panel
        item = new JMenuItem("Post Analysis");
        item.addActionListener(new LoadPostAnalysisPanelAction());
        submenu.add(item);


        //About Box
        item = new JMenuItem("About");
        item.addActionListener(new ShowAboutPanelAction());
        submenu.add(item);

        menu.add(submenu);

        // read buildId properties:
        try {
            Enrichment_Map_Plugin.build_props = getPropertiesFromClasspath("buildID.props");
        } catch (IOException e) {
            // TODO: write Warning "Could not load 'buildID.props' - using default settings"
            Enrichment_Map_Plugin.build_props.setProperty("build.number", "0");
            Enrichment_Map_Plugin.build_props.setProperty("svn.revision", "0");
            Enrichment_Map_Plugin.build_props.setProperty("build.user", "user");
            Enrichment_Map_Plugin.build_props.setProperty("build.host", "host");
            Enrichment_Map_Plugin.build_props.setProperty("build.timestemp", "1900/01/01 00:00:00 +0000 (GMT)");
        }

        Enrichment_Map_Plugin.buildId = "Build: " + Enrichment_Map_Plugin.build_props.getProperty("build.number") +
                                        " from SVN: " + Enrichment_Map_Plugin.build_props.getProperty("svn.revision") +
                                        " by: " + Enrichment_Map_Plugin.build_props.getProperty("build.user") + "@" + Enrichment_Map_Plugin.build_props.getProperty("build.host") +
                                        " at: " + Enrichment_Map_Plugin.build_props.getProperty("build.timestamp") ;

        try {
            Enrichment_Map_Plugin.plugin_props = getPropertiesFromClasspath("org/baderlab/csplugins/enrichmentmap/plugin.props");
        } catch (IOException e) {
            // TODO: write Warning "Could not load 'plugin.props' - using default settings"
        }

    }

    public void onCytoscapeExit(){

        //test to see if we can write anything to a file in the session file
        File propFile = CytoscapeInit.getConfigFile("enrichmentMap.props");

        try{
            BufferedWriter writer = new BufferedWriter(new FileWriter(propFile));
            writer.write("line 1:");
            writer.newLine();
            writer.write("line2:");
            writer.close();
        } catch(Exception ex){
            ex.printStackTrace();
        }

    }

    public void saveSessionStateFiles(List<File> pFileList){
        // Create an empty file on system temp directory

        String tmpDir = System.getProperty("java.io.tmpdir");
        System.out.println("java.io.tmpdir: [" + tmpDir + "]");

        //get the networks
        HashMap<String, EnrichmentMapParameters> networks = EnrichmentMapManager.getInstance().getCyNetworkList();

        //create a props file for each network
        for(Iterator<String> i = networks.keySet().iterator(); i.hasNext();){
            String networkId = i.next().toString();
            EnrichmentMapParameters params = networks.get(networkId);
            String name = Cytoscape.getNetwork(networkId).getTitle();

            //get the network name specified in the parameters
            String param_name = params.getNetworkName();

            //check to see if the name of the network matches the one specified in it parameters
            //if the two names differ then use the network name specified by the user
            if(!name.equalsIgnoreCase(param_name))
                params.setNetworkName(name);

            File session_prop_file = new File(tmpDir, name+".props");
            File gmt = new File(tmpDir, name+".gmt");
            File genes = new File(tmpDir, name+".genes.txt");
            File hkgenes = new File(tmpDir, name+".hashkey2genes.txt");

            File enrichmentresults1 = new File(tmpDir, name+".ENR1.txt");
            File enrichmentresults1Ofinterest = new File(tmpDir, name+".SubENR1.txt");

            File enrichmentresults2;
            File enrichmentresults2Ofinterest;
            File expression1;
            File expression2;

            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(session_prop_file));
                writer.write(params.toString());
                writer.close();

                BufferedWriter gmtwriter = new BufferedWriter(new FileWriter(gmt));
                gmtwriter.write(params.printHashmap(params.getGenesetsOfInterest()));
                gmtwriter.close();
                pFileList.add(gmt);

                BufferedWriter geneswriter = new BufferedWriter(new FileWriter(genes));
                geneswriter.write(params.printHashmap(params.getGenes()));
                geneswriter.close();
                pFileList.add(genes);

                BufferedWriter hashkey2geneswriter = new BufferedWriter(new FileWriter(hkgenes));
                hashkey2geneswriter.write(params.printHashmap(params.getHashkey2gene()));
                hashkey2geneswriter.close();
                pFileList.add(hkgenes);

                BufferedWriter enr1writer = new BufferedWriter(new FileWriter(enrichmentresults1));
                enr1writer.write(params.printHashmap(params.getEnrichmentResults1()));
                enr1writer.close();
                pFileList.add(enrichmentresults1);

                BufferedWriter subenr1writer = new BufferedWriter(new FileWriter(enrichmentresults1Ofinterest));
                subenr1writer.write(params.printHashmap(params.getEnrichmentResults1OfInterest()));
                subenr1writer.close();
                pFileList.add(enrichmentresults1Ofinterest);

                if(params.getDataset1RankedFile()!=null){
                     File ranks1 = new File(tmpDir, name+".RANKS1.txt");
                    BufferedWriter subrank1writer = new BufferedWriter(new FileWriter(ranks1));
                    subrank1writer.write(params.printHashmap(params.getDataset1Rankings()));
                    subrank1writer.close();
                    pFileList.add(ranks1);

                }

                if(params.isTwoDatasets()){
                    enrichmentresults2 = new File(tmpDir, name+".ENR2.txt");
                    BufferedWriter enr2writer = new BufferedWriter(new FileWriter(enrichmentresults2));
                    enr2writer.write(params.printHashmap(params.getEnrichmentResults2()));
                    enr2writer.close();
                    pFileList.add(enrichmentresults2);

                    enrichmentresults2Ofinterest = new File(tmpDir, name+".SubENR2.txt");
                    BufferedWriter subenr2writer = new BufferedWriter(new FileWriter(enrichmentresults2Ofinterest));
                    subenr2writer.write(params.printHashmap(params.getEnrichmentResults1OfInterest()));
                    subenr2writer.close();
                    pFileList.add(enrichmentresults2Ofinterest);

                    if(params.getDataset2RankedFile()!=null){
                        File ranks2 = new File(tmpDir, name+".RANKS2.txt");
                        BufferedWriter subrank2writer = new BufferedWriter(new FileWriter(ranks2));
                        subrank2writer.write(params.printHashmap(params.getDataset2Rankings()));
                        subrank2writer.close();
                        pFileList.add(ranks2);

                    }
                }

                if(params.isData()){
                    expression1 = new File(tmpDir, name+".expression1.txt");
                    BufferedWriter expression1writer = new BufferedWriter(new FileWriter(expression1));
                    expression1writer.write(params.getExpression().toString());
                    expression1writer.close();
                    pFileList.add(expression1);
                }
                if(params.isData2()){
                    expression2 = new File(tmpDir, name+".expression2.txt");
                    BufferedWriter expression2writer = new BufferedWriter(new FileWriter(expression2));
                    expression2writer.write(params.getExpression2().toString());
                    expression2writer.close();
                    pFileList.add(expression2);
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }

            pFileList.add(session_prop_file);
        }
    }

    public void restoreSessionState(List<File> pStateFileList) {

        if ((pStateFileList == null) || (pStateFileList.size() == 0)) {
            //No previous state to restore
            return;
        }

        try {
            //go through the prop files first to create the correct objects to be able
            //to add other files to.
            for(int i = 0; i < pStateFileList.size(); i++){

                File prop_file = pStateFileList.get(i);

                if(prop_file.getName().contains(".props")){

                    TextFileReader reader = new TextFileReader(prop_file.getAbsolutePath());
                    reader.read();
                    String fullText = reader.getText();

                    //Given the file with all the parameters create a new parameter
                    EnrichmentMapParameters params = new EnrichmentMapParameters(fullText);

                    //get the network name
                    String param_name = params.getNetworkName();

                    //get the network name from the file name
                    String[] fullname = prop_file.getName().split("Enrichment_Map_Plugin_");
                    String  props_name = (fullname[1].split("\\."))[0];
                    String name = param_name;

                    //check to see if the network name matches the name of the file
                    //the network name specified in the props file is different from the name of the props
                    //file then assume the name in the props file is wrong and set it to the file name (legacy issue)
                    //related to bug ticket #49
                    if(!props_name.equalsIgnoreCase(param_name)){
                        name = props_name;
                        params.setNetworkName(name);
                    }

                    //register network and parameters
                    EnrichmentMapManager.getInstance().registerNetwork(Cytoscape.getNetwork(name),params);
                }
            }
            //go through the rest of the files
            for(int i = 0; i < pStateFileList.size(); i++){

                File prop_file = pStateFileList.get(i);

                //get the network name and network and parameters
                //for this file
                String[] fullname = prop_file.getName().split("Enrichment_Map_Plugin_");
                String  name = (fullname[1].split("\\."))[0];

                EnrichmentMapParameters params = EnrichmentMapManager.getInstance().getParameters(name);

                if(params == null)
                    System.out.println("network for file" + prop_file.getName() + " does not exist.");
                else if((!prop_file.getName().contains(".props"))
                        && (!prop_file.getName().contains(".expression1.txt"))
                        && (!prop_file.getName().contains(".expression2.txt"))){
                    //read the file
                    TextFileReader reader = new TextFileReader(prop_file.getAbsolutePath());
                    reader.read();
                    String fullText = reader.getText();

                    if(prop_file.getName().contains(".gmt")){
                        params.setGenesetsOfInterest(params.repopulateHashmap(fullText,1));
                    }
                    if(prop_file.getName().contains(".genes.txt")){
                        params.setGenes(params.repopulateHashmap(fullText,2));
                    }
                    if(prop_file.getName().contains(".hashkey2genes.txt")){
                        params.setHashkey2gene(params.repopulateHashmap(fullText,5));
                    }



                    if(prop_file.getName().contains(".ENR1.txt")){
                        if(params.isGSEA())
                            params.setEnrichmentResults1(params.repopulateHashmap(fullText,3));
                        else
                            params.setEnrichmentResults1(params.repopulateHashmap(fullText,4));

                    }
                    if(prop_file.getName().contains(".SubENR1.txt")){
                        if(params.isGSEA())
                            params.setEnrichmentResults1OfInterest(params.repopulateHashmap(fullText,3));
                        else
                            params.setEnrichmentResults1OfInterest(params.repopulateHashmap(fullText,4));
                    }
                    if(prop_file.getName().contains(".RANKS1.txt")){
                        params.setDataset1Rankings(params.repopulateHashmap(fullText,6));
                    }


                    if(params.isTwoDatasets()){
                        if(prop_file.getName().contains(".ENR2.txt")){
                            if(params.isGSEA())
                                params.setEnrichmentResults2(params.repopulateHashmap(fullText,3));
                            else
                                params.setEnrichmentResults2(params.repopulateHashmap(fullText,4));
                        }
                        if(prop_file.getName().contains(".SubENR2.txt")){
                            if(params.isGSEA())
                                params.setEnrichmentResults2OfInterest(params.repopulateHashmap(fullText,3));
                            else
                                params.setEnrichmentResults2OfInterest(params.repopulateHashmap(fullText,4));
                        }
                    }
                    if(prop_file.getName().contains(".RANKS2.txt")){
                        params.setDataset2Rankings(params.repopulateHashmap(fullText,6));
                    }
                }

            }

            //load the expression files.  Load them last because they require
            //info from the parameters
            for(int i = 0; i < pStateFileList.size(); i++){

                File prop_file = pStateFileList.get(i);

                if(prop_file.getName().contains("expression1.txt")){
                    String[] fullname = prop_file.getName().split("Enrichment_Map_Plugin_");
                    String  name = (fullname[1].split("\\."))[0];

                    EnrichmentMapParameters params = EnrichmentMapManager.getInstance().getParameters(name);

                    //Load the GCT file
                    GCTFileReaderTask gctFile1 = new GCTFileReaderTask(params,prop_file.getAbsolutePath(),1);
                    gctFile1.run();
                    params.getExpression().rowNormalizeMatrix();
                }
                if(prop_file.getName().contains("expression2.txt")){
                    //get the network name and network and parameters
                    //for this file
                    String[] fullname = prop_file.getName().split("Enrichment_Map_Plugin_");
                    String  name = (fullname[1].split("\\."))[0];

                    EnrichmentMapParameters params = EnrichmentMapManager.getInstance().getParameters(name);


                    GCTFileReaderTask gctFile2 = new GCTFileReaderTask(params,prop_file.getAbsolutePath(),2);
                    gctFile2.run();
                    params.getExpression2().rowNormalizeMatrix();
                }

            }

            //register the action listeners for all the networks.
            EnrichmentMapManager manager = EnrichmentMapManager.getInstance();
            HashMap networks = manager.getCyNetworkList();

            //interate over the networks
            for(Iterator j = networks.keySet().iterator();j.hasNext();){
                String currentNetwork = (String)j.next();
                CyNetworkView view = Cytoscape.getNetworkView(currentNetwork);
                EnrichmentMapParameters params = (EnrichmentMapParameters)networks.get(currentNetwork);

                //for each map compute the similarity matrix, (easier than storing it)
                //compute the geneset similarities
                ComputeSimilarityTask similarities = new ComputeSimilarityTask(params);
                similarities.run();
                HashMap<String, GenesetSimilarity> similarity_results = similarities.getGeneset_similarities();
                params.setGenesetSimilarity(similarity_results);

                //add the click on edge listener
                view.addGraphViewChangeListener(new EnrichmentMapActionListener(params));

                //set the last network to be the one viewed
                //and initialize the parameters panel
                if(!j.hasNext()){
                    Cytoscape.setCurrentNetwork(currentNetwork);
                    ParametersPanel paramPanel = manager.getParameterPanel();
                    paramPanel.updatePanel(params);
                    paramPanel.revalidate();
                }

            }

        } catch (Exception ee) {
            ee.printStackTrace();
        }

    }

    private Properties getPropertiesFromClasspath(String propFileName) throws IOException {
        // loading properties file from the classpath
        Properties props = new Properties();
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(propFileName);

        if (inputStream == null) {
            throw new FileNotFoundException("property file '" + propFileName
                    + "' not found in the classpath");
        }

        props.load(inputStream);
        return props;
    }


}
