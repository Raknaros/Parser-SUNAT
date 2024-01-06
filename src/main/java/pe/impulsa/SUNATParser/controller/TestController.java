package pe.impulsa.SUNATParser.controller;

import jakarta.transaction.Transactional;
import jakarta.xml.bind.JAXBException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.xml.sax.SAXException;
import pe.impulsa.SUNATParser.service.DataMethods;
import pe.impulsa.SUNATParser.service.ParseXML;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@RestController
@RequestMapping("/test")
public class TestController {
    @Autowired
    DataMethods dataMethods;
    @Autowired
    ParseXML parseXML;

    @GetMapping("/facturas")
    @Transactional
    public void parsearFacturas(@RequestParam String a) throws JAXBException{

        parseXML.parse("D:\\XMLSUNAT\\"+a);
    }
}
