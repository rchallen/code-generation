package org.protege.owl.codegeneration.names;

import java.util.Set;

import org.protege.owl.codegeneration.CodeGenerationOptions;
import org.protege.owl.codegeneration.Constants;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;

public class IriNames extends AbstractCodeGenerationNames {
	private OWLOntology ontology;
	private ShortFormProvider provider;

	public IriNames(OWLOntology ontology, CodeGenerationOptions options) {
		super(options);
		this.ontology = ontology;
		provider = new SimpleShortFormProvider();
	}
	
	
	
	public String getInterfaceName(OWLClass owlClass) {
		String name = 
				(getJavaname(owlClass) != null) ?
						getJavaname(owlClass) :
						provider.getShortForm(owlClass);
		name = NamingUtilities.convertToJavaIdentifier(name);
		name = NamingUtilities.convertInitialLetterToUpperCase(name);
		return name;
	}
	
	public String getClassName(OWLClass owlClass) {
		String name = 
				(getJavaname(owlClass) != null) ?
						getJavaname(owlClass) :
						provider.getShortForm(owlClass);
		name = NamingUtilities.convertToJavaIdentifier(name);
		return name;
	}

	
	public String getObjectPropertyName(OWLObjectProperty owlObjectProperty) {
		String name = 
				(getJavaname(owlObjectProperty) != null) ?
						getJavaname(owlObjectProperty) :
						provider.getShortForm(owlObjectProperty);
		name = NamingUtilities.convertToJavaIdentifier(name);
		return name;
	}
	
	
	public String getDataPropertyName(OWLDataProperty owlDataProperty) {
		String name = 
				(getJavaname(owlDataProperty) != null) ?
						getJavaname(owlDataProperty) :
						provider.getShortForm(owlDataProperty);
		name = NamingUtilities.convertToJavaIdentifier(name);
		return name;
	}
	
	private String getJavaname(OWLEntity e) {
	    StringBuffer sb = new StringBuffer();
	    Set<OWLAnnotation> annotations = e.getAnnotations(ontology, Constants.JAVANAME);
	    if (annotations.size() == 1) {
	        OWLAnnotation javanameAnnotation = annotations.iterator().next();
	        if (javanameAnnotation.getValue() instanceof OWLLiteral) {
	            sb.append(((OWLLiteral) javanameAnnotation.getValue()).getLiteral());
	        }
	        return sb.toString().trim();
	    }
	    return null;
	}
}
