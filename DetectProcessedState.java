package com.autowares.mongoose.camel.processors.conditiondetection;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import com.autowares.mongoose.exception.RetryLaterException;
import com.autowares.mongoose.service.SupplyChainService;
import com.autowares.servicescommon.model.Document;
import com.autowares.servicescommon.model.TransactionStage;

@Component
public class DetectProcessedState implements Processor {

	@Autowired
	private SupplyChainService supplyChainService;

	@Override
	public void process(Exchange exchange) throws Exception {

		Boolean forced = exchange.getIn().getHeader("forcedProcessing", Boolean.class);

		if (forced != null && forced) {
			return;
		}

		Document document = exchange.getIn().getBody(Document.class);
		try {
			TransactionStage transactionStage = supplyChainService.getClient()
					.getSourceDocumentTransactionStage(document.getDocumentId());
			if (!TransactionStage.Ready.equals(transactionStage)) {
				throw new RetryLaterException("Transaction stage is not Ready.  Expected Ready, found "
						+ transactionStage + "  " + document.getDocumentId());
			}
		} catch (Exception e) {
			Throwable rootException = ExceptionUtils.getRootCause(e);
			if (rootException instanceof HttpClientErrorException) {
				HttpClientErrorException clientError = (HttpClientErrorException) rootException;
				if (clientError.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
					throw new RetryLaterException("Document Not Found " + document.getDocumentId());
				}
			}
			throw e;
		}
	}
}
