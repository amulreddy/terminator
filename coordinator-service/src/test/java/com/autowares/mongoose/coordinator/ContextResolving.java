package com.autowares.mongoose.coordinator;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.autowares.mongoose.model.DocumentContext;
import com.autowares.mongoose.model.ProcurementGroupContext;
import com.autowares.mongoose.model.TransactionalContext;
import com.autowares.mongoose.service.SupplyChainService;
import com.autowares.servicescommon.util.PrettyPrint;
import com.autowares.supplychain.model.ProcurementGroup;
import com.autowares.supplychain.model.TransactionContext;

public class ContextResolving {
	
	private SupplyChainService supplyChainService = new SupplyChainService();
	
	@Test
	public void resolveProcurementGroupContext() {
		ProcurementGroup procurementGroup = ProcurementGroup.builder().withProcumentGroupId(UUID.fromString("b84a9ca9-3211-45c1-a572-35ab63969451")).build();
		ProcurementGroupContext context = supplyChainService.resolveProcurementGroupContext(procurementGroup);
		PrettyPrint.print(context);
	}
	
	@Test
	public void resolveTransactionalContext() {
		TransactionContext transactionContext = TransactionContext.builder().withTransactionContextId(UUID.fromString("d8798063-4ebf-4d78-b1c1-098694ba4143")).build();
		TransactionalContext context = supplyChainService.resolveTransactionalContext(transactionContext);
		PrettyPrint.print(context.getOrderContext());
	}
	
	@Test
	public void resolveDocumentContext() {
		DocumentContext dc = new DocumentContext();
		dc.setDocumentId("1d44b798-a02a-4c0c-a713-792defa5d001");
		dc = supplyChainService.resolveDocumentContext(dc);
		PrettyPrint.print(dc.getAction());
	}
	
	@Test
	public void resolveTransactionalContextByReference() {
		TransactionContext transactionContext = TransactionContext.builder().withTransactionReferenceId("1186666374636523520").build();
		transactionContext.setTransactionContextId(null);
		TransactionalContext context = supplyChainService.resolveTransactionalContext(transactionContext);
		PrettyPrint.print(context.getOrderContext());
	}
	
	
	@Test
	public void resolveDocumentContextWith() {
		DocumentContext dc = new DocumentContext();
		dc.setDocumentId("1d44b798-a02a-4c0c-a713-792defa5d001");
		dc = supplyChainService.resolveDocumentContext(dc);
		PrettyPrint.print(dc.getAction());
	}


}
