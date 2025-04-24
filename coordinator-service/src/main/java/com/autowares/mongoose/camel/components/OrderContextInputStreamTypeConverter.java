package com.autowares.mongoose.camel.components;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.camel.Converter;
import org.apache.camel.TypeConverters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.CoordinatorOrderContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class OrderContextInputStreamTypeConverter implements TypeConverters {

	  private final ObjectMapper mapper;

	  @Autowired
	  public OrderContextInputStreamTypeConverter(ObjectMapper mapper) {
	    this.mapper = mapper;
	  }

	  @Converter
	  public InputStream contextToInputStream(CoordinatorOrderContext source) {
	    try {
	      return new ByteArrayInputStream(mapper.writeValueAsBytes(source));
	    } catch (JsonProcessingException e) {
	      throw new RuntimeException(e);
	    }
	  }

	  @Converter
	  public CoordinatorOrderContext inputStreamToContext(InputStream source) {
	    try {
	      return mapper.readValue(source, CoordinatorOrderContext.class);
	    } catch (IOException e) {
	      throw new RuntimeException(e);
	    }
	  }

}
