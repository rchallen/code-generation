package org.protege.owl.codegeneration.maven;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.InferredOntologyGenerator;
import org.semanticweb.owlapi.util.OWLOntologyMerger;

import uk.ac.manchester.cs.jfact.JFactFactory;

@Mojo( name = "owl-inferencing", defaultPhase = LifecyclePhase.GENERATE_RESOURCES )
public class InferencingMojo extends AbstractMojo {

	/**
	 * The input file.
	 */
	@Parameter(required=true)
	private File inputOntologyFile;

	/**
	 * Ontology imports.
	 * <imports>
	 * 		<param>file_1.owl</param>
	 * 		<param>file_2.owl</param>
	 * </imports>
	 */
	@Parameter
	private File[] imports;

	@Parameter(defaultValue = "false")
	private boolean merge;
	
	/**
	 * The output file.
	 */
	@Parameter(required=true)
	private File outputOntologyFile;


	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		try {
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();
			config.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
			if (imports != null) {
				for (File imported: imports) {
					manager.loadOntologyFromOntologyDocument(
							new FileDocumentSource(imported),config);
				}
			}
			config.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.THROW_EXCEPTION);
			OWLOntology schema = manager.loadOntologyFromOntologyDocument(
					new FileDocumentSource(inputOntologyFile),config);
			

			OWLReasoner r = new JFactFactory().createNonBufferingReasoner(schema);

			InferredOntologyGenerator inference = new InferredOntologyGenerator(r);
			//inference.addGenerator(new InferredClassAssertionAxiomGenerator());
			//inference.addGenerator(new InferredDataPropertyCharacteristicAxiomGenerator());
			//inference.addGenerator(new InferredDisjointClassesAxiomGenerator());
			//inference.addGenerator(new InferredEquivalentClassAxiomGenerator());
			//inference.addGenerator(new InferredEquivalentDataPropertiesAxiomGenerator());
			//inference.addGenerator(new InferredEquivalentObjectPropertyAxiomGenerator());
			//inference.addGenerator(new InferredInverseObjectPropertiesAxiomGenerator());
			//inference.addGenerator(new InferredObjectPropertyCharacteristicAxiomGenerator());
			//inference.addGenerator(new InferredPropertyAssertionGenerator());
			//inference.addGenerator(new InferredSubClassAxiomGenerator());
			//inference.addGenerator(new InferredSubDataPropertyAxiomGenerator());
			//inference.addGenerator(new InferredSubObjectPropertyAxiomGenerator());
			inference.fillOntology(manager.getOWLDataFactory(), schema);
			outputOntologyFile.getParentFile().mkdirs();
			if (merge) {
				OWLOntologyMerger merger = new OWLOntologyMerger(manager);
				manager.saveOntology(merger.createMergedOntology(manager, IRI.create(outputOntologyFile)), IRI.create(outputOntologyFile));
			} else {
				manager.saveOntology(schema, IRI.create(outputOntologyFile));
			}

		} catch (Exception e) {
			throw new MojoExecutionException("Inferencing failed: "+e.getMessage(),e);
		}
		this.getLog().info("Inferencing complete.");
	}

}
