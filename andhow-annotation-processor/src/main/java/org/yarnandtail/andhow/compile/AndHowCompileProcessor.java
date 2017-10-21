package org.yarnandtail.andhow.compile;

import org.yarnandtail.andhow.service.PropertyRegistrationList;
import org.yarnandtail.andhow.service.PropertyRegistration;
import com.sun.source.util.Trees;
import java.io.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.element.*;


import javax.tools.FileObject;
import org.yarnandtail.andhow.api.Property;

import static javax.tools.StandardLocation.CLASS_OUTPUT;

/**
 *
 * Note: check to ensure that Props are not referenced in static init blocks b/c
 * we may need to load the class (and run its init) before andHow init can
 * complete, causing a circular init loop.
 *
 * @author ericeverman
 */
@SupportedAnnotationTypes("*")
public class AndHowCompileProcessor extends AbstractProcessor {

//	public static final String GENERATED_CLASS_PREFIX = "$GlobalPropGrpStub";
//	public static final String GENERATED_CLASS_NESTED_SEP = "$";
//
	private static final String SERVICES_PACKAGE = "";
	
	private static final String RELATIVE_NAME = "META-INF/services/org.yarnandtail.andhow.service.PropertyRegistrar";

	//Static to insure all generated classes have the same timestamp
	private static Calendar runDate;

	private Trees trees;
	
	private List<String> registrars;		//List of registrars, one per registeredTLC
	private List<Element> registeredTLCs;	//Top level classes containing Properties

	public AndHowCompileProcessor() {
		//required by Processor API
		runDate = new GregorianCalendar();
	}
	
	protected void addRegistrar(String fullClassName, Element causeElement) {
		if (registrars == null) {
			registrars = new ArrayList();
			registeredTLCs = new ArrayList();
		}
		
		registrars.add(fullClassName);
		registeredTLCs.add(causeElement);
	}


	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

		boolean isLastRound = roundEnv.processingOver();
		
		Filer filer = this.processingEnv.getFiler();


		StringBuilder existingServiceFileContent = new StringBuilder();
		StringBuilder newServiceFileContent = new StringBuilder();

		try {
			
			if (isLastRound) {
				trace("(THIS IS THE LAST ROUND OF PROCESSING.  Root Element count: " + roundEnv.getRootElements().size());
				
				if (registrars != null && registrars.size() > 0) {
					writeServiceRegistrarsFile(filer, registrars, registeredTLCs.toArray(new Element[registeredTLCs.size()]));
				}
				
			} else {
				trace("(Just another round of processing...  Root Element count: " + roundEnv.getRootElements().size());
			}

		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}

		//
		//Scan all the Compilation units (i.e. class files) for AndHow Properties
		Iterator<? extends Element> it = roundEnv.getRootElements().iterator();
		for (Element e : roundEnv.getRootElements()) {


			TypeElement te = (TypeElement) e;
			AndHowElementScanner7 st = new AndHowElementScanner7(this.processingEnv, Property.class.getCanonicalName());
			CompileUnit ret = st.scan(e);
			
			

			if (ret.hasRegistrations()) {
				
				trace("Found " + ret.getRegistrations().size() + " registration");
				PropertyRegistrarClassGenerator gen = new PropertyRegistrarClassGenerator(ret, AndHowCompileProcessor.class, runDate);
				this.addRegistrar(gen.buildGeneratedClassFullName(), te);
				PropertyRegistrationList regs = ret.getRegistrations();
				
				trace("Found " + ret.getRegistrations().size() + " registration");
				for (PropertyRegistration p : ret.getRegistrations()) {
					trace("Found Property '" + p.getCanonicalPropertyName() + 
							"' in : " + p.getCanonicalRootName() + " parent class: " + p.getJavaCanonicalParentName());
				}
				
				
				try {
					trace("Will write new generated class file " + gen.buildGeneratedClassSimpleName());
					writeClassFile(filer, gen, e);
				} catch (Exception ex) {
					error("Unable to write generated classfile '" + gen.buildGeneratedClassFullName() + "'", ex);
				}
			}

			for (String err : ret.getErrors()) {
				System.out.println("Found Error: " + err);
			}

		}

		return false;

	}

	public void writeClassFile(Filer filer, PropertyRegistrarClassGenerator generator, Element causingElement) throws Exception {

		trace("Writing " + generator.buildGeneratedClassFullName() + " as a generated source file");
		
		String classContent = generator.generateSource();

		FileObject classFile = filer.createSourceFile(generator.buildGeneratedClassFullName(), causingElement);

		try (Writer writer = classFile.openWriter()) {
			writer.write(classContent);
		}	
	}
	
	public void writeServiceRegistrarsFile(Filer filer, List<String> registrars, Element... causingElements) throws Exception {

		trace("Writing service registrars file");

		//The CLASS_OUTPUT location is used instead of SOURCE_OUTPUT because it
		//seems that non-Java files are not copied over from the SOURCE_OUTPUT
		//location.
		FileObject svsFile = filer.createResource(CLASS_OUTPUT, SERVICES_PACKAGE, RELATIVE_NAME, causingElements);

		try (Writer writer = svsFile.openWriter()) {
			for (String svs : registrars) {
				writer.write(svs);
				writer.write(System.lineSeparator());
			}
		}
	}
		

	public static void trace(String msg) {
		System.out.println("AndHowCompileProcessor: " + msg);
	}

	public static void error(String msg) {
		error(msg, null);
	}

	public static void error(String msg, Exception e) {
		System.err.println("AndHowCompileProcessor: " + msg);
		e.printStackTrace(System.err);
		throw new RuntimeException(msg, e);
	}

}