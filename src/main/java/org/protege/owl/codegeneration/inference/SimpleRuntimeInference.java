package org.protege.owl.codegeneration.inference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLRuntimeException;
import org.semanticweb.owlapi.search.EntitySearcher;

public class SimpleRuntimeInference implements RuntimeInference {

	static final Logger LOGGER = Logger.getLogger(SimpleRuntimeInference.class);
	ClassMap map;

	private static class ClassMap {

		HashMap<OWLClass, ClassMapEntry> entries = new HashMap<OWLClass, ClassMapEntry>();

		public ClassMap(OWLOntologyManager manager) {
			for (OWLOntology allOntology: manager.getOntologies()) {
				for (OWLClass owlClass: allOntology.getClassesInSignature(true)) {
					LOGGER.debug("Caching class hierarchy: "+owlClass.getIRI());
					entries.put(owlClass, new ClassMapEntry(owlClass));
				}
				for (Map.Entry<OWLClass, ClassMapEntry> entry: entries.entrySet()) {
					for (OWLClass parent: getSuperClasses(entry.getKey(), manager)) {
						LOGGER.debug("Caching child: "+entry.getKey().getIRI()+" parent "+parent.getIRI());
						entry.getValue().addParent(entries.get(parent));
					}
				}
			}
		}

		public ClassMapEntry getEntry(OWLClass c) {return entries.get(c);}
	}

	private static class ClassMapEntry {

		OWLClass owlClass;
		List<ClassMapEntry> parents = new ArrayList<ClassMapEntry>();
		List<ClassMapEntry> children = new ArrayList<ClassMapEntry>();

		public ClassMapEntry(OWLClass owlClass) {
			this.owlClass= owlClass;
		}

		public void addParent(ClassMapEntry parent) {
			parents.add(parent);
			parent.children.add(this);
		}

		public boolean isSubtype(OWLClass owlClass) {
			if (this.owlClass.getIRI().equals(owlClass.getIRI())) return true;
			for (ClassMapEntry parent : parents) {
				if (parent.isSubtype(owlClass)) return true;
			}
			return false;
		}

		/*public boolean isSupertype(OWLClass owlClass) {
			if (this.owlClass.getIRI().equals(owlClass.getIRI())) return true;
			for (ClassMapEntry child : children) {
				if (child.isSupertype(owlClass)) return true;
			}
			return false;
		}*/

		public Set<OWLClass> getSubtypes() {
			Set<OWLClass> out = new HashSet<OWLClass>();
			addSubtypes(out);
			return out;
		}

		private void addSubtypes(Set<OWLClass> out) {
			if (!out.contains(owlClass)) {
				out.add(owlClass);
				for (ClassMapEntry child: children) {
					child.addSubtypes(out);
				}
			}
		}
	}

	OWLOntology ontology;
	OWLOntologyManager manager;

	public SimpleRuntimeInference(OWLOntology ontology) {
		this.ontology = ontology;
		this.manager = ontology.getOWLOntologyManager();
		this.map = new ClassMap(manager);  
	}

	@Override
	public OWLOntology getOWLOntology() {
		return ontology;
	}

	@Override
	public boolean canAs(OWLNamedIndividual i, OWLClass c) {
		long time = System.currentTimeMillis();
		Collection<OWLClass> types = getTypes(i);
		if (types.contains(c)) {
			return true;
		}
		StringBuilder typeDebug = new StringBuilder(); 
		for (OWLClass type : types) {
			if (map.getEntry(type).isSubtype(c)) {
				return true;
			}
			typeDebug.append(type.getIRI()+", ");
		}
		if (System.currentTimeMillis()-time>10) LOGGER.debug("REASN: "+(System.currentTimeMillis()-time)+" ms for reasoner canAs: "+c.toStringID()+" on "+i.toStringID());
		LOGGER.warn("Individual: "+i.getIRI()+" has types: ["+typeDebug.toString()+"] but is not a: "+c.getIRI());
		return false;
	}

