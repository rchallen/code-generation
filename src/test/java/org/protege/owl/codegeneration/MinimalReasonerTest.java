package org.protege.owl.codegeneration;

import java.io.File;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.testng.Assert;
import org.testng.annotations.Test;

import uk.ac.manchester.cs.jfact.JFactFactory;

public class MinimalReasonerTest {

	public static void main(String[] args) throws OWLOntologyCreationException {
		new MinimalReasonerTest().checkConsistency();
	}
	
	@Test
	public void checkConsistency() throws OWLOntologyCreationException {
		
		File[] ontFiles = new File[] {
			new File(MinimalReasonerTest.class.getResource("/CodeGeneration001.owl").getFile()),
			new File(MinimalReasonerTest.class.getResource("/CodeGeneration002.owl").getFile()),
			new File(MinimalReasonerTest.class.getResource("/pizza.owl").getFile())
		};
		
		//OWLOntologyFactory factory = new OWLOntologyFactory();
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		
		for (File file: ontFiles) {
			
			System.out.println("Testing consistency of: "+file.getAbsolutePath());
			OWLOntology ont = manager.loadOntologyFromOntologyDocument(file);
			OWLReasoner r = new JFactFactory().createReasoner(ont);//.createNonBufferingReasoner(ont);
			Assert.assertTrue(r.isConsistent());
			
		}
	}
}
