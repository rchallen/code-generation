package org.protege.owl.codegeneration.inference;

import java.util.Set;

import org.protege.owl.codegeneration.Constants;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationObjectVisitor;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;

public class Ignored implements OWLAnnotationObjectVisitor {
	boolean ignore = false;
	public boolean getIgnore() {return ignore;}
	public void visit(OWLAnonymousIndividual individual) {}
	@Override
	public void visit(IRI iri) {}
	@Override
	public void visit(OWLLiteral literal) {}
	@Override
	public void visit(OWLAnnotation node) {
		if (node.getProperty().equals(Constants.IGNORE)) {
			OWLLiteral c = (OWLLiteral) node.getValue();
			ignore = Boolean.parseBoolean(c.getLiteral());
		}    	        
	}
	@Override
	public void visit(OWLAnnotationAssertionAxiom axiom) {}
	@Override
	public void visit(OWLAnnotationPropertyDomainAxiom axiom) {}
	@Override
	public void visit(OWLAnnotationPropertyRangeAxiom axiom) {}
	@Override
	public void visit(OWLSubAnnotationPropertyOfAxiom axiom) {}
	//public void visit(OWLAnnotationProperty property) {}
	//public void visit(OWLAnnotationValue value) {}
	
	public static boolean ignore(OWLEntity en, OWLOntology ontology) {
		Ignored visitor = new Ignored();
		Set<OWLAnnotation> annotations = en.getAnnotations(ontology);
		for (OWLAnnotation anno : annotations) {
			anno.accept(visitor);
		}
		return visitor.getIgnore();
	}
}