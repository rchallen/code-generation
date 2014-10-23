package org.protege.owl.codegeneration.property;

import static org.protege.owl.codegeneration.SubstitutionVariable.PROPERTY_RANGE;
import static org.protege.owl.codegeneration.SubstitutionVariable.PROPERTY_RANGE_FOR_CLASS;
import static org.protege.owl.codegeneration.SubstitutionVariable.PROPERTY_RANGE_FOR_INTERFACE;
import static org.protege.owl.codegeneration.SubstitutionVariable.PROPERTY_RANGE_IMPLEMENTATION;

import java.util.Map;

import org.protege.owl.codegeneration.Constants;
import org.protege.owl.codegeneration.SubstitutionVariable;
import org.protege.owl.codegeneration.inference.CodeGenerationInference;
import org.protege.owl.codegeneration.names.CodeGenerationNames;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObjectProperty;

/**
 * This class represents the following java methods that are associated with an OWL object property:
 * <pre>
 *     Collection<? extends ${propertyRange}> get${OwlProperty}();
 *     boolean has${OwlProperty}();
 *     void add${OwlProperty}(${propertyRange} new${OwlProperty});
 *     void remove${OwlProperty}(${propertyRange} old${OwlProperty});
 * </pre>
 * Note that these methods do not get specialized as we move to subclasses.
 * <p/>
 * @author tredmond
 */
public class JavaObjectPropertyDeclarations implements JavaPropertyDeclarations {
	private CodeGenerationInference inference;
	private CodeGenerationNames names;
	private OWLClass owlClass;
	private OWLObjectProperty property;
	
	public JavaObjectPropertyDeclarations(CodeGenerationInference inference, CodeGenerationNames names, OWLClass owlClass, 
			                              OWLObjectProperty property) {
		this.inference = inference;
		this.names     = names;
		this.property  = property;
		this.owlClass = owlClass;
	}
	
	public OWLObjectProperty getOwlProperty() {
		return property;
	}

	public boolean isCollection() {
		return !inference.isSingleton(owlClass, property);
	}
	
	public JavaPropertyDeclarations specializeTo(OWLClass subclass) {
		return this; // no specialization is done...
	}

	public void configureSubstitutions(Map<SubstitutionVariable, String> substitutions) {
		substitutions.put(PROPERTY_RANGE_FOR_INTERFACE, getObjectPropertyRangeForClass(true));
		substitutions.put(PROPERTY_RANGE_FOR_CLASS, getObjectPropertyRangeForClass(false));
        substitutions.put(PROPERTY_RANGE_IMPLEMENTATION, getObjectPropertyRange(false));
        substitutions.put(PROPERTY_RANGE, getObjectPropertyRange(true));
	}

	private String getObjectPropertyRangeForClass(boolean isInterface) {
	    return 
	    		(isCollection() ? "Collection<? extends " : "") +
	    		getObjectPropertyRange(isInterface) +
	    		(isCollection() ? ">" : "");
	}
	
	private String getObjectPropertyRange(boolean isInterface) {
		OWLClass range = inference.getRange(property);
		if (range == null || !inference.getAllOwlClasses().contains(range)) {
			return isInterface ? Constants.UKNOWN_CODE_GENERATED_INTERFACE : Constants.ABSTRACT_CODE_GENERATOR_INDIVIDUAL_CLASS;
		}
		return 
				(isInterface ? names.getInterfaceName(range) : names.getImplementationName(range));
				
	}
}
