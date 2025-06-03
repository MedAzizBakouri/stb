package com.example.demo;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.StringWriter;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.FileInputStream;

public class XmlGenerator {

    private static final String GENERATED_SOURCES_PATH = "../target/generated-sources/jaxb/com/example/demo/generated/v2";
    private static final String COMPILED_CLASSES_PATH = "../target/classes/com/example/demo/generated/v2";
    private static final Logger logger = Logger.getLogger(XmlGenerator.class.getName());
    
    // Add cache declarations
    private static final Map<String, Class<?>> classCache = new HashMap<>();
    private static final Map<String, Long> lastModifiedCache = new HashMap<>();
    private static volatile ClassLoader currentClassLoader = XmlGenerator.class.getClassLoader();

    private static StringBuilder xmlBuilder = new StringBuilder();
    private static int indentLevel = 0;

    private static void addXmlLine(String line) {
        // Add indentation
        for (int i = 0; i < indentLevel; i++) {
            xmlBuilder.append("  ");
        }
        // Remove extra whitespace and newlines within tags
        xmlBuilder.append(line.trim()).append("\n");
    }

    private static void startXml() {
        xmlBuilder = new StringBuilder();
        indentLevel = 0;
        addXmlLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        addXmlLine("<Document>");
        indentLevel++;
    }

    private static void endXml() {
        indentLevel--;
        addXmlLine("</Document>");
    }

    private static void processField(String fieldName, Object value) {
        if (value instanceof List) {
            addXmlLine("<" + fieldName + ">");
            indentLevel++;
            for (Object item : (List<?>) value) {
                processObject(item);
            }
            indentLevel--;
            addXmlLine("</" + fieldName + ">");
        } else if (value instanceof LabelValue) {
            LabelValue lv = (LabelValue) value;
            processField(lv.balise, lv.value);
        } else if (value != null && value.getClass().getName().contains("Document$")) {
            // Handle nested objects
            addXmlLine("<" + fieldName + ">");
            indentLevel++;
            processObject(value);
            indentLevel--;
            addXmlLine("</" + fieldName + ">");
        } else if (value != null && value.getClass().getName().contains("TMontant")) {
            // Handle TMontant objects with Ccy attribute
            try {
                // Use reflection to try to get the Ccy value
                Method getCcyMethod = value.getClass().getMethod("getCcy");
                String ccyValue = (String) getCcyMethod.invoke(value);
                
                // Create tag with Ccy attribute
                if (ccyValue != null && !ccyValue.isEmpty()) {
                    addXmlLine("<" + fieldName + " Ccy=\"" + ccyValue + "\"></" + fieldName + ">");
                } else {
                    addXmlLine("<" + fieldName + " Ccy=\"\"></" + fieldName + ">");
                }
            } catch (Exception e) {
                // Fallback to simple empty tag if reflection fails
                addXmlLine("<" + fieldName + "></" + fieldName + ">");
            }
            
            // Add Devise tag after cvMntDin
            if (fieldName.equals("cvMntDin")) {
                addXmlLine("<Devise></Devise>");
            }
        } else {
            // Handle primitive values and null values
            String stringValue = (value != null) ? value.toString().trim() : "";
            if (stringValue.isEmpty() || stringValue.equals("0") || stringValue.equals("0.0")) {
                // For empty/zero values, create an empty tag instead of self-closing
                addXmlLine("<" + fieldName + "></" + fieldName + ">");
            } else if (!stringValue.contains("@")) { // Skip object references
                addXmlLine("<" + fieldName + ">" + stringValue + "</" + fieldName + ">");
            }
        }
    }

