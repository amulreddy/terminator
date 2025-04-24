package com.autowares.mongoose.coordinator.routes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.autowares.mongoose.service.SupplyChainService;
import com.autowares.servicescommon.model.SourceDocument;
import com.autowares.servicescommon.util.PrettyPrint;
import com.autowares.supplychain.model.SupplyChainSourceDocumentImpl;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("local")
@ExtendWith(MockitoExtension.class)
public class MockitoPlay {

	@Autowired
	private ObjectMapper objectMapper;

	@Mock
	private SupplyChainService supplyChainService;

	@Captor
	ArgumentCaptor<String> stringCaptor;

	@BeforeEach
	public void setUp() {
		
		when(supplyChainService.retrieve(anyString())).thenAnswer(invocation -> {
            String argument = invocation.getArgument(0);
    		SupplyChainSourceDocumentImpl supplyChainSourceDocument = objectMapper.readValue(
    				this.getClass().getClassLoader().getResourceAsStream(argument),
    				SupplyChainSourceDocumentImpl.class);
            return supplyChainSourceDocument;
        });
		
		when(supplyChainService.persist(any(SourceDocument.class))).thenAnswer(invocation -> {
			SourceDocument argument = invocation.getArgument(0);
			Path path = Paths.get("src","test","resources",argument.getDocumentId());
			Files.writeString(path, objectMapper.writeValueAsString(argument));
			return argument;
		});
		
	}

	@Test
	public void readAndWriteDocuments() throws IOException {
		// Call the method with a specific argument
		SupplyChainSourceDocumentImpl x = supplyChainService.retrieve("2fd25781-95e0-4bc7-9d49-9a604dc4cd3f");

		// Capture the argument for the original behavior
		verify(supplyChainService).retrieve(stringCaptor.capture());

		// Perform assertions for the original behavior
		assertNotNull(x);
		PrettyPrint.print(x);

		// Additional assertion based on the captured argument for the original behavior
		assertEquals("2fd25781-95e0-4bc7-9d49-9a604dc4cd3f", stringCaptor.getValue());

		// Verify the call count for the original behavior
		verify(supplyChainService, times(1)).retrieve(anyString());

		SourceDocument persisted = supplyChainService.persist(x);
		assertEquals("2fd25781-95e0-4bc7-9d49-9a604dc4cd3f", persisted.getDocumentId());

	}

}
