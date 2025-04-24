package com.autowares.mongoose.coordinator.yooz;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import com.autowares.mongoose.model.yooz.YoozDocument;
import com.autowares.servicescommon.model.SystemType;
import com.autowares.servicescommon.util.PrettyPrint;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;

public class YoozToOpenCSVTest {

	@Test
	@Ignore
	public void testCreateTabDelimitedCsvFromYoozDocument() throws Exception {
		// Create a YoozDocument instance
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
		YoozDocument yoozDocument = new YoozDocument();
		yoozDocument.setAction("CREATE");
		yoozDocument.setVendorCode("V123");
		yoozDocument.setOrderNumber("O123");
		String date = dateFormat.format(new Date(System.currentTimeMillis() - 10 * 24 * 60 * 60 * 1000L));
		yoozDocument.setDocumentTimeStamp(date);
		yoozDocument.setDocumentTaxedAmount(new BigDecimal("1000.00"));
		yoozDocument.setCurrency("USD");
		yoozDocument.setOrderCreator("John Doe");
		yoozDocument.setOrderApprovers("Jane Doe");
		yoozDocument.setLineNumber("1");
//		yoozDocument.setProductCode(new BigDecimal("123456"));
		yoozDocument.setDescription("Sample Product");
		yoozDocument.setProductUnitPrice(new BigDecimal("100.00"));
		yoozDocument.setQuantityOrdered(10);
		yoozDocument.setQuantityReceived(8);
		yoozDocument.setQuantityInvoices(2);
		yoozDocument.setAmount(new BigDecimal("800.00"));
		yoozDocument.setDiscountedAmout(new BigDecimal("20.00"));
		yoozDocument.setTaxProfileCode("TP123");
		yoozDocument.setTaxAmount(new BigDecimal("80.00"));
		yoozDocument.setGlAccount("GL123");
		yoozDocument.setCostCenterChartsCodes("CC1");
		yoozDocument.setSubsidiary("Sub1");
//		yoozDocument.setSupplierProduct(new BigDecimal("654321"));
		yoozDocument.setHeader("Header1");
		yoozDocument.setLines("Line1");
		yoozDocument.setReceptionComment("Received well");
		String receptionDate = dateFormat.format(new Date(System.currentTimeMillis() + 10 * 24 * 60 * 60 * 1000L));
		yoozDocument.setReceptionDate(receptionDate); // 10 days later
		String deliveryDate = dateFormat.format(new Date(System.currentTimeMillis() + 10 * 24 * 60 * 60 * 1000L));
		yoozDocument.setPlannedDeliveryDate(deliveryDate); // 10 days later
		yoozDocument.setToBeRecevied("1");
		yoozDocument.setTypeOfPO("Standard");
		yoozDocument.setDeliveryAddress("Warehouse 1");
		yoozDocument.setInvoicingAddress("Office 1");
		yoozDocument.setYoozNumber("YN123");
		yoozDocument.setTypeOfLine("LineType");
		yoozDocument.setSublinesManagement("SubMgmt");
		yoozDocument.setBudgetPeriodCode("BP123");
		yoozDocument.setBudgetCode("BC123");
		String startDate = dateFormat.format(new Date(System.currentTimeMillis() - 10 * 24 * 60 * 60 * 1000L));
		yoozDocument.setStartDate(startDate); // 10 days ago
		String endDate = dateFormat.format(new Date(System.currentTimeMillis() + 20 * 24 * 60 * 60 * 1000L));
		yoozDocument.setEndDate(endDate); // 20 days later
		yoozDocument.setLabel("Test Label");
		yoozDocument.setVendorItemCode("VI123");

		// Write the YoozDocument instance to a CSV string
		StringWriter writer = new StringWriter();
		StatefulBeanToCsv<YoozDocument> beanToCsv = new StatefulBeanToCsvBuilder<YoozDocument>(writer).build();
		beanToCsv.write(yoozDocument);

		// Convert comma-delimited CSV to tab-delimited CSV
		String commaDelimitedCsv = writer.toString();
		String tabDelimitedCsv = commaDelimitedCsv.replace(",", "\t");

		// Print tab-delimited CSV output (for demonstration purposes)
		PrettyPrint.print("Output:");
		System.out.println(tabDelimitedCsv);

		// Format date for the expected output
		String formattedDate = dateFormat.format(new Date());
		String formattedPlannedDeliveryDate = dateFormat
				.format(new Date(System.currentTimeMillis() + 10 * 24 * 60 * 60 * 1000L));
		String formattedStartDate = dateFormat.format(new Date(System.currentTimeMillis() - 10 * 24 * 60 * 60 * 1000L));
		String formattedEndDate = dateFormat.format(new Date(System.currentTimeMillis() + 20 * 24 * 60 * 60 * 1000L));

		// Expected tab-delimited CSV output (adjust based on actual format and data)
		String expectedTabDelimitedCsvOutput = "\"CREATE\"\t\"V123\"\t\"O123\"\t\"AwiWarehouse\"\t\"" + formattedDate
				+ "\"\t\"1000.00\"\t\"USD\"\t\"John Doe\"\t\"Jane Doe\"\t\"1\"\t\"123456\"\t\"Sample Product\"\t\"100.00\"\t\"10\"\t\"8\"\t\"2\"\t\"800.00\"\t\"20.00\"\t\"TP123\"\t\"80.00\"\t\"GL123\"\t\"CC1\"\t\"C1\"\t\"Sub1\"\t\"654321\"\t\"Header1\"\t\"Line1\"\t\"Received well\"\t\""
				+ formattedDate + "\"\t\"" + formattedPlannedDeliveryDate
				+ "\"\t\"true\"\t\"Standard\"\t\"Warehouse 1\"\t\"Office 1\"\t\"YN123\"\t\"LineType\"\t\"SubMgmt\"\t\"BP123\"\t\"BC123\"\t\""
				+ formattedStartDate + "\"\t\"" + formattedEndDate + "\"\t\"Test Label\"\t\"VI123\"\n";

		// Replace newline with the system's newline character
		expectedTabDelimitedCsvOutput = expectedTabDelimitedCsvOutput.replace("\n", System.lineSeparator());
		
		PrettyPrint.print("Expected:");
		System.out.println(expectedTabDelimitedCsvOutput);

		// Assert the tab-delimited CSV output matches the expected output
		assertEquals(expectedTabDelimitedCsvOutput, tabDelimitedCsv);
	}

}
