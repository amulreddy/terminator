package com.autowares.mongoose.coordinator.routes;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;

import com.autowares.mongoose.camel.processors.communication.CustomerNotification;
import com.autowares.mongoose.camel.processors.conditiondetection.DetectProcessedState;
import com.autowares.mongoose.camel.processors.util.DocumentLocker;
import com.autowares.mongoose.camel.processors.util.DocumentUnLocker;
import com.autowares.mongoose.camel.routes.SupervisoryRoutes;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.CoordinatorContextImpl;
import com.autowares.mongoose.service.SupplyChainService;
import com.autowares.servicescommon.model.SourceDocument;
import com.autowares.servicescommon.util.PrettyPrint;
import com.autowares.supplychain.model.SupplyChainSourceDocument;
import com.autowares.supplychain.model.SupplyChainSourceDocumentImpl;
import com.autowares.xmlgateway.model.InquiryRequest;
import com.autowares.xmlgateway.model.RequestItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

@SpringBootTest
@CamelSpringBootTest
@ActiveProfiles("unitTest")
@ExtendWith(MockitoExtension.class)
/*
 * Move to Integration testing if keeping
 */
@Disabled
public class InputRouteTest {

	@Autowired
	private ProducerTemplate template;

	@Autowired
	private ObjectMapper objectMapper;
	
	@MockBean
	private DocumentLocker documentLocker;
	
	@MockBean
	private DetectProcessedState detectProcessedState;
	
	@Mock
	private SupplyChainService supplyChainService;
	
	@Captor
	ArgumentCaptor<String> stringCaptor;
	
	@MockBean
	private DocumentUnLocker documentUnLocker;
	
	@MockBean
	private CustomerNotification customerNotification;
	
	@MockBean
	private SupervisoryRoutes supervisoryRoutes;
	
//    @Rule
//    public MockitoRule rule = MockitoJUnit.rule().silent();
	
	@BeforeEach
	public void setUp() {
		
		when(supplyChainService.retrieve(anyString())).thenAnswer(invocation -> {
            String argument = invocation.getArgument(0);
        	String json = new String(this.getClass().getClassLoader().getResourceAsStream(argument).readAllBytes());
    		SupplyChainSourceDocumentImpl supplyChainSourceDocument = objectMapper.readValue(
    				json,
    				SupplyChainSourceDocumentImpl.class);
            return supplyChainSourceDocument;
        });
		
		when(supplyChainService.persist(any(SourceDocument.class))).thenAnswer(invocation -> {
			SourceDocument argument = invocation.getArgument(0);
			Path path = Paths.get("src","test","resources",argument.getDocumentId());
			Files.writeString(path, objectMapper.writeValueAsString(argument));
			return argument;
		});
		
		when(supplyChainService.resolveOrderContext(anyString())).thenCallRealMethod();
		
	}
	
	@Test
	public void whenInquiringWeGetAResponse() throws InterruptedException {
		InquiryRequest request = new InquiryRequest();
		request.setAccountNumber("750");
		RequestItem lineItem = new RequestItem();
		lineItem.setPartNumber("51515");
		lineItem.setLineCode("WIX");
		lineItem.setQuantity(1);
		request.setLineItems(Lists.newArrayList(lineItem));
		CoordinatorContext response = template.requestBody("direct:inquiry", request, CoordinatorContext.class);
	}

	@Test
	public void whenWeSubmitAnInitialQuoteWeGetAReferenceDocument() throws InterruptedException, IOException {
		InquiryRequest request = new InquiryRequest();
		request.setAccountNumber("750");
		RequestItem lineItem = new RequestItem();
		lineItem.setPartNumber("51515");
		lineItem.setLineCode("WIX");
		lineItem.setQuantity(1);
		request.setLineItems(Lists.newArrayList(lineItem));
		CoordinatorContextImpl response = template.requestBody("direct:saveInitialQuote", request,
				CoordinatorContextImpl.class);
		assertNotNull(response.getReferenceDocument());
	}

	@Test
	public void initialQuoteProcessing() throws IOException {

		SupplyChainSourceDocument supplyChainSourceDocument = supplyChainService.retrieve("2fd25781-95e0-4bc7-9d49-9a604dc4cd3f");
		CoordinatorContext response = template.requestBody("direct:processQuote", supplyChainSourceDocument,
				CoordinatorContext.class);
		
		PrettyPrint.print(response.getProcurementGroupContext());
		supplyChainService.retrieve(supplyChainSourceDocument.getDocumentId());

	}
	
	@Test
	public void quoteProcessingPhase2() throws IOException {

		SupplyChainSourceDocument supplyChainSourceDocument = supplyChainService.retrieve("94c6d028-7c40-4488-abc9-1ebb31580f90");
		CoordinatorContext response = template.requestBody("direct:processQuote", supplyChainSourceDocument,
				CoordinatorContext.class);
		
		PrettyPrint.print(response.getProcurementGroupContext());
		supplyChainService.retrieve(supplyChainSourceDocument.getDocumentId());

	}


}
