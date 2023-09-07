package pe.impulsa.SUNATParser;

import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.xml.sax.SAXException;
import pe.impulsa.SUNATParser.impulsadb.repo.IEntitiesRepo;
import pe.impulsa.SUNATParser.parserdb.repo.PEntitiesRepo;
import pe.impulsa.SUNATParser.service.DataMethods;
import pe.impulsa.SUNATParser.service.Factura;

import javax.xml.crypto.Data;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

@SpringBootTest
class SunatParserApplicationTests {
	@Autowired
	Factura factura;
	@Autowired
	IEntitiesRepo Ientitiesrepo;
	@Autowired
	PEntitiesRepo Pentitiesrepo;
	@Autowired
	DataMethods dataMethods;

	@Test
	void contextLoads() throws ParserConfigurationException, IOException, SAXException {
		//dataMethods.resetAllEntities();
		//factura.facturas("D:\\SUNAT-Parser\\xmls");
		Ientitiesrepo.findAllByActivoAndObservacionesIsNot(true,"Problema Autenticacion").forEach(IEntities -> System.out.println(IEntities.getNumeroDocumento()));
	}
}