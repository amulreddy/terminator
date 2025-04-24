package com.autowares.mongoose.service;

import org.springframework.stereotype.Component;

import com.autowares.servicescommon.util.SequenceGenerator;

@Component
public class SequenceGeneratorService {
	
	private final static SequenceGenerator sequenceGenerator = new SequenceGenerator();
	
	public String getNextSequence() {
		return String.valueOf(sequenceGenerator.nextId());
	}

}
