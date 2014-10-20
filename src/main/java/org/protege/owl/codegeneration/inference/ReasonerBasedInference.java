package org.protege.owl.codegeneration.inference;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.protege.owl.codegeneration.Constants;
import org.protege.owl.codegeneration.HandledDatatypes;
import org.protege.owl.codegeneration.names.CodeGenerationNames;
import org.protege.owl.codegeneration.property.JavaDataPropertyDeclarations;
import org.protege.owl.codegeneration.property.JavaObjectPropertyDeclarations;
import org.protege.owl.codegeneration.property.JavaPropertyDeclarations;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationObjectVisitor;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEntityVisitor;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLProperty;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

public class ReasonerBasedInference implements CodeGenerationInference {
	public static final Logger LOGGER = Logger.getLogger(ReasonerBasedInference.class);

	private OWLOntology ontology;
	private OWLReasoner reasoner;
	private OWLDataFactory factory;
	private Set<OWLClass> allClasses;
	private Map<OWLClass, Set<OWLEntity>> domainMap;
	private Map<OWLClass, Map<OWLObjectProperty, OWLClass>> objectRangeMap = new HashMap<OWLClass, Map<OWLObjectProperty, OWLClass>>();
	private Map<OWLClass, Map<OWLDataProperty, OWLDatatype>> dataRangeMap = new HashMap<OWLClass, Map<OWLDataProperty,OWLDatatype>>();


	public ReasonerBasedInference(OWLOntology ontology, OWLReasoner reasoner) {
		this.ontology = ontology;
		this.reasoner = reasoner;
		factory = ontology.getOWLOntologyManager().getOWLDataFactory();
	}

	@Override
	public OWLOntology getOWLOntology() {
		return ontology;
	}