    private static void processObject(Object obj) {
        if (obj == null) {
            return;
        }
        
        try {
            Set<String> processedFields = new HashSet<>(); // Track processed fields
            for (Field field : getAllFields(obj.getClass())) {
                field.setAccessible(true);
                if (!field.getName().startsWith("_") && !Modifier.isStatic(field.getModifiers())) {
                    if (!processedFields.contains(field.getName())) {
                        processedFields.add(field.getName());
                        Object fieldValue = field.get(obj);
                        processField(field.getName(), fieldValue);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing object: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // Clear caches before each run
                classCache.clear();
                lastModifiedCache.clear();
                
                Map<String, Object> fieldValues = generateXmlContent(args);
                System.out.println("Field values map: " + fieldValues);
                Main.main(args);
            } catch (Exception e) {
                System.err.println("Error during XML generation: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, 2, TimeUnit.SECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down scheduler...");
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }));
    }

    private static String enhanceXmlWithFieldValues(String xmlContent, Map<String, Object> fieldValues) {
        try {
            System.out.println("Enhancing XML with field values...");
            System.out.println("Original XML content:\n" + xmlContent);
            
            // Create a new StringBuilder to build the enhanced XML
            StringBuilder enhancedXml = new StringBuilder();
            int currentIndex = 0;
            
            // Process each field value
            for (Map.Entry<String, Object> entry : fieldValues.entrySet()) {
                if (entry.getValue() instanceof LabelValue) {
                    LabelValue lv = (LabelValue) entry.getValue();
                    
                    // Find the position of the empty element in the XML
                    String emptyElement = "<" + lv.balise + "/>";
                    int elementIndex = xmlContent.indexOf(emptyElement, currentIndex);
                    
                    if (elementIndex != -1) {
                        // Add the content before the empty element
                        enhancedXml.append(xmlContent.substring(currentIndex, elementIndex));
                        
                        // Add the opening tag
                        enhancedXml.append("<").append(lv.balise).append(">");
                        
                        // Process the value
                        if (lv.value instanceof List) {
                            // Handle list values
                            List<?> list = (List<?>) lv.value;
                            for (Object item : list) {
                                if (item != null) {
                                    // Process each item in the list
                                    enhancedXml.append("<").append(lv.balise.substring(0, lv.balise.length() - 1)).append(">");
                                    processObjectToXml(item, enhancedXml);
                                    enhancedXml.append("</").append(lv.balise.substring(0, lv.balise.length() - 1)).append(">");
                                }
                            }
                        } else if (lv.value != null && lv.value.getClass().getName().contains("Document$")) {
                            // Handle nested objects
                            processObjectToXml(lv.value, enhancedXml);
                        } else {
                            // Handle primitive values
                            enhancedXml.append(lv.value != null ? lv.value.toString() : "");
                        }
                        
                        // Add the closing tag
                        enhancedXml.append("</").append(lv.balise).append(">");
                        
                        // Update the current index
                        currentIndex = elementIndex + emptyElement.length();
                    }
                }
            }
            
            // Add any remaining content
            if (currentIndex < xmlContent.length()) {
                enhancedXml.append(xmlContent.substring(currentIndex));
            }
            
            System.out.println("Enhanced XML content:\n" + enhancedXml.toString());
            return enhancedXml.toString();
        } catch (Exception e) {
            System.err.println("Error enhancing XML: " + e.getMessage());
            e.printStackTrace();
            return xmlContent;
        }
    }

    private static void processObjectToXml(Object obj, StringBuilder xmlBuilder) {
        try {
            for (Field field : getAllFields(obj.getClass())) {
                field.setAccessible(true);
                if (!field.getName().startsWith("_") && !Modifier.isStatic(field.getModifiers())) {
                    Object value = field.get(obj);
                    if (value != null) {
                        String fieldName = field.getName();
                        xmlBuilder.append("<").append(fieldName).append(">");
                        
                        if (value instanceof List) {
                            // Handle list values
                            for (Object item : (List<?>) value) {
                                if (item != null) {
                                    processObjectToXml(item, xmlBuilder);
                                }
                            }
                        } else if (value.getClass().getName().contains("Document$")) {
                            // Handle nested objects
                            processObjectToXml(value, xmlBuilder);
                        } else {
                            // Handle primitive values
                            xmlBuilder.append(value.toString());
                        }
                        
                        xmlBuilder.append("</").append(fieldName).append(">");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing object to XML: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static Map<String, Object> generateXmlContent(String[] args) {
        Map<String, Object> fieldValues = new LinkedHashMap<>();
        try {
            System.out.println("Starting XML generation...");

            xmlBuilder = new StringBuilder();
            indentLevel = 0;
            addXmlLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            addXmlLine("<Document>");
            indentLevel++;

            File generatedDir = new File(GENERATED_SOURCES_PATH);
            if (!generatedDir.exists()) {
                System.err.println("Generated sources directory not found: " + GENERATED_SOURCES_PATH);
                return fieldValues;
            }

            String largestFileName = getLargestFileName(GENERATED_SOURCES_PATH);
            if (largestFileName == null) {
                System.err.println("No Java files found in package path: " + GENERATED_SOURCES_PATH);
                return fieldValues;
            }
            System.out.println("Largest file found: " + largestFileName);

            File sourceFile = new File(generatedDir, largestFileName + ".java");
            if (sourceFile.exists()) {
                forceRecompile(sourceFile);
            }

            String className = "com.example.demo.generated.v2." + largestFileName;
            System.out.println("Class name to load: " + className);

            Class<?> clazz = forceClassReload(className, sourceFile);
            if (clazz == null) {
                System.err.println("Failed to load class: " + className);
                return fieldValues;
            }
            System.out.println("Class loaded: " + clazz.getName());

            Object objectToMarshal = createObjectWithNestedStructure(clazz);
            System.out.println("Object created: " + objectToMarshal);

            fieldValues = setAllFieldsToEmpty(objectToMarshal);

            // Process the root object
            processObject(objectToMarshal);

            // Close the root tag
            indentLevel--;
            addXmlLine("</Document>");

            // Create output directory if it doesn't exist
            File outputDir = new File("../");
            outputDir.mkdirs();

            String timestamp = String.valueOf(System.currentTimeMillis());
            File outputFile = new File(outputDir, "right.xml");

            // Write the XML to file
            Files.write(outputFile.toPath(), xmlBuilder.toString().getBytes());
            System.out.println("XML saved to: " + outputFile.getAbsolutePath());

            // Also write to a fixed file for easier testing
            File fixedFile = new File("output/latest.xml");
            Files.write(fixedFile.toPath(), xmlBuilder.toString().getBytes());
            System.out.println("XML also saved to: " + fixedFile.getAbsolutePath());

            return fieldValues;

        } catch (Exception e) {
            System.err.println("Error during XML generation: " + e.getMessage());
            e.printStackTrace();
            return fieldValues;
        }
    }

    private static void compileJavaFile(File sourceFile) {
        try {
            // Create the output directory if it doesn't exist
            File outputDir = new File(COMPILED_CLASSES_PATH);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            // Get the Java compiler
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                System.err.println("No Java compiler found. Please ensure JDK is installed and JAVA_HOME is set correctly.");
                return;
            }

            // Compile the Java file
            int result = compiler.run(null, null, null,
                    "-d", "target/classes",
                    sourceFile.getAbsolutePath());

            if (result != 0) {
                System.err.println("Compilation failed for: " + sourceFile.getName());
            } else {
                System.out.println("Successfully compiled: " + sourceFile.getName());
            }
        } catch (Exception e) {
            System.err.println("Error compiling Java file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void addToClasspath(String path) {
        try {
            // Convert the path to a URL
            File file = new File(path);
            if (!file.exists()) {
                System.err.println("Classpath directory not found: " + path);
                return;
            }

            // Add the directory to the classpath
            java.net.URLClassLoader classLoader = (java.net.URLClassLoader) ClassLoader.getSystemClassLoader();
            java.lang.reflect.Method method = java.net.URLClassLoader.class.getDeclaredMethod("addURL", java.net.URL.class);
            method.setAccessible(true);
            method.invoke(classLoader, file.toURI().toURL());
        } catch (Exception e) {
            System.err.println("Error adding to classpath: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Method to get the default constructor for a class
    private static Constructor<?> getDefaultConstructor(Class<?> clazz) {
        try {
            return clazz.getConstructor(); // Default no-arg constructor
        } catch (NoSuchMethodException e) {
            System.err.println("No default constructor found for class: " + clazz.getName());
            return null;
        }
    }

    // Improved method to create object with proper nested structure
    private static Object createObjectWithNestedStructure(Class<?> clazz) throws Exception {
        Constructor<?> constructor = getDefaultConstructor(clazz);
        if (constructor == null) {
            throw new InstantiationException("No default constructor found for class: " + clazz.getName());
        }

        Object instance = constructor.newInstance();

        // Special handling for common JAXB generated patterns
        for (Field field : getAllFields(clazz)) {
            field.setAccessible(true);

            // Skip JAXB specific fields
            if (field.getName().startsWith("_") || Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            // Handle nested objects that should be initialized
            if (!field.getType().isPrimitive() &&
                    !field.getType().isEnum() &&
                    !field.getType().getName().startsWith("java.")) {

                Object nestedObject = createObjectWithNestedStructure(field.getType());
                field.set(instance, nestedObject);
            }
        }

        return instance;
    }

    // Helper method to get all fields including inherited ones
    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> currentClass = clazz;

        while (currentClass != null && currentClass != Object.class) {
            fields.addAll(Arrays.asList(currentClass.getDeclaredFields()));
            currentClass = currentClass.getSuperclass();
        }

        return fields;
    }

    // Improved method to set all fields with better nested handling
    public static Map<String, Object> setAllFieldsToEmpty(Object object) throws Exception {
        Map<String, Object> fieldValues = new LinkedHashMap<>();
        Set<String> processedFields = new HashSet<>();
        int counter = 1;

        logger.info("Starting to process object: " + object.getClass().getSimpleName());

        for (Field field : getAllFields(object.getClass())) {
            field.setAccessible(true);

            if (field.getName().startsWith("_") || Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            // Skip if field already processed
            if (processedFields.contains(field.getName())) {
                continue;
            }
            processedFields.add(field.getName());

            String label = "A" + counter++;
            String balise = field.getName();

            logger.info("Processing field: " + balise + " of type: " + field.getType().getName());

            Object value;

            if (field.getType() == String.class) {
                value = "";
            } else if (field.getType() == BigInteger.class) {
                value = BigInteger.ZERO;
            } else if (field.getType() == BigDecimal.class) {
                value = BigDecimal.ZERO;
            } else if (field.getType() == int.class || field.getType() == Integer.class) {
                value = 0;
            } else if (field.getType() == double.class || field.getType() == Double.class) {
                value = 0.0;
            } else if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                value = false;
            } else if (field.getType().isEnum()) {
                value = field.getType().getEnumConstants()[0];
            } else if (field.getType() == List.class) {
                value = new ArrayList<>();
                if (field.getGenericType() instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType) field.getGenericType();
                    Type[] actualTypeArguments = pt.getActualTypeArguments();
                    if (actualTypeArguments.length > 0 && actualTypeArguments[0] instanceof Class) {
                        Class<?> listType = (Class<?>) actualTypeArguments[0];
                        logger.info("Creating list item of type: " + listType.getName());
                        Object listItem = createObjectWithNestedStructure(listType);
                        ((List) value).add(listItem);
                        // Process nested fields of list items
                        Map<String, Object> nestedValues = setAllFieldsToEmpty(listItem);
                        fieldValues.putAll(nestedValues);
                    }
                }
            } else {
                // Handle nested objects
                Object nestedObject = field.get(object);
                if (nestedObject == null) {
                    nestedObject = createObjectWithNestedStructure(field.getType());
                    field.set(object, nestedObject);
                }
                value = nestedObject;
                // Process nested fields
                Map<String, Object> nestedValues = setAllFieldsToEmpty(nestedObject);
                fieldValues.putAll(nestedValues);
            }

            // Set the field value and add to fieldValues
            field.set(object, value);
            fieldValues.put(label, new LabelValue(label, balise, value));
            logger.info("Set field: " + balise + " with value type: " + 
                       (value != null ? value.getClass().getName() : "null"));
        }

        logger.info("Finished processing object: " + object.getClass().getSimpleName());
        return fieldValues;
    }

    private static String getLargestFileName(String packagePath) {
        File dir = new File(packagePath);
        File[] files = dir.listFiles((dir1, name) -> name.endsWith(".java") && !name.equals("ObjectFactory.java"));
        if (files != null && files.length > 0) {
            File largestFile = Arrays.stream(files)
                    .max((file1, file2) -> Long.compare(file1.length(), file2.length()))
                    .orElse(null);

            if (largestFile != null) {
                return largestFile.getName().replace(".java", "");
            }
        }
        return null;
    }

    // Inner class to hold label, balise and value information
    public static class LabelValue {
        public final String label;
        public final String balise;
        public final Object value;

        public LabelValue(String label, String balise, Object value) {
            this.label = label;
            this.balise = balise;
            this.value = value;
        }

        @Override
        public String toString() {
            return "LabelValue{" +
                    "label='" + label + '\'' +
                    ", balise='" + balise + '\'' +
                    ", value=" + value +
                    '}';
        }
    }

    private static void forceRecompile(File sourceFile) {
        try {
            System.out.println("Forcing recompilation of: " + sourceFile.getName());
            
            // Create output directory if it doesn't exist
            File outputDir = new File("target/classes");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            // Get the Java compiler
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                System.err.println("No Java compiler found. Please ensure JDK is installed and JAVA_HOME is set correctly.");
                return;
            }

            // Compile all Java files in the directory to ensure dependencies are compiled
            File[] javaFiles = sourceFile.getParentFile().listFiles((dir, name) -> name.endsWith(".java"));
            if (javaFiles != null) {
                // Sort files to compile dependencies first
                Arrays.sort(javaFiles, (f1, f2) -> {
                    // Put TMontant.java first
                    if (f1.getName().equals("TMontant.java")) return -1;
                    if (f2.getName().equals("TMontant.java")) return 1;
                    return f1.getName().compareTo(f2.getName());
                });

                for (File file : javaFiles) {
                    System.out.println("Compiling: " + file.getName());
                    int result = compiler.run(null, null, null,
                            "-d", "target/classes",
                            file.getAbsolutePath());

                    if (result != 0) {
                        System.err.println("Compilation failed for: " + file.getName());
                    } else {
                        System.out.println("Successfully compiled: " + file.getName());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error during forced recompilation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Class<?> forceClassReload(String className, File sourceFile) {
        try {
            System.out.println("Forcing class reload for: " + className);
            
            // Create a new class loader that will load classes from the compiled directory
            currentClassLoader = new URLClassLoader(
                new URL[]{new File("target/classes").toURI().toURL()},
                null  // Use null as parent to force loading from our URL
            );

            // Force garbage collection to clean up old class loaders mta3 le5ra
            System.gc();
            
            // Load the class
            Class<?> clazz = currentClassLoader.loadClass(className);
            System.out.println("Successfully loaded class: " + className);
            
            // Update cache
            classCache.put(className, clazz);
            lastModifiedCache.put(className, sourceFile.lastModified());
            
            return clazz;
        } catch (Exception e) {
            System.err.println("Error forcing class reload: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}