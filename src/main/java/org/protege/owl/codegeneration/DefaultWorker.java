package org.protege.owl.codegeneration;

import static org.protege.owl.codegeneration.SubstitutionVariable.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.protege.owl.codegeneration.inference.CodeGenerationInference;
import org.protege.owl.codegeneration.inference.SimpleInference;
import org.protege.owl.codegeneration.names.CodeGenerationNames;
import org.protege.owl.codegeneration.names.NamingUtilities;
import org.protege.owl.codegeneration.property.JavaPropertyDeclarationCache;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLRuntimeException;
import org.semanticweb.owlapi.search.EntitySearcher;

public class DefaultWorker implements Worker {
	private HashMap<String, String> templateMap = new HashMap<String, String>();
	private OWLOntology owlOntology;
	private CodeGenerationOptions options;
	private CodeGenerationNames names;
    private CodeGenerationInference inference;
    private JavaPropertyDeclarationCache propertyDeclarations;
    
    public static void generateCode(OWLOntology ontology, CodeGenerationOptions options, CodeGenerationNames names) throws IOException {
    	generateCode(ontology, options, names, new SimpleInference(ontology));
    }
    
    public static void generateCode(OWLOntology ontology, CodeGenerationOptions options, CodeGenerationNames names, CodeGenerationInference inference) throws IOException {
    	Worker worker = new DefaultWorker(ontology, options, names, inference);
    	JavaCodeGenerator generator = new JavaCodeGenerator(worker);
    	generator.createAll();
    }    
	
    
    public DefaultWorker(OWLOntology ontology, 
    		             CodeGenerationOptions options,
    		             CodeGenerationNames names,
			             CodeGenerationInference inference) {
		this.owlOntology = ontology;
		this.options = options;
		this.names = names;
		this.inference = inference;
		propertyDeclarations = new JavaPropertyDeclarationCache(inference, names);
	}

    public OWLOntology getOwlOntology() {
		return owlOntology;
	}
    
    public Collection<OWLClass> getOwlClasses() {
    	return new TreeSet<OWLClass>(inference.getOwlClasses());
    }
    
    public Collection<OWLObjectProperty> getOwlObjectProperties() {
    	return new TreeSet<OWLObjectProperty>(owlOntology.getObjectPropertiesInSignature(true));
    }
    
    public Collection<OWLDataProperty> getOwlDataProperties() {
    	return new TreeSet<OWLDataProperty>(owlOntology.getDataPropertiesInSignature(true));
    }

    public void initialize() {
        File folder = options.getOutputFolder();
        if (folder != null && !folder.exists()) {
            folder.mkdirs();
        }
        String pack = options.getPackage();
        if (pack != null) {
            pack = pack.replace('.', '/');
            File file = folder == null ? new File(pack) : new File(folder, pack);
            file.mkdirs();
            File f = new File(file, "impl");
            f.mkdirs();
        } else {
            File file = folder == null ? new File("impl") : new File(folder, "impl");
            file.mkdirs();
        }
    }
    
    public Collection<OWLObjectProperty> getObjectPropertiesForClass(OWLClass owlClass) {
    	return propertyDeclarations.getObjectPropertiesForClass(owlClass);
    }
    
    public Collection<OWLDataProperty> getDataPropertiesForClass(OWLClass owlClass) {
    	return propertyDeclarations.getDataPropertiesForClass(owlClass);
    }
    
    public File getInterfaceFile(OWLClass owlClass) {
        String interfaceName = names.getInterfaceName(owlClass);
        return getInterfaceFile(interfaceName);
    }
    
    public File getImplementationFile(OWLClass owlClass) {
    	String implName = names.getImplementationName(owlClass);
    	return getImplementationFile(implName);
    }
    
    public File getVocabularyFile() {
    	return getInterfaceFile(Constants.VOCABULARY_CLASS_NAME);
    }
    
