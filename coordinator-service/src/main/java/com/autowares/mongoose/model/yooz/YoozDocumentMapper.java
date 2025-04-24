package com.autowares.mongoose.model.yooz;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

public class YoozDocumentMapper {

    public static DataContainer mapToDataContainer(List<YoozDocument> yoozDocuments) {
        DataContainer dataContainer = new DataContainer();
        DataContainer.Data data = new DataContainer.Data();
        DataContainer.DataBlocks dataBlocks = new DataContainer.DataBlocks();
        BigDecimal yoozAmount = BigDecimal.ZERO;
        // Assuming the first YoozDocument represents the header data for YZ_COMMONS
        if (!yoozDocuments.isEmpty()) {
        	
        	Double yA = yoozDocuments.stream()
        		    .mapToDouble(i -> i.getAmount().doubleValue() * i.getQuantityCharged())
        		    .sum();
            yoozAmount = BigDecimal.valueOf(yA);
        	
            YoozDocument firstDoc = yoozDocuments.get(0);
            DataContainer.YZCommons yzCommons = mapToYZCommons(firstDoc, yoozAmount);
            dataBlocks.setYZ_COMMONS(yzCommons);

        // Map each YoozDocument entry to a YZInvoice item in DataBlocks
        DataContainer.YZInvoice yzInvoice = new DataContainer.YZInvoice();
        if (!yoozDocuments.isEmpty()) {
            yzInvoice = mapToYZInvoice(firstDoc);
            dataBlocks.setYZ_INVOICE(yzInvoice);
        }
        List<Map<String, String>> yoozLineItems = new ArrayList<>();

        for (YoozDocument yoozDocument : yoozDocuments) {
        	Map<String, String> yoozLine = new HashMap<>();
            yoozLine.put("lineNumber", yoozDocument.getLineNumber());
            yoozLine.put("productCode", yoozDocument.getProductCode().toString());
            yoozLine.put("qty", yoozDocument.getQuantityCharged().toString());
            yoozLine.put("UOM", yoozDocument.getUnitOfMeasure().toUpperCase());
            yoozLine.put("buyerCode", yoozDocument.getBuyerCode());
            yoozLine.put("unitPrice", yoozDocument.getAmount().toString());
            yoozLineItems.add(yoozLine);
        }
        
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String listString = objectMapper.writeValueAsString(yoozLineItems);
            yzInvoice.setCUSTOM_2(new DataContainer.YZField<String>(listString));

        } catch (Exception e) {
            e.printStackTrace();
        }
        }

        data.setDataBlocks(dataBlocks);
        dataContainer.setData(data);
        return dataContainer;
    }

    private static DataContainer.YZCommons mapToYZCommons(YoozDocument yoozDocument, BigDecimal yoozAmount) {
        DataContainer.YZCommons yzCommons = new DataContainer.YZCommons();

        yzCommons.setYZ_THIRD(new DataContainer.YZField<String>(yoozDocument.getVendorCode()));
        yzCommons.setYZ_NUMBER(new DataContainer.YZField<String>(yoozDocument.getInvoiceNumber()));
        yzCommons.setYZ_DOCUMENT_TYPE(new DataContainer.YZField<String>("YZ_PURCHASE_INVOICE_ON_ORDER")); // Example value
        yzCommons.setYZ_ORGANIZATIONAL_UNIT(new DataContainer.YZField<String>("AWI")); // Example value
        if(yoozDocument.getOrderDate() != null) {
        	yzCommons.setYZ_DATE(new DataContainer.YZField<LocalDate>(parseDate(yoozDocument.getOrderDate())));
        }
        yzCommons.setYZ_TOTAL_AMOUNT(new DataContainer.YZField<BigDecimal>(yoozAmount));
        yzCommons.setYZ_AMOUNT(new DataContainer.YZField<BigDecimal>(yoozAmount));
        yzCommons.setYZ_TAX_AMOUNT(new DataContainer.YZField<BigDecimal>(BigDecimal.ZERO));
        yzCommons.setYZ_CURRENCY(new DataContainer.YZField<String>(yoozDocument.getCurrency()));
        if(yoozDocument.getDueDate() != null) {
        	yzCommons.setYZ_DUE_DATE(new DataContainer.YZField<LocalDate>(parseDate(yoozDocument.getDueDate())));
        }
        yzCommons.setYZ_PAYMENT_METHOD(new DataContainer.YZField<String>(yoozDocument.getPaymentMethod()));
        yzCommons.setYZ_RECEPTION_DATE(new DataContainer.YZField<LocalDate>(parseDate(yoozDocument.getReceptionDate())));

        return yzCommons;
    }

    private static DataContainer.YZInvoice mapToYZInvoice(YoozDocument yoozDocument) {
        DataContainer.YZInvoice yzInvoice = new DataContainer.YZInvoice();

        yzInvoice.setCUSTOM_1(new DataContainer.YZField<String>(yoozDocument.getOrderCreator()));
        String poNumber = yoozDocument.getOrderNumber().substring(0, 14);
        if (poNumber != null) {
        	poNumber = poNumber.replaceAll("[^a-zA-Z0-9]", "");
        }
		yzInvoice.setYZ_PURCHASE_ORDER_NUMBER(new DataContainer.YZField<String>(poNumber));
        yzInvoice.setYZ_DEDUCED_AMOUNT(new DataContainer.YZField<BigDecimal>(BigDecimal.ZERO)); // Example default
        // TODO Build List of YoozLineItem for CUSTOM_2 field
        // TODO need to add UOM and buyerCode to YoozDocument and populate to build 
        return yzInvoice;
    }

    private static LocalDate parseDate(String dateString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        return LocalDate.parse(dateString, formatter);
    }
}