	@Override
	public Collection<OWLNamedIndividual> getPropertyValues(OWLNamedIndividual i, OWLObjectProperty p) {
		long time = System.currentTimeMillis();
		Collection<OWLNamedIndividual> results = new HashSet<OWLNamedIndividual>();
		for (OWLOntology imported : ontology.getImportsClosure()) {
			for (OWLIndividual j : EntitySearcher.getObjectPropertyValues( i,p, imported)) {
				if (!j.isAnonymous()) {
					results.add(j.asOWLNamedIndividual());
				}
			}
		}
		if (System.currentTimeMillis()-time>10) LOGGER.debug("REASN: "+(System.currentTimeMillis()-time)+" ms for reasoner getObjectPropertyValues: "+p.toStringID()+" on "+i.toStringID());
		return results;
	}

	@Override
	public Collection<OWLLiteral> getPropertyValues(OWLNamedIndividual i, OWLDataProperty p) {
		long time = System.currentTimeMillis();
		Set<OWLLiteral> results = new HashSet<OWLLiteral>();
		for (OWLOntology imported : ontology.getImportsClosure()) {
			results.addAll(EntitySearcher.getDataPropertyValues(i,p, imported));
		}
		if (System.currentTimeMillis()-time>10) LOGGER.debug("REASN: "+(System.currentTimeMillis()-time)+" ms for reasoner getDataPropertyValues: "+p.toStringID()+" on "+i.toStringID());
		return results;
	}

	@Override
	public Collection<OWLNamedIndividual> getIndividuals(OWLClass owlClass) {
		long time = System.currentTimeMillis();
		Set<OWLNamedIndividual> individuals = new HashSet<OWLNamedIndividual>();
		for (OWLClass subtype: map.getEntry(owlClass).getSubtypes()) {
			for (OWLIndividual i : EntitySearcher.getIndividuals(subtype,ontology.getImportsClosure())) {
				if (!i.isAnonymous()) {
					individuals.add(i.asOWLNamedIndividual());
				}
			}
		}
		if (System.currentTimeMillis()-time>10) LOGGER.debug("REASN: "+(System.currentTimeMillis()-time)+" ms for reasoner getIndividuals: "+owlClass.toStringID());
		return individuals;
	}


	public Collection<OWLClass> getTypes(OWLNamedIndividual i) {
		long time = System.currentTimeMillis();
		Set<OWLClass> types = new HashSet<OWLClass>();
		for (OWLClassExpression ce : EntitySearcher.getTypes(i,manager.getOntologies())) {
			try {
				if (!ce.isAnonymous()) { types.add(ce.asOWLClass());}
			} catch (OWLRuntimeException e) {
				LOGGER.debug("Individual "+i.getIRI()+" class expression "+ce.toString());
				//Anonymous types
			}
		}
		if (System.currentTimeMillis()-time>10) LOGGER.debug("REASN: "+(System.currentTimeMillis()-time)+" ms for reasoner getTypes: "+i.toStringID());
		return types;
	}


	//=======================

	private static Collection<OWLClass> getSuperClasses(OWLClass owlClass, OWLOntologyManager manager) {
		Set<OWLClass> superClasses = new HashSet<OWLClass>();
		for (OWLOntology ontology: manager.getOntologies()) {
			for (OWLClassExpression ce : EntitySearcher.getSuperClasses(owlClass, ontology.getImportsClosure())) {
				if (!ce.isAnonymous()) {
					superClasses.add(ce.asOWLClass());
				}
				else if (ce instanceof OWLObjectIntersectionOf) {
					superClasses.addAll(getNamedConjuncts((OWLObjectIntersectionOf) ce, ontology));
				}
			}
			for (OWLClassExpression ce : EntitySearcher.getEquivalentClasses(owlClass, ontology.getImportsClosure())) {
				if (ce instanceof OWLObjectIntersectionOf) {
					superClasses.addAll(getNamedConjuncts((OWLObjectIntersectionOf) ce, ontology));
				}
			}
		}
		superClasses.remove(manager.getOWLDataFactory().getOWLThing());
		superClasses.remove(owlClass);
		return superClasses;
	}

	private static Collection<OWLClass> getNamedConjuncts(OWLObjectIntersectionOf ce, OWLOntology ontology) {
		Set<OWLClass> conjuncts = new HashSet<OWLClass>();
		for (OWLClassExpression conjunct : ce.getOperands()) {
			if (!conjunct.isAnonymous()) {
				conjuncts.add(conjunct.asOWLClass());
			}
		}
		return conjuncts;
	}


}
