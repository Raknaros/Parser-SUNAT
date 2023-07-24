package pe.impulsa.SUNATParser;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.xml.sax.SAXException;
import pe.impulsa.SUNATParser.service.Factura;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

@SpringBootTest
class SunatParserApplicationTests {



	@Test
	void contextLoads() throws ParserConfigurationException, IOException, SAXException {
		Factura factura = new Factura("/home/raknaros/xmls/");
		factura.facturas();

	}

}
