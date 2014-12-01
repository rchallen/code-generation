package org.protege.owl.codegeneration.inference;

import java.util.Collection;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;

public interface RuntimeInference {
	
	OWLOntology getOWLOntology();
	
	boolean canAs(OWLNamedIndividual i, OWLClass c);

	Collection<OWLNamedIndividual> getPropertyValues(OWLNamedIndividual i, OWLObjectProperty p);
	
    Collection<OWLLiteral> getPropertyValues(OWLNamedIndividual i, OWLDataProperty p);

	Collection<OWLNamedIndividual> getIndividuals(OWLClass owlClass);
	
	Collection<OWLClass> getTypes(OWLNamedIndividual i);
}
