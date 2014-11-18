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

public class SimpleRuntimeInference implements RuntimeInference {

	static final Logger LOGGER = Logger.getLogger(ReasonerBasedInference.class);
	ClassMap map;
	
	private static class ClassMap {
		
		HashMap<OWLClass, ClassMapEntry> entries = new HashMap<OWLClass, ClassMapEntry>();
		
		public ClassMap(OWLOntology ontology) {
			for (OWLClass owlClass: ontology.getClassesInSignature(true)) {
				entries.put(owlClass, new ClassMapEntry(owlClass,ontology));
			}
			for (Map.Entry<OWLClass, ClassMapEntry> entry: entries.entrySet()) {
				for (OWLClass parent: getSuperClasses(entry.getKey(), ontology)) {
					entry.getValue().addParent(entries.get(parent));
				}
			}
		}
		
		public ClassMapEntry getEntry(OWLClass c) {return entries.get(c);}
	}
	
	private static class ClassMapEntry {
		
		OWLClass owlClass;
		List<ClassMapEntry> parents = new ArrayList<ClassMapEntry>();
		List<ClassMapEntry> children = new ArrayList<ClassMapEntry>();
		
		public ClassMapEntry(OWLClass owlClass, OWLOntology ontology) {
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
	
	public SimpleRuntimeInference(OWLOntology ontology) {
		this.ontology = ontology;
		this.map = new ClassMap(ontology);  
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
	    for (OWLClass type : types) {
	        if (map.getEntry(type).isSubtype(c)) {
	            return true;
	        }
	    }
	    if (System.currentTimeMillis()-time>10) LOGGER.debug("REASN: "+(System.currentTimeMillis()-time)+" ms for reasoner canAs: "+c.toStringID()+" on "+i.toStringID());
		return false;
	}
	
	@Override
	public Collection<OWLNamedIndividual> getPropertyValues(OWLNamedIndividual i, OWLObjectProperty p) {
		long time = System.currentTimeMillis();
		Collection<OWLNamedIndividual> results = new HashSet<OWLNamedIndividual>();
	    for (OWLOntology imported : ontology.getImportsClosure()) {
	        for (OWLIndividual j : i.getObjectPropertyValues(p, imported)) {
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
            results.addAll(i.getDataPropertyValues(p, imported));
        }
        if (System.currentTimeMillis()-time>10) LOGGER.debug("REASN: "+(System.currentTimeMillis()-time)+" ms for reasoner getDataPropertyValues: "+p.toStringID()+" on "+i.toStringID());
		return results;
	}

	@Override
	public Collection<OWLNamedIndividual> getIndividuals(OWLClass owlClass) {
		long time = System.currentTimeMillis();
		Set<OWLNamedIndividual> individuals = new HashSet<OWLNamedIndividual>();
		for (OWLClass subtype: map.getEntry(owlClass).getSubtypes()) {
		for (OWLIndividual i : subtype.getIndividuals(ontology)) {
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
		for (OWLClassExpression ce : i.getTypes(ontology.getImportsClosure())) {
			if (!ce.isAnonymous()) {
				types.add(ce.asOWLClass());
			}
		}
		if (System.currentTimeMillis()-time>10) LOGGER.debug("REASN: "+(System.currentTimeMillis()-time)+" ms for reasoner getTypes: "+i.toStringID());
		return types;
	}
	
	
	//=======================
	
	private static Collection<OWLClass> getSuperClasses(OWLClass owlClass, OWLOntology ontology) {
		Set<OWLClass> superClasses = new HashSet<OWLClass>();
		for (OWLClassExpression ce : owlClass.getSuperClasses(ontology.getImportsClosure())) {
			if (!ce.isAnonymous()) {
				superClasses.add(ce.asOWLClass());
			}
			else if (ce instanceof OWLObjectIntersectionOf) {
			    superClasses.addAll(getNamedConjuncts((OWLObjectIntersectionOf) ce, ontology));
			}
		}
		for (OWLClassExpression ce : owlClass.getEquivalentClasses(ontology.getImportsClosure())) {
		    if (ce instanceof OWLObjectIntersectionOf) {
                superClasses.addAll(getNamedConjuncts((OWLObjectIntersectionOf) ce, ontology));
            }
		}
		superClasses.remove(ontology.getOWLOntologyManager().getOWLDataFactory().getOWLThing());
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
