package com.autowares.mongoose.coordinator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.autowares.ServiceDiscovery;
import com.autowares.servicescommon.util.PrettyPrint;

public class SendSupplyChainIdsToDelete {

//	@Test
	public void testSendNumbersToEndpoint() throws Exception {
		// List of numbers to send
		List<String> numbers = List.of("162188676", "162648835", "162541223", "162541232", "162190667", "162190684",
				"162542154", "162542175", "162541136", "162541156", "162189097", "162189106", "162541898", "162541900",
				"162189880", "162189890", "162189038", "162189052", "162541010", "162541022", "162188453", "162188458",
				"162189160", "162189204", "162541195", "162541204", "162188942", "162188983", "162541381", "162541392",
				"162188893", "162188913", "162188387", "162188400", "162189779", "162189796", "162188807", "162188814",
				"162189677", "162189726", "162190375", "162190406", "162189453", "162189456", "162188469", "162188480",
				"162541336", "162541344", "162189842", "162189870", "162190267", "162190282", "162541711", "162541740",
				"162189070", "162189088", "162541931", "162541952", "162188739", "162188776", "162189411", "162189434",
				"162542203", "162542218", "162189347", "162189376", "162541972", "162541978", "162188912", "162188918",
				"162542068", "162542096", "162190323", "162190330", "162190820", "162190824", "162542119", "162542141",
				"162190239", "162190252", "162190463", "162190502", "162190535", "162190568", "162189618", "162189628",
				"162189442", "162189450", "162190786", "162190816", "162542124", "162542146", "162190209", "162190226",
				"162189486", "162189496", "162190340", "162190362", "162188367", "162188374", "162541771", "162541802",
				"162541264", "162541280", "162188959", "162189000", "162541253", "162541269", "162189111", "162189116",
				"162190582", "162190596", "162189739", "162189752", "162541988", "162541998", "162541905", "162541910",
				"162189534", "162189572", "162541166", "162541176", "162188610", "162188626", "162189902", "162189914",
				"162189239", "162189276", "162189275", "162189278", "162541959", "162541966", "162188788", "162188800",
				"162190859", "162190872", "162189757", "162189762", "162541829", "162541850", "162542167", "162542188",
				"162190299", "162190316", "162190393", "162190424", "162190146", "162190192", "162190632", "162190650",
				"162190018", "162190024", "162189466", "162189476", "162188410", "162188420", "162190720", "162190756",
				"162188846", "162188878", "162541578", "162541585", "162190062", "162190100", "162188434", "162188448",
				"162189965", "162189988", "162190346", "162190361", "162541520", "162541540", "162190451", "162190490",
				"162188563", "162188586", "162190605", "162190614", "162189304", "162189330", "162188590", "162188594",
				"162541051", "162541080", "162541323", "162541328", "162189034", "162189048", "162189019", "162189024",
				"162542251", "162542268", "162541494", "162541500", "162541309", "162541318", "162541805", "162541808",
				"162188521", "162188534", "162189590", "162189608", "162542226", "162542234", "162188664", "162188702",
				"162188598", "162188614", "162541440", "162541488", "162188360", "162646307", "162541290", "162541300",
				"162189805", "162189814", "162541240", "162541248", "162541181", "162541186", "162541862", "162541874",
				"162190203", "162190220", "162542016", "162542034", "162190835", "162190846", "162188494", "162188508",
				"162190521", "162190554", "162541618", "162541648", "162541357", "162541370", "162541761", "162541792",
				"162541098", "162541116", "162189359", "162189388", "162189928", "162189942", "162541885", "162541896",
				"162541209", "162541214", "162189007", "162189014", "162541665", "162541682", "162190000", "162190012",
				"162188537", "162188540");

		/*
		 * Query used to get list of supply chain invoice ids that needed to be deleted so the
		 * invoice edi documents could be reprocessed. 
		 * -- Step 1: Get the list of
		 * supply_chain_ids WITH supply_chain_list AS ( SELECT pg.supply_chain_id FROM
		 * source_document_entity sd JOIN procurement_groups pg ON sd.source_document_id
		 * = pg.source_document_id WHERE sd.creation_time > '2024-06-19' AND
		 * sd.document_type = '1' AND sd.from_party = '150133823' )
		 * 
		 * -- Step 2: Use the list to fetch source_document_ids with document_type of 1
		 * SELECT pg.source_document_id FROM procurement_groups pg JOIN
		 * source_document_entity sd ON pg.source_document_id = sd.source_document_id
		 * WHERE pg.supply_chain_id IN (SELECT supply_chain_id FROM supply_chain_list)
		 * AND sd.document_type = 1;
		 */
		// Create an HttpClient instance
		HttpClient client = HttpClient.newHttpClient();

		for (String number : numbers) {
			// Create an HttpRequest instance for each number
			try {
				ServiceDiscovery.setDomain("consul");
//				URL url = ServiceDiscovery.resolveServiceURL("supplychain");
				String url = null;
				url = "mesosps-b3.autowares.com:31271";
				URI uri = new URI("http://" + url + "/supplychain/sourcedocument/" + number);
				HttpRequest request = HttpRequest.newBuilder().uri(uri).header("accept", "*/*").DELETE().build();

				// Send the request and get the response
				HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

				// Assert that the response status code is 200 (OK)
				PrettyPrint.print(response.statusCode() + " " + number + "  " + uri);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Test
	public void testDocumentContextToYoozDocument() throws Exception {

		// Create an HttpClient instance
		HttpClient client = HttpClient.newHttpClient();

		try {
			ServiceDiscovery.setDomain("consul");
				URL url = ServiceDiscovery.resolveServiceURL("supplychain");
//			String url = null;
//			url = "mesosps-b3.autowares.com:31271";
			URI uri = new URI(url + "/supplychain/sourcedocument/?documentId=1255575568800313344");
			HttpRequest request = HttpRequest.newBuilder().uri(uri).header("accept", "*/*").build();

			// Send the request and get the response
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			// Assert that the response status code is 200 (OK)
			String body = response.body();
//				body.
//			DocumentContext dc = body.getDocument();
//			documentContextToYoozDocument(dc);
			PrettyPrint.print(body);
			PrettyPrint.print(response.statusCode() + " " + uri);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