    public String getTemplate(CodeGenerationPhase phase, OWLClass owlClass, OWLEntity owlProperty) {
    	
    	String resource = "/" + phase.getTemplateName();
    	if (owlClass != null && owlProperty != null) {
    		resource = resource + ( propertyDeclarations.get(owlClass, owlProperty).isCollection() ? "" : ".single" );
    	}
		
    	String template = templateMap.get(resource);
		if (template == null) {
			try {
				URL u = CodeGenerationOptions.class.getResource(resource);
				Reader reader = new InputStreamReader(u.openStream());
				StringBuffer buffer = new StringBuffer();
				int charsRead;
				char[] characters = new char[1024];
				while (true) {
					charsRead = reader.read(characters);
					if (charsRead < 0) {
						break;
					}
					buffer.append(characters, 0, charsRead);
				}
				template = buffer.toString();
				templateMap.put(resource, template);
				reader.close();
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return template;
    }
	
	public void configureSubstitutions(CodeGenerationPhase phase,
									   Map<SubstitutionVariable, String> substitutions, 
									   OWLClass owlClass,
									   OWLEntity owlProperty) {
		switch (phase) {
		case CREATE_VOCABULARY_HEADER:
		case CREATE_FACTORY_HEADER:
			configureCommonSubstitutions(substitutions, owlClass, owlProperty);
			break;
		case CREATE_CLASS_VOCABULARY:
		case CREATE_FACTORY_CLASS:
			configureClassSubstitutions(substitutions, owlClass);
			break;
		case CREATE_OBJECT_PROPERTY_VOCABULARY:
		case CREATE_DATA_PROPERTY_VOCABULARY:
			configurePropertySubstitutions(substitutions, owlClass, owlProperty);
			break;
		case CREATE_INTERFACE_HEADER:
		case CREATE_IMPLEMENTATION_HEADER:
			configureCommonSubstitutions(substitutions, owlClass, owlProperty);
			configureClassSubstitutions(substitutions, owlClass);
			break;
		case CREATE_DATA_PROPERTY_INTERFACE:
		case CREATE_DATA_PROPERTY_IMPLEMENTATION:
		case CREATE_OBJECT_PROPERTY_INTERFACE:
		case CREATE_OBJECT_PROPERTY_IMPLEMENTATION:
			configureClassSubstitutions(substitutions, owlClass);
			configurePropertySubstitutions(substitutions, owlClass, owlProperty);
	        propertyDeclarations.get(owlClass, owlProperty).configureSubstitutions(substitutions);
			break;
		default:
			break;
			
		}
		
	}

	private void configureCommonSubstitutions(Map<SubstitutionVariable, String> substitutions, 
											  OWLClass owlClass,
											  OWLEntity owlProperty) {
        substitutions.put(PACKAGE, options.getPackage());
        substitutions.put(DATE, new Date().toString());
        substitutions.put(USER, System.getProperty("user.name"));
        substitutions.put(FACTORY_CLASS_NAME, options.getFactoryClassName());
        substitutions.put(FACTORY_PACKAGE_NAME, options.getFactoryPackageName());
    }

	private void configureClassSubstitutions(Map<SubstitutionVariable, String> substitutions, 
											  OWLClass owlClass) {
		String upperCaseClassName = names.getClassName(owlClass).toUpperCase();
        substitutions.put(INTERFACE_NAME, names.getInterfaceName(owlClass));
        substitutions.put(IMPLEMENTATION_NAME, names.getImplementationName(owlClass));
        substitutions.put(JAVADOC, getJavadoc(owlClass));
        substitutions.put(UPPERCASE_CLASS, upperCaseClassName);
        substitutions.put(SubstitutionVariable.VOCABULARY_CLASS, "CLASS_" + upperCaseClassName);
        substitutions.put(CLASS_IRI, owlClass.getIRI().toString());
        substitutions.put(INTERFACE_LIST, getSuperInterfaceList(owlClass));
    }

	private void doAnnotations(Map<SubstitutionVariable, String> substitutions, OWLClass owlClass, OWLEntity owlProperty) {
		boolean nullable = false;
		boolean singleton = false;
		boolean transitive = false;
		if (owlProperty instanceof OWLObjectProperty) {
			nullable = inference.isNullable(owlClass, (OWLObjectProperty) owlProperty);
			singleton = inference.isSingleton(owlClass, (OWLObjectProperty) owlProperty);
			transitive = EntitySearcher.isTransitive(owlProperty.asOWLObjectProperty(),owlOntology.getImportsClosure());
		} else {
			nullable = inference.isNullable(owlClass, (OWLDataProperty) owlProperty);
			singleton = inference.isSingleton(owlClass, (OWLDataProperty) owlProperty);
			transitive = false;
		}
		StringBuilder annot = new StringBuilder();
		
		if (nullable) {
			if (singleton) {
				// 0..1
				// 0..*
			}
		} else {
			annot.append("@NotNull ");
			if (singleton) {
				// 1..1
			} else {
				// 1..*
				annot.append("@Size(min=1) ");
			}
		}
		
		if (transitive) annot.append("@Transitive ");
		
		substitutions.put(PROPERTY_ANNOTATION,annot.toString());
	}
	
	private void configurePropertySubstitutions(Map<SubstitutionVariable, String> substitutions,
				OWLClass owlClass, OWLEntity owlProperty) {
        String propertyName;
        if (owlProperty instanceof OWLObjectProperty) {
            
        	OWLObjectProperty owlObjectProperty = (OWLObjectProperty) owlProperty;
            propertyName = names.getObjectPropertyName(owlObjectProperty);
            
            substitutions.put(SubstitutionVariable.VOCABULARY_PROPERTY, "OBJECT_PROPERTY_" + propertyName.toUpperCase());
            
            if (hasInverse(owlProperty.asOWLObjectProperty())) {
            	OWLObjectProperty inverseProp = getInverse(((OWLObjectProperty) owlProperty));
            	String invName = names.getObjectPropertyName(inverseProp).toUpperCase();
            	substitutions.put(SubstitutionVariable.VOCABULARY_INVERSE, "OBJECT_PROPERTY_" + invName);
            } else {
            	substitutions.put(SubstitutionVariable.VOCABULARY_INVERSE, "null");
            }
            
        } else {
            
        	OWLDataProperty owlDataProperty = owlProperty.asOWLDataProperty();
            propertyName = names.getDataPropertyName(owlDataProperty);
            substitutions.put(SubstitutionVariable.VOCABULARY_PROPERTY, "DATA_PROPERTY_" + propertyName.toUpperCase());
        }
        
        if (owlClass != null) doAnnotations(substitutions, owlClass, owlProperty);
        
        String propertyCapitalized = NamingUtilities.convertInitialLetterToUpperCase(propertyName);
        String propertyUpperCase = propertyName.toUpperCase();
        
        substitutions.put(JAVADOC, getJavadoc(owlProperty));
        substitutions.put(PROPERTY, propertyName);
        substitutions.put(CAPITALIZED_PROPERTY, propertyCapitalized);
        substitutions.put(UPPERCASE_PROPERTY, propertyUpperCase);
        substitutions.put(PROPERTY_IRI, owlProperty.getIRI().toString());
       
    }
	
	public boolean hasInverse(OWLObjectProperty p) {
		return getInverse(p) != null;
	}

	public OWLObjectProperty getInverse(OWLObjectProperty p) {

		Set<OWLInverseObjectPropertiesAxiom> setInverseAxiom = owlOntology.getInverseObjectPropertyAxioms(p);
			for(OWLInverseObjectPropertiesAxiom inverseAxiom : setInverseAxiom)	{
				//System.out.print(inverseAxiom);
				try {
					if (inverseAxiom.getFirstProperty().asOWLObjectProperty().getIRI().equals(p.getIRI())) {
						return inverseAxiom.getSecondProperty().asOWLObjectProperty();
					} else {
						return inverseAxiom.getFirstProperty().asOWLObjectProperty();
					}
				} catch (OWLRuntimeException e) {
					//Something wasn't named.
				}
			}
		return null;
	}

	
	/* ******************************************************************************
	 * 
	 */
	private File getInterfaceFile(String name) {
	    String pack = options.getPackage();
	    if (pack != null) {
	        pack = pack.replace('.', '/') + "/";
	    } else {
	        pack = "";
	    }
	    return new File(options.getOutputFolder(), pack + name + ".java");
	}
	
	public File getFactoryFile() {
	    return new File(options.getOutputFolder(),
	    		options.getFactoryFqn().replace('.', '/')+".java");
	}

	private String getSuperInterfaceList(OWLClass owlClass) {
	    String base = getBaseInterface(owlClass);
	    if (base == null) {
	    	return Constants.UKNOWN_CODE_GENERATED_INTERFACE;
	    }
	    else {
	    	return base;
	    }
	}
	
    /** Returns base interface of the provided OWLClass
     * @param owlClass The OWLClass whose base interface is to be returned
     * @return
     */
    private String getBaseInterface(OWLClass owlClass) {
        String baseInterfaceString = "";
        for (OWLClass superClass : inference.getSuperClasses(owlClass)) {
            if (inference.getOwlClasses().contains(superClass)) {
                baseInterfaceString += (baseInterfaceString.equals("") ? "" : ", ") + names.getInterfaceName(superClass);
            }
        }
        if (baseInterfaceString.equals("")) {
            return null;
        } else {
            return baseInterfaceString;
        }
    }

	private File getImplementationFile(String implName) {
	    String pack = options.getPackage();
	    if (pack != null) {
	        pack = pack.replace('.', '/') + "/";
	    } else {
	        pack = "";
	    }
	    return new File(options.getOutputFolder(), pack + "impl/" + implName + ".java");
	}
	
	private String getJavadoc(OWLEntity e) {
	    StringBuffer sb = new StringBuffer();
	    Collection<OWLAnnotation> annotations = EntitySearcher.getAnnotations(e, owlOntology, Constants.JAVADOC);
	    if (annotations.size() == 1) {
	        OWLAnnotation javadocAnnotation = annotations.iterator().next();
	        if (javadocAnnotation.getValue() instanceof OWLLiteral) {
	            sb.append('\n');
	            sb.append(((OWLLiteral) javadocAnnotation.getValue()).getLiteral());
	        }
	    }
	    return sb.toString();
	}


}
