package com.example.demo;

import javax.xml.bind.*;
import java.io.File;
import java.io.StringWriter;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.logging.Logger;

public class XmlUtils {
    private static final Logger logger = Logger.getLogger(XmlUtils.class.getName());

    public static Object createObjectWithNestedStructure(Class<?> clazz) throws Exception {
        Constructor<?> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object instance = constructor.newInstance();

        for (Field field : getAllFields(clazz)) {
            field.setAccessible(true);
            if (field.getName().startsWith("_") || Modifier.isStatic(field.getModifiers())) continue;

            if (!field.getType().isPrimitive()
                    && !field.getType().isEnum()
                    && !field.getType().getName().startsWith("java.")) {
                Object nestedObject = createObjectWithNestedStructure(field.getType());
                field.set(instance, nestedObject);
            }
        }
        return instance;
    }

    public static Map<String, Object> setAllFieldsToEmpty(Object object) throws Exception {
        Map<String, Object> fieldValues = new LinkedHashMap<>();
        int counter = 1;

        for (Field field : getAllFields(object.getClass())) {
            field.setAccessible(true);
            if (field.getName().startsWith("_") || Modifier.isStatic(field.getModifiers())) continue;

            String label = "A" + counter++;
            String balise = field.getName();
            Object value;

            if (field.getType() == String.class) value = "";
            else if (field.getType() == BigInteger.class) value = BigInteger.ZERO;
            else if (field.getType() == BigDecimal.class) value = BigDecimal.ZERO;
            else if (field.getType() == int.class || field.getType() == Integer.class) value = 0;
            else if (field.getType() == double.class || field.getType() == Double.class) value = 0.0;
            else if (field.getType() == boolean.class || field.getType() == Boolean.class) value = false;
            else if (field.getType().isEnum()) value = field.getType().getEnumConstants()[0];
            else if (field.getType() == List.class) {
                value = new ArrayList<>();
                if (field.getGenericType() instanceof ParameterizedType pt) {
                    Class<?> listType = (Class<?>) pt.getActualTypeArguments()[0];
                    Object listItem = createObjectWithNestedStructure(listType);
                    ((List) value).add(listItem);
                    fieldValues.putAll(setAllFieldsToEmpty(listItem));
                }
            } else {
                Object nestedObject = field.get(object);
                if (nestedObject == null) {
                    nestedObject = createObjectWithNestedStructure(field.getType());
                    field.set(object, nestedObject);
                }
                value = nestedObject;
                fieldValues.putAll(setAllFieldsToEmpty(nestedObject));
            }

            field.set(object, value);
            fieldValues.put(label, new LabelValue(label, balise, value));
        }

        return fieldValues;
    }

    public static void marshalToXml(Object object, Class<?> clazz, String filePath) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(clazz);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(object, new File(filePath));
    }

    public static String marshalToString(Object object, Class<?> clazz) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(clazz);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        StringWriter writer = new StringWriter();
        marshaller.marshal(object, writer);
        return writer.toString();
    }

    public static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }
}
