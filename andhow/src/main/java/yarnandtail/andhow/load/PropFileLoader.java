package yarnandtail.andhow.load;

import yarnandtail.andhow.ParsingException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import yarnandtail.andhow.LoaderException;
import java.util.Map.Entry;
import java.util.Properties;
import yarnandtail.andhow.*;
import static yarnandtail.andhow.ReportGenerator.DEFAULT_LINE_WIDTH;
import yarnandtail.andhow.internal.RuntimeDefinition;
import yarnandtail.andhow.property.StrProp;

/**
 *
 * @author eeverman
 */
public class PropFileLoader extends BaseLoader implements ConfigSamplePrinter {

	String specificLoadDescription = null;
	
	@Override
	public LoaderValues load(RuntimeDefinition appConfigDef, List<String> cmdLineArgs,
			ValueMapWithContext existingValues, List<LoaderException> loaderExceptions) throws FatalException {
		
		ArrayList<PropertyValue> values = new ArrayList();
		Properties props = null;
		
		String filePath = existingValues.getEffectiveValue(CONFIG.FILESYSTEM_PATH);

		if (filePath != null) {
			specificLoadDescription = "File at: " + filePath;
			props = loadPropertiesFromFilesystem(new File(filePath), CONFIG.FILESYSTEM_PATH);			
		}
		
		if (props == null && existingValues.getEffectiveValue(CONFIG.EXECUTABLE_RELATIVE_PATH) != null) {
			File relPath = buildExecutableRelativePath(existingValues.getEffectiveValue(CONFIG.EXECUTABLE_RELATIVE_PATH));
			
			specificLoadDescription = "File at: " + filePath;
			
			if (relPath != null) {
				props = loadPropertiesFromFilesystem(relPath, CONFIG.EXECUTABLE_RELATIVE_PATH);
			}
		}
		
		if (props == null && existingValues.getEffectiveValue(CONFIG.CLASSPATH_PATH) != null) {
			
			specificLoadDescription = "File on classpath at: " + existingValues.getEffectiveValue(CONFIG.CLASSPATH_PATH);
			
			props = loadPropertiesFromClasspath(
				existingValues.getEffectiveValue(CONFIG.CLASSPATH_PATH), CONFIG.CLASSPATH_PATH);

		}

		if (props == null) {
			throw new FatalException(null,
				"Expected to find one of the PropFileLoader configuration properties " +
				"pointing to a valid file, but couldn't read any file. ");
		}
		
		for(Entry<Object, Object> entry : props.entrySet()) {
			if (entry.getKey() != null && entry.getValue() != null) {
				String k = entry.getKey().toString();
				String v = entry.getValue().toString();
				
				try {

					attemptToAdd(appConfigDef, values, k, v);

				} catch (ParsingException e) {
					loaderExceptions.add(new LoaderException(e, this, null, specificLoadDescription)
					);
				}
				
				
			}
		}
		
		values.trimToSize();
		
		return new LoaderValues(this, values);
	}
	
	@Override
	public Class<? extends PropertyGroup> getLoaderConfig() {
		return CONFIG.class;
	}
	

	protected Properties loadPropertiesFromFilesystem(File propFile, Property<?> fromPoint) throws FatalException {
		
		if (propFile.exists() && propFile.canRead()) {

			try (FileInputStream in = new FileInputStream(propFile)) {
				return loadPropertiesFromInputStream(in, fromPoint, propFile.getAbsolutePath());
			} catch (IOException e) {
				//this exception from opening the FileInputStream
				//Ignore - non-fatal b/c we can try another
			}
		}

		return null;	
	}
	
	protected Properties loadPropertiesFromClasspath(String classpath, Property<?> fromPoint) throws FatalException {
		
		InputStream inS = PropFileLoader.class.getResourceAsStream(classpath);
		
		return loadPropertiesFromInputStream(inS, fromPoint, classpath);

	}
	
	protected Properties loadPropertiesFromInputStream(InputStream inputStream, Property<?> fromPoint, String fromPath) throws FatalException {

		if (inputStream == null) return null;
		
		try {
			Properties props = new Properties();
			props.load(inputStream);
			return props;
		} catch (Exception e) {

			LoaderException le = new LoaderException(e, this, fromPoint,
					"The properties file at '" + fromPath + 
					"' exists and is accessable, but was unparsable.");

			throw new FatalException(le,
					"Unable to continue w/ configuration loading.  " +
					"Fix the properties file and try again.");
		}
	
	}
	

	protected File buildExecutableRelativePath(String filePath) {
		try {
			String path = PropFileLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
			File jarFile = new File(path);
			File jarDir = jarFile.getParentFile();

			if (jarDir.exists()) {
				return new File(jarDir, filePath);
			} else {
				//LOG.debug("Unable to find a directory containing the running jar file (maybe this is not running from a jar??)");
				return null;
			}
		} catch (Exception e) {
			//LOG.error("Attempting to find the executable directory containing the running jar file caused an exception", e);
			return null;
		}
	}
	
	@Override
	public String getSpecificLoadDescription() {
		return specificLoadDescription;
	}
	
