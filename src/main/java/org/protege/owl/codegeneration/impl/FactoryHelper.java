package org.protege.owl.codegeneration.impl;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.protege.owl.codegeneration.CodeGenerationRuntimeException;
import org.protege.owl.codegeneration.inference.CodeGenerationInference;
import org.protege.owl.codegeneration.inference.ReasonerBasedInference;
import org.protege.owl.codegeneration.inference.RuntimeInference;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class FactoryHelper {
	
	public static final Logger LOGGER = Logger.getLogger(FactoryHelper.class);

	private OWLOntology owlOntology;
	private OWLOntologyManager manager;
	private OWLDataFactory owlDataFactory;
	private RuntimeInference inference;
	
	public FactoryHelper(OWLOntology ontology, RuntimeInference inference) {
		this.owlOntology = ontology;
		this.inference = inference;
		manager = ontology.getOWLOntologyManager();
		owlDataFactory = manager.getOWLDataFactory();
	}
	
	public void flushOwlReasoner() {
		if (inference instanceof CodeGenerationInference)
			((CodeGenerationInference) inference).flush();
	}
	
	
	public <X extends WrappedIndividualImpl> X createWrappedIndividual(String name, OWLClass type, Class<X> c) {
		OWLNamedIndividual i = owlDataFactory.getOWLNamedIndividual(IRI.create(name));
		manager.addAxiom(owlOntology, owlDataFactory.getOWLClassAssertionAxiom(type, i));
		return getWrappedIndividual(name, c);
	}
	
	public <X extends WrappedIndividualImpl> X getWrappedIndividual(String name, OWLClass type, Class<X> c) {
		IRI iri = IRI.create(name);
		OWLNamedIndividual i = owlDataFactory.getOWLNamedIndividual(iri);
		if (!inference.canAs(i, type)) {
			LOGGER.debug("IRI: "+name+" cannot be cast to a "+type.getIRI());
			return null;
		}
		return getWrappedIndividual(name, c);
	}
	
	private <X extends WrappedIndividualImpl> X getWrappedIndividual(String name, Class<X> c) {
		try {
    		Constructor<X> constructor = c.getConstructor(RuntimeInference.class, IRI.class);
    		return constructor.newInstance(inference, IRI.create(name));
		}
		catch (Exception e) {
			throw new CodeGenerationRuntimeException(e);
		}
	}
	
	public <X extends WrappedIndividualImpl> Collection<X> getWrappedIndividuals(OWLClass owlClass, Class<X> c) {
		Set<X> wrappers = new HashSet<X>();
		for (OWLNamedIndividual i : inference.getIndividuals(owlClass)) {
			wrappers.add(getWrappedIndividual(i.getIRI().toString(), c));
		}
		return wrappers;
	}
	
}