	@Override
	public void preCompute() {
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY, InferenceType.CLASS_ASSERTIONS);
	}

	@Override
	public void flush() {
		reasoner.flush();
	}

	@Override
	public Collection<OWLClass> getOwlClasses() {
		if (allClasses == null) {
			allClasses = new HashSet<OWLClass>(ontology.getClassesInSignature());
			allClasses.removeAll(reasoner.getUnsatisfiableClasses().getEntities());
			allClasses.removeAll(reasoner.getEquivalentClasses(factory.getOWLThing()).getEntities());
		}
		return allClasses;
	}

	public boolean isNullable(OWLClass owlClass, OWLDataProperty owlProperty) {
		OWLClassExpression intersection = factory
				.getOWLObjectIntersectionOf(owlClass, factory.getOWLDataMaxCardinality(0, owlProperty));
		return reasoner.isSatisfiable(intersection);	
	}

	@Override
	public Collection<OWLClass> getSubClasses(OWLClass owlClass) {
		return reasoner.getSubClasses(owlClass, true).getFlattened();
	}

	@Override
	public Collection<OWLClass> getSuperClasses(OWLClass owlClass) {
		return reasoner.getSuperClasses(owlClass, true).getFlattened();
	}

	private static class Ignored implements OWLAnnotationObjectVisitor {
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
		public void visit(OWLAnnotationProperty property) {}
		public void visit(OWLAnnotationValue value) {}
	}

	private boolean ignore(OWLEntity en) {
		Ignored visitor = new Ignored();
		Set<OWLAnnotation> annotations = en.getAnnotations(ontology);
		for (OWLAnnotation anno : annotations) {
			anno.accept(visitor);
		}
		return visitor.getIgnore();
	}


	@Override
	public Set<JavaPropertyDeclarations> getJavaPropertyDeclarations(OWLClass cls, CodeGenerationNames names) {

		if (domainMap == null) {
			initializeDomainMap();
		}
		Set<JavaPropertyDeclarations> declarations = new HashSet<JavaPropertyDeclarations>();
		if (!ignore(cls) && domainMap.get(cls) != null) {
			for (OWLEntity p : domainMap.get(cls)) {
				if (!ignore(p)) {
					if (p instanceof OWLObjectProperty) {
						declarations.add(new JavaObjectPropertyDeclarations(this, names, cls, (OWLObjectProperty) p));
					}
					else if (p instanceof OWLDataProperty) {
						declarations.add(new JavaDataPropertyDeclarations(this, cls, (OWLDataProperty) p));
					}
				}
			}
		}
		return declarations;
	}

	@Override
	public OWLClass getRange(OWLObjectProperty p) {
		return getRange(factory.getOWLThing(), p);
	}

	@Override
	public OWLClass getRange(OWLClass owlClass, OWLObjectProperty p) {
		Map<OWLObjectProperty, OWLClass> property2RangeMap = objectRangeMap.get(owlClass);
		if (property2RangeMap == null) {
			property2RangeMap = new HashMap<OWLObjectProperty, OWLClass>();
			objectRangeMap.put(owlClass, property2RangeMap);
		}
		OWLClass cls = property2RangeMap.get(p);
		if (cls == null) {
			OWLClassExpression possibleValues = factory.getOWLObjectSomeValuesFrom(factory.getOWLObjectInverseOf(p), owlClass);
			Collection<OWLClass> classes;
			classes = reasoner.getEquivalentClasses(possibleValues).getEntities();
			if (classes != null && !classes.isEmpty()) {
				cls =  asSingleton(classes, ontology);
			}
			else {
				classes = reasoner.getSuperClasses(possibleValues, true).getFlattened();
				cls = asSingleton(classes, ontology);
			}
			property2RangeMap.put(p, cls);
		}
		return cls;
	}

	@Override
	public OWLDatatype getRange(OWLDataProperty p) {
		return getRange(factory.getOWLThing(), p);
	}

	@Override
	public OWLDatatype getRange(OWLClass owlClass, OWLDataProperty p) {
		Map<OWLDataProperty, OWLDatatype> property2RangeMap = dataRangeMap.get(owlClass);
		if (property2RangeMap == null) {
			property2RangeMap = new HashMap<OWLDataProperty, OWLDatatype>();
			dataRangeMap.put(owlClass, property2RangeMap);
		}
		OWLDatatype range = property2RangeMap.get(p);
		if (range == null) {
			for (HandledDatatypes handled : HandledDatatypes.values()) {
				OWLDatatype dt = factory.getOWLDatatype(handled.getIri());
				OWLClassExpression couldHaveOtherValues = factory.getOWLObjectComplementOf(factory.getOWLDataAllValuesFrom(p, dt));
				OWLClassExpression classCouldHaveOtherValues = factory.getOWLObjectIntersectionOf(owlClass, couldHaveOtherValues);
				if (!reasoner.isSatisfiable(classCouldHaveOtherValues)) {
					range = dt;
					break;
				}
			}
			if (range != null) {
				property2RangeMap.put(p, range);
			}
		}
		return range;
	}

	@Override
	public Collection<OWLNamedIndividual> getIndividuals(OWLClass owlClass) {
		return reasoner.getInstances(owlClass, false).getFlattened();
	}

	@Override
	public boolean canAs(OWLNamedIndividual i, OWLClass c) {
		OWLDataFactory factory = ontology.getOWLOntologyManager().getOWLDataFactory();
		return reasoner.isSatisfiable(factory.getOWLObjectIntersectionOf(c, factory.getOWLObjectOneOf(i)));
	}

	@Override
	public Collection<OWLClass> getTypes(OWLNamedIndividual i) {
		return reasoner.getTypes(i, true).getFlattened();
	}

	@Override
	public Collection<OWLNamedIndividual> getPropertyValues(OWLNamedIndividual i, OWLObjectProperty p) {
		return reasoner.getObjectPropertyValues(i, p).getFlattened();
	}

	@Override
	public Collection<OWLLiteral> getPropertyValues(OWLNamedIndividual i, OWLDataProperty p) {
		Set<OWLLiteral> results = new HashSet<OWLLiteral>();
		results.addAll(reasoner.getDataPropertyValues(i, p));
		// the behavior of getDataPropertyValues is somewhat undefined
		// so make sure that the asserted ones are included.
		for (OWLOntology imported : ontology.getImportsClosure()) {
			results.addAll(i.getDataPropertyValues(p, imported));
		}
		return results;
	}

	/* *-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*
	 * 
	 */
	 private static <X extends OWLEntity> X asSingleton(Collection<X> xs, OWLOntology owlOntology) {
		 X result = null;
		 for (X x : xs) {
			 if (owlOntology.containsEntityInSignature(x, true)) {
				 if (result == null) {
					 result = x;
				 }
				 else {
					 return null;
				 }
			 }
		 }
		 return result;
	 }

	 private void initializeDomainMap() {
		 domainMap = new HashMap<OWLClass, Set<OWLEntity>>();
		 for (OWLObjectProperty p : ontology.getObjectPropertiesInSignature()) {
			 OWLClassExpression mustHavePropertyValue = factory.getOWLObjectSomeValuesFrom(p, factory.getOWLThing());
			 addPropertyToDomainMap(p, mustHavePropertyValue);
		 }
		 for (OWLDataProperty p : ontology.getDataPropertiesInSignature()) {
			 OWLClassExpression mustHavePropertyValue = factory.getOWLDataSomeValuesFrom(p, factory.getTopDatatype());
			 addPropertyToDomainMap(p, mustHavePropertyValue);
		 }
	 }

	 private void addPropertyToDomainMap(OWLEntity p, OWLClassExpression mustHavePropertyValue) {
		 Set<OWLClass> equivalents = reasoner.getEquivalentClasses(mustHavePropertyValue).getEntities();
		 if (!equivalents.isEmpty()) {
			 for (OWLClass domain : equivalents) {
				 addToDomainMap(domain, p);
			 }
		 }
		 else {
			 for (OWLClass domain : reasoner.getSuperClasses(mustHavePropertyValue, true).getFlattened()) {
				 addToDomainMap(domain, p);
			 }
		 }
	 }

	 private void addToDomainMap(OWLClass domain, OWLEntity property) {
		 Set<OWLEntity> properties = domainMap.get(domain);
		 if (properties == null) {
			 properties = new TreeSet<OWLEntity>();
			 domainMap.put(domain, properties);
		 }
		 properties.add(property);
	 }

	 @Override
	 public boolean isNullable(OWLClass owlClass, OWLObjectProperty p) {
		 OWLClassExpression intersection = factory
				 .getOWLObjectIntersectionOf(owlClass, factory.getOWLObjectMaxCardinality(0, p));
		 return reasoner.isSatisfiable(intersection);	
	 }

	 @Override
	 public boolean isSingleton(OWLClass owlClass, OWLObjectProperty p) {
		 OWLClassExpression intersection = factory
				 .getOWLObjectIntersectionOf(owlClass, factory.getOWLObjectMinCardinality(2, p));
		 return !reasoner.isSatisfiable(intersection);
	 }

	 @Override
	 public boolean isSingleton(OWLClass owlClass, OWLDataProperty p) {
		 OWLClassExpression intersection = factory
				 .getOWLObjectIntersectionOf(owlClass, factory.getOWLDataMinCardinality(2, p));
		 return !reasoner.isSatisfiable(intersection);
	 }

}
