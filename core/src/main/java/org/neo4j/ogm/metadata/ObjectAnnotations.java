/*
 * Copyright (c) 2002-2018 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 *  conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.metadata;

import static java.util.stream.Collectors.*;

import java.lang.annotation.Annotation;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.neo4j.ogm.annotation.typeconversion.DateLong;
import org.neo4j.ogm.annotation.typeconversion.DateString;
import org.neo4j.ogm.annotation.typeconversion.EnumString;
import org.neo4j.ogm.annotation.typeconversion.NumberString;
import org.neo4j.ogm.typeconversion.DateLongConverter;
import org.neo4j.ogm.typeconversion.DateStringConverter;
import org.neo4j.ogm.typeconversion.EnumStringConverter;
import org.neo4j.ogm.typeconversion.InstantLongConverter;
import org.neo4j.ogm.typeconversion.NumberStringConverter;

/**
 * @author Vince Bickers
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
public class ObjectAnnotations {

    private final Map<String, AnnotationInfo> annotations;

    static ObjectAnnotations of(Annotation... annotations) {

        Map<String, AnnotationInfo> annotationInfo = Arrays.stream(annotations) //
            .map(AnnotationInfo::new) //
            .collect(toMap(AnnotationInfo::getName, Function.identity()));
        return new ObjectAnnotations(annotationInfo);
    }

    private ObjectAnnotations(Map<String, AnnotationInfo> annotations) {
        this.annotations = annotations;
    }

    public AnnotationInfo get(String key) {
        return annotations.get(key);
    }

    public AnnotationInfo get(Class<?> keyClass) {
        return keyClass == null ? null : annotations.get(keyClass.getName());
    }

    public boolean isEmpty() {
        return annotations.isEmpty();
    }

    Object getConverter(Class<?> fieldType) {

        // try to get a custom type converter
        AnnotationInfo customType = get(Convert.class);
        if (customType != null) {
            String classDescriptor = customType.get(Convert.CONVERTER, null);
            if (classDescriptor == null || Convert.Unset.class.getName().equals(classDescriptor)) {
                return null; // will have a default proxy converter applied later on
            }

            try {
                Class<?> clazz = Class.forName(classDescriptor, false, Thread.currentThread().getContextClassLoader());
                return clazz.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        // try to find a pre-registered type annotation. this is very clumsy, but at least it is done only once
        AnnotationInfo dateLongConverterInfo = get(DateLong.class);
        if (dateLongConverterInfo != null) {
            if (fieldType.equals(Instant.class)) {
                return new InstantLongConverter();
            }
            return new DateLongConverter();
        }

        AnnotationInfo dateStringConverterInfo = get(DateString.class);
        if (dateStringConverterInfo != null) {
            String format = dateStringConverterInfo.get(DateString.FORMAT, DateString.ISO_8601);
            return new DateStringConverter(format, isLenientConversion(dateStringConverterInfo));
        }

        AnnotationInfo enumStringConverterInfo = get(EnumString.class);
        if (enumStringConverterInfo != null) {
            String classDescriptor = enumStringConverterInfo.get(EnumString.TYPE, null);
            try {
                Class clazz = Class.forName(classDescriptor, false, Thread.currentThread().getContextClassLoader());
                return new EnumStringConverter(clazz, isLenientConversion(enumStringConverterInfo));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        AnnotationInfo numberStringConverterInfo = get(NumberString.class);
        if (numberStringConverterInfo != null) {
            String classDescriptor = numberStringConverterInfo.get(NumberString.TYPE, null);
            try {
                Class clazz = Class.forName(classDescriptor, false, Thread.currentThread().getContextClassLoader());
                return new NumberStringConverter(clazz, isLenientConversion(numberStringConverterInfo));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return null;
    }

    private boolean isLenientConversion(AnnotationInfo converterInfo) {
        String lenientConversionKey = "lenient";
        return Boolean.parseBoolean(converterInfo.get(lenientConversionKey));
    }

    public boolean has(Class<?> clazz) {
        return annotations.containsKey(clazz.getName());
    }
}
