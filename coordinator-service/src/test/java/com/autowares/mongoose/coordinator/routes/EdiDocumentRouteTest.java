package com.autowares.mongoose.coordinator.routes;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ExecutionException;

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
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;

import com.autowares.invoicesservice.client.InvoicesClient;
import com.autowares.invoicesservice.model.MoaInvoice;
import com.autowares.mongoose.camel.processors.conditiondetection.DetectProcessedState;
import com.autowares.mongoose.camel.processors.postfulfillment.PickupWarehouseVendorIntegration;
import com.autowares.mongoose.camel.processors.util.DocumentLocker;
import com.autowares.mongoose.camel.processors.util.DocumentUnLocker;
import com.autowares.mongoose.camel.routes.SupervisoryRoutes;
import com.autowares.mongoose.model.DocumentContext;
import com.autowares.mongoose.model.ProcurementGroupContext;
import com.autowares.mongoose.service.SupplyChainService;
import com.autowares.partyconfiguration.client.PartyConfigurationClient;
import com.autowares.partyconfiguration.model.Configuration;
import com.autowares.partyconfiguration.model.SupplyChain;
import com.autowares.servicescommon.model.CustomerFulfillmentOptions;
import com.autowares.servicescommon.model.DocumentModification;
import com.autowares.servicescommon.model.SourceDocument;
import com.autowares.servicescommon.model.SystemType;
import com.autowares.servicescommon.util.PrettyPrint;
import com.autowares.supplychain.model.ProcurementGroup;
import com.autowares.supplychain.model.SupplyChainSourceDocumentImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;

@SpringBootTest
@CamelSpringBootTest
@ActiveProfiles("local")
/*
 * Move to Integration testing if keeping
 */
@Disabled
@ExtendWith(MockitoExtension.class)
public class EdiDocumentRouteTest {

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
	private SupervisoryRoutes supervisoryRoutes;
	
	@MockBean
	private PickupWarehouseVendorIntegration pickupWarehouse;
	
    @Rule
    public MockitoRule rule = MockitoJUnit.rule().silent();
	
	@BeforeEach
	public void setUp() {
		
//		when(supplyChainService.retrieve(anyString())).thenAnswer(invocation -> {
//            String argument = invocation.getArgument(0);
//    		SupplyChainSourceDocumentImpl supplyChainSourceDocument = objectMapper.readValue(
//    				this.getClass().getClassLoader().getResourceAsStream(argument),
//    				SupplyChainSourceDocumentImpl.class);
//            return supplyChainSourceDocument;
//        });
//		
//		when(supplyChainService.persist(any(SourceDocument.class))).thenAnswer(invocation -> {
//			SourceDocument argument = invocation.getArgument(0);
//			Path path = Paths.get("src","test","resources",argument.getDocumentId());
//			Files.writeString(path, objectMapper.writeValueAsString(argument));
//			return argument;
//		});
		
//		Mockito.doReturn(new Object()).when(supplyChainService.retrieve(anyString()));

		
	}
	
	@Test
	public void whenInvoiceDocumentIsReceived() throws InterruptedException, IOException {
		InputStream file = this.getClass().getClassLoader().getResourceAsStream("INVhwinvtest001.dat");
		Object variable = template.requestBody("direct:ediDocumentSource", file, Object.class);
		assertNotNull(variable);
	}
	
	@Test
	public void generateMoaInvoice() {
		String packslip = "d31d1811-3fb1-4aeb-bee8-966fb5c1cdec";
		DocumentModification request = new DocumentModification();
		request.setDocumentId(packslip);
		DocumentContext context = new DocumentContext(request);
		Object variable = template.requestBody("direct:generate-invoice", context, Object.class);
		
	}
	
}