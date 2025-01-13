/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package com.nebula.web.boot.jackson;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.google.common.base.Charsets;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.lang.Nullable;
import org.springframework.util.TypeUtils;

/**
 * AbstractReadWriteJackson2HttpMessageConverter
 */
public abstract class AbstractReadWriteJackson2HttpMessageConverter extends AbstractJackson2HttpMessageConverter {
    
    private static final java.nio.charset.Charset DEFAULT_CHARSET = Charsets.UTF_8;
    
    private final ObjectMapper writeObjectMapper;
    @Nullable
    private PrettyPrinter ssePrettyPrinter;
    
    public AbstractReadWriteJackson2HttpMessageConverter(ObjectMapper readObjectMapper, ObjectMapper writeObjectMapper) {
        super(readObjectMapper);
        this.writeObjectMapper = writeObjectMapper;
        initSsePrettyPrinter();
    }
    
    public AbstractReadWriteJackson2HttpMessageConverter(ObjectMapper readObjectMapper, ObjectMapper writeObjectMapper, MediaType supportedMediaType) {
        this(readObjectMapper, writeObjectMapper);
        setSupportedMediaTypes(Collections.singletonList(supportedMediaType));
        initSsePrettyPrinter();
    }
    
    public AbstractReadWriteJackson2HttpMessageConverter(ObjectMapper readObjectMapper, ObjectMapper writeObjectMapper, MediaType... supportedMediaTypes) {
        this(readObjectMapper, writeObjectMapper);
        setSupportedMediaTypes(Arrays.asList(supportedMediaTypes));
    }
    
    private void initSsePrettyPrinter() {
        setDefaultCharset(DEFAULT_CHARSET);
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentObjectsWith(new DefaultIndenter("  ", "\ndata:"));
        this.ssePrettyPrinter = prettyPrinter;
    }
    
    @Override
    public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
        if (!canWrite(mediaType)) {
            return false;
        }
        AtomicReference<Throwable> causeRef = new AtomicReference<>();
        if (getObjectMapper().canSerialize(clazz, causeRef)) {
            return true;
        }
        logWarningIfNecessary(clazz, causeRef.get());
        return false;
    }
    
    @Override
    protected void writeInternal(Object object, @Nullable Type type, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        
        MediaType contentType = outputMessage.getHeaders().getContentType();
        JsonEncoding encoding = getJsonEncoding(contentType);
        JsonGenerator generator = this.writeObjectMapper.getFactory().createGenerator(outputMessage.getBody(), encoding);
        try {
            writePrefix(generator, object);
            
            Object value = object;
            Class<?> serializationView = null;
            FilterProvider filters = null;
            JavaType javaType = null;
            
            if (object instanceof MappingJacksonValue) {
                MappingJacksonValue container = (MappingJacksonValue) object;
                value = container.getValue();
                serializationView = container.getSerializationView();
                filters = container.getFilters();
            }
            if (type != null && TypeUtils.isAssignable(type, value.getClass())) {
                javaType = getJavaType(type, null);
            }
            
            ObjectWriter objectWriter = (serializationView != null ? this.writeObjectMapper.writerWithView(serializationView) : this.writeObjectMapper.writer());
            if (filters != null) {
                objectWriter = objectWriter.with(filters);
            }
            if (javaType != null && javaType.isContainerType()) {
                objectWriter = objectWriter.forType(javaType);
            }
            SerializationConfig config = objectWriter.getConfig();
            if (contentType != null && contentType.isCompatibleWith(MediaType.TEXT_EVENT_STREAM) &&
                    config.isEnabled(SerializationFeature.INDENT_OUTPUT)) {
                objectWriter = objectWriter.with(this.ssePrettyPrinter);
            }
            objectWriter.writeValue(generator, value);
            
            writeSuffix(generator, object);
            generator.flush();
        } catch (InvalidDefinitionException ex) {
            throw new HttpMessageConversionException("Type definition error: " + ex.getType(), ex);
        } catch (JsonProcessingException ex) {
            throw new HttpMessageNotWritableException("Could not write JSON: " + ex.getOriginalMessage(), ex);
        }
    }
    
}