	@Override
	public void printSampleStart(PrintStream out) {
		out.println(TextUtil.repeat("#", DEFAULT_LINE_WIDTH));
		out.println(TextUtil.padRight("# Sample properties file generated by " + 
				AndHow.ANDHOW_NAME + "  " + AndHow.ANDHOW_TAG_LINE + "  ", "#", DEFAULT_LINE_WIDTH));
		out.println(TextUtil.padRight(TextUtil.repeat("#", 50) + "  " + AndHow.ANDHOW_URL + " ", "#", DEFAULT_LINE_WIDTH));
		out.println("# " + ConfigSamplePrinter.REQUIRED_HEADER_TEXT);
	}
	
	private static final String LINE_PREFIX = "# ";
	
	@Override
	public void printPropertyGroupStart(PrintStream out, Class<? extends PropertyGroup> group) {
		out.println();
		out.println();
		out.println(TextUtil.repeat("#", DEFAULT_LINE_WIDTH));
		
		String name = null;
		String desc = null;
		
		GroupInfo groupDesc = group.getAnnotation(GroupInfo.class);
		if (groupDesc != null) {
			name = TextUtil.trimToNull(groupDesc.name());
			desc = TextUtil.trimToNull(groupDesc.desc());
		}
		
		if (name != null || desc != null) {
			if (name != null && desc != null) {
				
				if (! name.endsWith(".")) name = name + ".";
				
				TextUtil.println(out, DEFAULT_LINE_WIDTH, LINE_PREFIX, 
						"Property Group {} - {}  Defined in interface {}", name, desc, group.getCanonicalName());

			} else {
				TextUtil.println(out, LINE_PREFIX + "Property Group {}", group.getCanonicalName());
				TextUtil.println(out, DEFAULT_LINE_WIDTH, LINE_PREFIX, "Description: {}", (name != null)?name:desc);
			}
			
			
		} else {
			TextUtil.println(out, "# Property Group {}", group.getCanonicalName());
		}
		
	}
	
	
	@Override
	public void printProperty(PrintStream out, Class<? extends PropertyGroup> group,
			Property<?> point) throws IllegalArgumentException, IllegalAccessException, SecurityException {
		
		
		String pointFieldName = PropertyGroup.getFieldName(group, point);
		String pointCanondName = PropertyGroup.getCanonicalName(group, point);
		
		out.println();
		TextUtil.println(out, DEFAULT_LINE_WIDTH, LINE_PREFIX, "{} ({}) {}{}", 
				pointFieldName,
				point.getValueType().getDestinationType().getSimpleName(),
				(point.isRequired())?ConfigSamplePrinter.REQUIRED_TEXT:"",
				(TextUtil.trimToNull(point.getShortDescription()) == null)?"":" - " + point.getShortDescription());
		
		if (point.getDefaultValue() != null) {
			out.println("# Default Value: " + point.getDefaultValue());
		}
		
		if (TextUtil.trimToNull(point.getHelpText()) != null) {
			TextUtil.println(out, DEFAULT_LINE_WIDTH, LINE_PREFIX, point.getHelpText());
		}
		
		if (point.getValidators().size() == 1) {
			TextUtil.println(out, DEFAULT_LINE_WIDTH, LINE_PREFIX, VALIDATION_TEXT + " " + point.getValidators().get(0).getTheValueMustDescription());
		}
		
		if (point.getValidators().size() > 1) {
			out.println(LINE_PREFIX + VALIDATION_TEXT + ":");	
			for (Validator v : point.getValidators()) {
				out.println(LINE_PREFIX + "\t- " + v.getTheValueMustDescription());
			}
		}
		
		//print the actual sample line
		if (point.getDefaultValue() != null) {
			TextUtil.println(out, "{} = {}", 
					pointCanondName, 
					point.getDefaultValue());
		} else {
			TextUtil.println(out, "{} = [{}]", 
					pointCanondName, 
					point.getValueType().getDestinationType().getSimpleName());
		}
		
		
	}

	@Override
	public void printPropertyGroupEnd(PrintStream out, Class<? extends PropertyGroup> group) {
	}
	
	@Override
	public void printSampleEnd(PrintStream out) {
		out.println();
		out.println(TextUtil.repeat("#", DEFAULT_LINE_WIDTH));
		out.println(TextUtil.repeat("#", DEFAULT_LINE_WIDTH));
	}

	
	
	//TODO:  WOULD LIKE TO HAVE A REQUIRE-ONE TYPE ConfigGroup
	@GroupInfo(
			name="PropFileLoader Configuration",
			desc= "Configure one of these properties to specify a location to load a properties file from. " +
					"Search order is the order listed below.")
	public static interface CONFIG extends PropertyGroup {
		StrProp FILESYSTEM_PATH = StrProp.builder()
				.setDescription("Local filesystem path to a properties file, as interpreted by a Java File object").build();
		
		StrProp EXECUTABLE_RELATIVE_PATH = StrProp.builder()
				.setDescription("Path relative to the current executable for a properties file.  "
						+ "If running from a jar file, this would be a path relative to that jar. "
						+ "In other contexts, the parent directory may be unpredictable.").build();
		
		StrProp CLASSPATH_PATH = StrProp.builder()
				.setDefault("/andhow.properties")
				.setDescription("Classpath to a properties file as interpreted by a Java Classloader.  "
						+ "This path should start with a slash like this: /org/name/MyProperties.props").build();
	}
	
}
