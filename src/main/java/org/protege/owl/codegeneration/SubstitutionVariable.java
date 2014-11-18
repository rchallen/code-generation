package org.protege.owl.codegeneration;


public enum SubstitutionVariable {
	PACKAGE("package"),
	FACTORY_CLASS_NAME("factoryClass"),
	FACTORY_PACKAGE_NAME("factoryPackage"),
    INTERFACE_LIST("superInterfaces"),
	INTERFACE_NAME("interfaceName"),
	IMPLEMENTATION_NAME("implementationName"),
	CLASS_IRI("classIri"),
    PROPERTY_IRI("propertyIri"),
	CAPITALIZED_CLASS("OwlClass"),
	UPPERCASE_CLASS("OWLClass"),
	VOCABULARY_CLASS("VocabClass"),
	PROPERTY("owlProperty"), 
	CAPITALIZED_PROPERTY("OwlProperty"),
	UPPERCASE_PROPERTY("OWLProperty"),
	VOCABULARY_PROPERTY("VocabProperty"),
	VOCABULARY_INVERSE("VocabInverse"),
	PROPERTY_RANGE("propertyRange"),
	PROPERTY_RANGE_FOR_INTERFACE("propertyRangeForInterface"),
	PROPERTY_RANGE_FOR_CLASS("propertyRangeForClass"),
	PROPERTY_RANGE_IMPLEMENTATION("propertyRangeImplementation"),
	JAVADOC("javadoc"),
	DATE("date"),
	USER("user"),
	PROPERTY_ANNOTATION("atProp");
	
	private String name;
	
	private SubstitutionVariable(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
}
