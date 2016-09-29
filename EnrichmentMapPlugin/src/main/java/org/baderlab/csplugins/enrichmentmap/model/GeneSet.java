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

package org.baderlab.csplugins.enrichmentmap.model;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;


public class GeneSet {

	private final String name;
	private final String description;
	private final Set<Integer> genes;
	private final Optional<String> source;
	
	public GeneSet(String name, String description, Set<Integer> genes) {
		this.name = name;
		this.description = description;
		this.genes = genes;
		
		String[] name_tokens = name.split("%");
		if(name_tokens.length > 1)
			this.source = Optional.of(name_tokens[1]);
		else 
			this.source = Optional.empty();
	}
	

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public Set<Integer> getGenes() {
		return genes;
	}

	public Optional<String> getSource() {
		return source;
	}
	
	public static GeneSet fromTokens(String[] tokens) {
		String name = tokens[1];
		String description = tokens[2];
		if(tokens.length < 3)
			return new GeneSet(name, description, ImmutableSet.of());
		
		ImmutableSet.Builder<Integer> builder = ImmutableSet.builder();
		for(int i = 3; i < tokens.length; i++) {
			builder.add(Integer.parseInt(tokens[i]));
		}
		return new GeneSet(name, description, builder.build());
	}

	@Override
	public  String toString() {
		StringBuffer geneset = new StringBuffer();
		geneset.append(getName() + "\t" + getDescription() + "\t");
		for(Iterator<Integer> i = getGenes().iterator(); i.hasNext();)
			geneset.append(i.next().toString() + "\t");
		return geneset.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((genes == null) ? 0 : genes.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		GeneSet other = (GeneSet) obj;
		if(description == null) {
			if(other.description != null)
				return false;
		} else if(!description.equals(other.description))
			return false;
		if(genes == null) {
			if(other.genes != null)
				return false;
		} else if(!genes.equals(other.genes))
			return false;
		if(name == null) {
			if(other.name != null)
				return false;
		} else if(!name.equals(other.name))
			return false;
		if(source == null) {
			if(other.source != null)
				return false;
		} else if(!source.equals(other.source))
			return false;
		return true;
	}
	
	


}
