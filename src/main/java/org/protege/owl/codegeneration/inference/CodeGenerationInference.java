package org.protege.owl.codegeneration.inference;

import java.util.Collection;
import java.util.Set;

import org.protege.owl.codegeneration.names.CodeGenerationNames;
import org.protege.owl.codegeneration.property.JavaPropertyDeclarations;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;

public interface CodeGenerationInference {
	
	OWLOntology getOWLOntology();
	
	void preCompute();
	
	void flush();

	Collection<OWLClass> getOwlClasses();
		
	Collection<OWLClass> getSubClasses(OWLClass owlClass);
	
	Collection<OWLClass> getSuperClasses(OWLClass owlClass);
	
	Set<JavaPropertyDeclarations> getJavaPropertyDeclarations(OWLClass cls, CodeGenerationNames names);

	OWLClass getRange(OWLObjectProperty p);
	
	OWLClass getRange(OWLClass owlClass, OWLObjectProperty p);
	
	boolean isNullable(OWLClass owlClass, OWLObjectProperty p);
	
	boolean isNullable(OWLClass owlClass, OWLDataProperty p);
	
	boolean isSingleton(OWLClass owlClass, OWLObjectProperty p);
	
	boolean isSingleton(OWLClass owlClass, OWLDataProperty p);
		
	OWLDatatype getRange(OWLDataProperty p);
	
	OWLDatatype getRange(OWLClass owlClass, OWLDataProperty p);

	Collection<OWLNamedIndividual> getIndividuals(OWLClass owlClass);

	boolean canAs(OWLNamedIndividual i, OWLClass c);

	Collection<OWLClass> getTypes(OWLNamedIndividual i);
	
	Collection<OWLNamedIndividual> getPropertyValues(OWLNamedIndividual i, OWLObjectProperty p);
	
    Collection<OWLLiteral> getPropertyValues(OWLNamedIndividual i, OWLDataProperty p);

}
