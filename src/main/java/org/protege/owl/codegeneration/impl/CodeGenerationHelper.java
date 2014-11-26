package org.protege.owl.codegeneration.impl;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.protege.owl.codegeneration.CodeGenerationRuntimeException;
import org.protege.owl.codegeneration.HandledDatatypes;
import org.protege.owl.codegeneration.WrappedIndividual;
import org.protege.owl.codegeneration.inference.RuntimeInference;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class CodeGenerationHelper {
    private OWLOntology owlOntology;    
    private OWLDataFactory owlDataFactory;
    private OWLOntologyManager manager;
    private RuntimeInference inference;
    
    
    public CodeGenerationHelper(RuntimeInference inference) {
        this.inference = inference;
        this.owlOntology = inference.getOWLOntology();        
        manager = owlOntology.getOWLOntologyManager();
        owlDataFactory = manager.getOWLDataFactory();
    }
    
    public OWLOntology getOwlOntology() {
        return owlOntology;
    }
    
    public <X> Collection<X> getPropertyValues(OWLNamedIndividual i, OWLObjectProperty p, Class<X> c) {
        try {
            Constructor<X> constructor = c.getConstructor(RuntimeInference.class, IRI.class);
            Set<X> results = new HashSet<X>();
            for (OWLNamedIndividual j : inference.getPropertyValues(i, p)) {
                results.add(constructor.newInstance(inference, j.getIRI()));
            }
            return results;
        }
        catch (Exception e) {
            throw new CodeGenerationRuntimeException(e);
        }
    }
 
    public void setPropertyValue(OWLNamedIndividual i, OWLObjectProperty p, WrappedIndividual j, OWLObjectProperty ep) {
    	for (OWLNamedIndividual existing : inference.getPropertyValues(i, p)) {
            removePropertyValue(i,p,existing,ep);
        }
    	if (j != null) addPropertyValue(i,p,j,ep);
    }
    
    public void addPropertyValue(OWLNamedIndividual i, OWLObjectProperty p, WrappedIndividual j, OWLObjectProperty ep) {
    	OWLAxiom axiom = owlDataFactory.getOWLObjectPropertyAssertionAxiom(p, i, j.getOwlIndividual());
    	manager.addAxiom(owlOntology, axiom);
    	if (ep != null) {
    		OWLAxiom axiom2 = owlDataFactory.getOWLObjectPropertyAssertionAxiom(ep, j.getOwlIndividual(), i);
    		manager.addAxiom(owlOntology, axiom2);
    	}
    	//TODO: we should be checking the reasoner says this is consistent, shoudln't we?
    }
    
    public void removePropertyValue(OWLNamedIndividual i, OWLObjectProperty p, WrappedIndividual j, OWLObjectProperty ep) {
    	removePropertyValue(i,p,j.getOwlIndividual(),ep);
    }
    
    public void removePropertyValue(OWLNamedIndividual i, OWLObjectProperty p, OWLNamedIndividual j, OWLObjectProperty ep) {
    	OWLAxiom axiom = owlDataFactory.getOWLObjectPropertyAssertionAxiom(p, i, j);
    	for (OWLOntology imported : owlOntology.getImportsClosure()) {
        	manager.removeAxiom(imported, axiom);
    	}
    	if (ep != null) {
    		OWLAxiom axiom2 = owlDataFactory.getOWLObjectPropertyAssertionAxiom(ep, j, i);
        	for (OWLOntology imported : owlOntology.getImportsClosure()) {
            	manager.removeAxiom(imported, axiom2);
        	}   
    	}
    }
    
    public <X> Collection<X> getPropertyValues(OWLNamedIndividual i, OWLDataProperty p, Class<X> c) {
        Set<X> results = new HashSet<X>();
        for (OWLLiteral l : inference.getPropertyValues(i, p)) {
            results.add(c.cast(getObjectFromLiteral(l)));
        }
        return results;
    }
    
    public void setPropertyValue(OWLNamedIndividual i, OWLDataProperty p, Object o) {
    	for (OWLLiteral l : inference.getPropertyValues(i, p)) {
            removePropertyValue(i,p,l);
        }
    	if (o != null) addPropertyValue(i,p,o);
    }
    
    public void addPropertyValue(OWLNamedIndividual i, OWLDataProperty p, Object o) {
    	OWLLiteral literal = getLiteralFromObject(owlDataFactory, o);
    	if (literal != null) {
    		OWLAxiom axiom = owlDataFactory.getOWLDataPropertyAssertionAxiom(p, i, literal);
    		manager.addAxiom(owlOntology, axiom);
    	}
    	else {
    		throw new CodeGenerationRuntimeException("Invalid type for property value object " + o);
    	}
    }
    
    public void removePropertyValue(OWLNamedIndividual i, OWLDataProperty p, Object o) {
    	OWLLiteral literal = getLiteralFromObject(owlDataFactory, o);
    	if (literal != null) {
    		removePropertyValue(i,p,literal);
    	} else {
    		throw new CodeGenerationRuntimeException("Invalid type for property value object " + literal.toString());
    	}
    }

    public void removePropertyValue(OWLNamedIndividual i, OWLDataProperty p, OWLLiteral literal) {
    	OWLAxiom axiom = owlDataFactory.getOWLDataPropertyAssertionAxiom(p, i, literal);
    	manager.removeAxiom(owlOntology, axiom);
    }
    
    public static Object getObjectFromLiteral(OWLLiteral literal) {
    	Object o = null;
    	for (HandledDatatypes handled : HandledDatatypes.values()) {
    		if (handled.isMatch(literal.getDatatype())) {
    			o = handled.getObject(literal);
    			break;
    		}
    	}
    	if (o == null) {
    		o = literal;
    	}
    	return o;
    }
    
    public static OWLLiteral getLiteralFromObject(OWLDataFactory owlDataFactory, Object o) {
    	OWLLiteral literal = null;
    	if (o instanceof OWLLiteral) {
    		literal = (OWLLiteral) o;
    	}
    	else {
    		for (HandledDatatypes handled : HandledDatatypes.values()) {
    			literal = handled.getLiteral(owlDataFactory, o);
    			if (literal != null) {
    				break;
    			}
    		}
    	}
    	return literal;
    }
    
}
