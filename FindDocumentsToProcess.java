package com.autowares.mongoose.camel.processors.lookup;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.service.SupplyChainService;
import com.autowares.servicescommon.model.TransactionStage;
import com.autowares.supplychain.model.SupplyChainSourceDocument;
import com.autowares.supplychain.request.SourceDocumentRequest;
import com.google.common.collect.Lists;

@Component
public class FindDocumentsToProcess implements Processor {

    @Autowired
    private SupplyChainService supplyChainService;
    
    private static Logger log = LoggerFactory.getLogger(FindDocumentsToProcess.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        SourceDocumentRequest request = SourceDocumentRequest.builder()
                .withTransactionStage(TransactionStage.Ready)
                .withOrder(Order.asc("supplyChainId"))
                .build();

        if (exchange.getIn().getBody() != null) {
            request = SourceDocumentRequest.builder().withDocumentId(exchange.getIn().getBody(String.class)).build();
        }

        Page<SupplyChainSourceDocument> result = supplyChainService.getClient().findSourceDocument(request);
        if (result != null && result.getTotalElements() > 0) {
            List<SupplyChainSourceDocument> results = Lists.newArrayList(result.getContent());
            if (result.getTotalElements() > 10) {
                List<String> documentIds = results.stream().map(i -> i.getDocumentId()).collect(Collectors.toList());
                request.setOrder(Order.desc("supplyChainId"));
                Page<SupplyChainSourceDocument> content = supplyChainService.getClient().findSourceDocument(request);
                content.forEach(i -> {
                    if (!documentIds.contains(i.getDocumentId())) {
                        results.add(i);
                    }
                });
            }
            log.info(
                    "Processing " + results.size() + " of: " + result.getTotalElements() + " to process");
            exchange.getIn().setBody(results);
        }
    }
}