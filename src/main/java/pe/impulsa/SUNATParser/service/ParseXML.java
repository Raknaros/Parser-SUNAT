package pe.impulsa.SUNATParser.service;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.stereotype.Service;

import pe.impulsa.SUNATParser.impulsadb.models.Iinventario;
import pe.impulsa.SUNATParser.impulsadb.models.Iventas;
import pe.impulsa.SUNATParser.impulsadb.models.Icompras;
import pe.impulsa.SUNATParser.impulsadb.repo.IcomprasRepo;
import pe.impulsa.SUNATParser.impulsadb.repo.IinventarioRepo;
import pe.impulsa.SUNATParser.impulsadb.repo.IventasRepo;
import pe.impulsa.SUNATParser.pojo.BoletaVenta;
import pe.impulsa.SUNATParser.pojo.Factura;
import pe.impulsa.SUNATParser.pojo.NotaCredito;
import pe.impulsa.SUNATParser.pojo.NotaDebito;
import pe.impulsa.SUNATParser.pojo.xmlelements.InvoiceLine;
import pe.impulsa.SUNATParser.pojo.xmlelements.taxtotal.TaxSubtotal;

import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class ParseXML extends ExtractXml {
    private final DataMethods dataMethods;
    private static DateTimeFormatter anomesdia = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static DateTimeFormatter anomes = DateTimeFormatter.ofPattern("yyyyMM");
    private final IventasRepo iventasRepo;
    private final IcomprasRepo icomprasRepo;
    private final IinventarioRepo iinventarioRepo;
    Integer i=0;

    public ParseXML(DataMethods dataMethods, IventasRepo iventasRepo, IcomprasRepo icomprasRepo, IinventarioRepo iinventarioRepo) {
        this.dataMethods = dataMethods;
        this.iventasRepo = iventasRepo;
        this.icomprasRepo = icomprasRepo;
        this.iinventarioRepo = iinventarioRepo;
    }

    public Integer facturas(String ruta) throws JAXBException {
        for (Map.Entry<String, String> entry : listaXml(ruta).entrySet()){
            if(entry.getKey().startsWith("FACTURA")){
                StringReader content = new StringReader(entry.getValue());
                JAXBContext jaxbContext = JAXBContext.newInstance(Factura.class);
                Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                Factura e=(Factura) jaxbUnmarshaller.unmarshal(content);
                String cui=Long.toHexString(Long.valueOf(e.getAccountingSupplierParty().getParty().getPartyIdentification().getId().getValue()))+e.getInvoiceTypeCode().getValor()+e.getId().split("-")[0].trim()+e.getId().split("-")[1].trim();
                if(dataMethods.verifyxml(Integer.valueOf(anomes.format(e.getIssuedate())),cui)){
                    BigDecimal totalBaseImponible=new BigDecimal(0);
                    BigDecimal totalDescuento=new BigDecimal(0);
                    BigDecimal totalIgv=new BigDecimal(0);
                    BigDecimal totalIsc=new BigDecimal(0);
                    BigDecimal totalOtrosCargos=new BigDecimal(0);
                    BigDecimal totalExonerado=new BigDecimal(0);
                    BigDecimal totalInafecto=new BigDecimal(0);
                    BigDecimal totalGratuito=new BigDecimal(0);
                    BigDecimal totalOtrosTributos=new BigDecimal(0);
                    BigDecimal subTotalVenta=new BigDecimal(0);
                    BigDecimal totalAdelanto=new BigDecimal(0);
                    if(dataMethods.verifysupplier(Long.valueOf(e.getAccountingSupplierParty().getParty().getPartyIdentification().getId().getValue()))){
                        //INGRESAR VENTA

                        Iventas venta=new Iventas();
                        venta.setRuc(Long.valueOf(e.getAccountingSupplierParty().getParty().getPartyIdentification().getId().getValue()));
                        venta.setPeriodoTributario(Integer.valueOf(anomes.format(e.getIssuedate())));
                        venta.setTipoOperacion(1);
                        venta.setTipoComprobante(Integer.valueOf(e.getInvoiceTypeCode().getValor()));
                        venta.setFechaEmision(Date.valueOf(e.getIssuedate()));
                        venta.setFechaVencimiento(Date.valueOf("2023-08-29"));
                        venta.setNumeroSerie(e.getId().split("-")[0].trim());
                        venta.setNumeroCorrelativo(e.getId().split("-")[1].trim());
                        venta.setTipoDocumento(e.getAccountingCustomerParty().getParty().getPartyIdentification().getId().getSchemeID());
                        venta.setNumeroDocumento(e.getAccountingCustomerParty().getParty().getPartyIdentification().getId().getValue());
                        for(TaxSubtotal t:e.getTaxTotal().getTaxSubtotal()){
                            switch (t.getTaxCategory().getTaxScheme().getId().getValue()) {
                                case "1000" -> {
                                    totalBaseImponible = t.getTaxableAmount().getValor();
                                    totalIgv = t.getTaxAmount().getValor();
                                }
                                case "2000" -> totalIsc = t.getTaxAmount().getValor();
                                case "9999" -> totalOtrosTributos = t.getTaxAmount().getValor();
                                case "9998" -> totalInafecto = t.getTaxableAmount().getValor();
                                case "9997" -> totalExonerado = t.getTaxableAmount().getValor();
                                case "9996" -> {
                                    totalGratuito = t.getTaxableAmount().getValor();
                                    totalIgv = t.getTaxAmount().getValor();
                                }
                            }
                        }
                        subTotalVenta=e.getLegalMonetaryTotal().getLineExtensionAmount().getValor();
                        totalDescuento=e.getLegalMonetaryTotal().getAllowanceTotalAmount().getValor();
                        totalOtrosCargos=e.getLegalMonetaryTotal().getChargeTotalAmount().getValor();
                        totalAdelanto=e.getLegalMonetaryTotal().getPrepaidAmount().getValor();
                        //Si Base imponible no es null y suma de exonerado e inafecto no es null ni 0 destino es 3
                        //Si base imponible es null y sumat de exonerado e inafecto no es null ni 0 destino 2
                        //Si base imponible no es null y suma de exonerado e inafecto es null o 0 destino 1
                        if(totalBaseImponible.compareTo(BigDecimal.ZERO)!=0&&totalExonerado.add(totalInafecto).compareTo(BigDecimal.ZERO)!=0){
                            venta.setDestino(3);
                            venta.setValor(totalBaseImponible);
                            venta.setIgv(totalIgv);
                            venta.setIsc(totalIsc);
                            venta.setOtrosCargos(totalOtrosCargos.add(totalOtrosTributos).add(totalInafecto).add(totalExonerado));
                        }else if(totalBaseImponible.compareTo(BigDecimal.ZERO)==0){
                            venta.setDestino(2);
                            venta.setValor(totalExonerado.add(totalInafecto));
                            venta.setIgv(new BigDecimal(0));
                            venta.setIsc(totalIsc);
                            venta.setOtrosCargos(totalOtrosCargos.add(totalOtrosTributos));
                        }else if(totalExonerado.add(totalInafecto).compareTo(BigDecimal.ZERO)==0){
                            venta.setDestino(1);
                            venta.setValor(totalBaseImponible);
                            venta.setIgv(totalIgv);
                            venta.setIsc(totalIsc);
                            venta.setOtrosCargos(totalOtrosCargos.add(totalOtrosTributos));
                        }
                        //venta.setIcbp(new BigDecimal(0));
                        venta.setTipoMoneda(e.getDocumentCurrencyCode().getValor());
                        //venta.setTasaDetraccion(null);
                        //venta.setTasaPercepcion(null);
                        venta.setObservaciones("PRUEBA");
                        iventasRepo.save(venta);
                        //INGRESAR INVENTARIO
                        Iinventario inventario=new Iinventario();
                        for(InvoiceLine i:e.getInvoiceLine()){
                            inventario.setRuc(Long.valueOf(e.getAccountingSupplierParty().getParty().getPartyIdentification().getId().getValue()));
                            inventario.setTipoOperacion(1);
                            inventario.setPeriodoTributario(Integer.valueOf(anomes.format(e.getIssuedate())));
                            inventario.setFecha(Date.valueOf(e.getIssuedate()));
                            inventario.setCodigoItem(i.getItem().getSellersItemIdentification().getId());
                            inventario.setDescripcion(i.getItem().getDescription());
                            inventario.setUnidadMedida(i.getInvoicedQuantity().getUnitCode());
                            inventario.setCantidad(i.getInvoicedQuantity().getValor());
                            inventario.setPrecioUnitario(i.getPrice().getPriceAmount().getValor());
                            //inventario.setIgv(i.getTaxTotal().getTaxSubtotal()[0].getTaxCaterogy().getPercent());
                            try {
                                inventario.setTipoDocumentoReferencia(Integer.valueOf(e.getDespatchDocumentReference().getDocumentTypeCode()));
                                inventario.setNumeroDocumentoReferencia(e.getDespatchDocumentReference().getId());
                            }catch (NullPointerException ex){
                                inventario.setTipoDocumentoReferencia(Integer.valueOf(e.getInvoiceTypeCode().getValor()));
                                inventario.setNumeroDocumentoReferencia(e.getId());
                            }
                            inventario.setCuiRelacionado(cui);
                            inventario.setObservaciones("PRUEBA");
                            iinventarioRepo.save(inventario);
                        }
                    }if(dataMethods.verifycustomer(Long.valueOf(e.getAccountingCustomerParty().getParty().getPartyIdentification().getId().getValue()))){
                        //INGRESAR COMPRA
                        Icompras compra=new Icompras();
                        compra.setRuc(Long.valueOf(e.getAccountingCustomerParty().getParty().getPartyIdentification().getId().getValue()));
                        compra.setPeriodoTributario(Integer.valueOf(anomes.format(e.getIssuedate())));
                        compra.setTipoOperacion(2);
                        compra.setTipoComprobante(Integer.valueOf(e.getInvoiceTypeCode().getValor()));
                        compra.setFechaEmision(Date.valueOf(e.getIssuedate()));
                        compra.setFechaVencimiento(Date.valueOf("2023-08-29"));
                        compra.setNumeroSerie(e.getId().split("-")[0].trim());
                        compra.setNumeroCorrelativo(e.getId().split("-")[1].trim());
                        compra.setTipoDocumento(e.getAccountingSupplierParty().getParty().getPartyIdentification().getId().getSchemeID());
                        compra.setNumeroDocumento(e.getAccountingSupplierParty().getParty().getPartyIdentification().getId().getValue());
                        for(TaxSubtotal t:e.getTaxTotal().getTaxSubtotal()){
                            switch (t.getTaxCategory().getTaxScheme().getId().getValue()) {
                                case "1000" -> {
                                    totalBaseImponible = t.getTaxableAmount().getValor();
                                    totalIgv = t.getTaxAmount().getValor();
                                }
                                case "2000" -> totalIsc = t.getTaxAmount().getValor();
                                case "9999" -> totalOtrosTributos = t.getTaxAmount().getValor();
                                case "9998" -> totalInafecto = t.getTaxableAmount().getValor();
                                case "9997" -> totalExonerado = t.getTaxableAmount().getValor();
                                case "9996" -> {
                                    totalGratuito = t.getTaxableAmount().getValor();
                                    totalIgv = t.getTaxAmount().getValor();
                                }
                            }
                        }
                        subTotalVenta=e.getLegalMonetaryTotal().getLineExtensionAmount().getValor();
                        totalDescuento=e.getLegalMonetaryTotal().getAllowanceTotalAmount().getValor();
                        totalOtrosCargos=e.getLegalMonetaryTotal().getChargeTotalAmount().getValor();
                        totalAdelanto=e.getLegalMonetaryTotal().getPrepaidAmount().getValor();
                        //Si Base imponible no es null y suma de exonerado e inafecto no es null ni 0 destino es 3
                        //Si base imponible es null y sumat de exonerado e inafecto no es null ni 0 destino 2
                        //Si base imponible no es null y suma de exonerado e inafecto es null o 0 destino 1
                        if(totalBaseImponible.compareTo(BigDecimal.ZERO)!=0&&totalExonerado.add(totalInafecto).compareTo(BigDecimal.ZERO)!=0){
                            compra.setDestino(5);
                            compra.setValor(totalBaseImponible);
                            compra.setIgv(totalIgv);
                            compra.setIsc(totalIsc);
                            compra.setOtrosCargos(totalOtrosCargos.add(totalOtrosTributos).add(totalInafecto).add(totalExonerado));
                        }else if(totalBaseImponible.compareTo(BigDecimal.ZERO)==0){
                            compra.setDestino(4);
                            compra.setValor(totalExonerado.add(totalInafecto));
                            compra.setIgv(new BigDecimal(0));
                            compra.setIsc(totalIsc);
                            compra.setOtrosCargos(totalOtrosCargos.add(totalOtrosTributos));
                        }else if(totalExonerado.add(totalInafecto).compareTo(BigDecimal.ZERO)==0){
                            compra.setDestino(1);
                            compra.setValor(totalBaseImponible);
                            compra.setIgv(totalIgv);
                            compra.setIsc(totalIsc);
                            compra.setOtrosCargos(totalOtrosCargos.add(totalOtrosTributos));
                        }
                        compra.setIcbp(new BigDecimal(0));
                        compra.setTipoMoneda(e.getDocumentCurrencyCode().getValor());
                        //compra.setTasaDetraccion(null);
                        //compra.setTasaPercepcion(null);
                        compra.setObservaciones("PRUEBA");
                        icomprasRepo.save(compra);
                        //INGRESAR INVENTARIO
                        Iinventario inventario=new Iinventario();
                        for(InvoiceLine i:e.getInvoiceLine()){

                            inventario.setTipoOperacion(2);
                            inventario.setPeriodoTributario(Integer.valueOf(anomes.format(e.getIssuedate())));
                            inventario.setFecha(Date.valueOf(e.getIssuedate()));
                            inventario.setCodigoItem(i.getItem().getSellersItemIdentification().getId());
                            inventario.setDescripcion(i.getItem().getDescription());
                            inventario.setUnidadMedida(i.getInvoicedQuantity().getUnitCode());
                            inventario.setCantidad(i.getInvoicedQuantity().getValor());
                            inventario.setPrecioUnitario(i.getPrice().getPriceAmount().getValor());
                            try {
                                inventario.setTipoDocumentoReferencia(Integer.valueOf(e.getDespatchDocumentReference().getDocumentTypeCode()));
                                inventario.setNumeroDocumentoReferencia(e.getDespatchDocumentReference().getId());
                            }catch (NullPointerException ex){
                                inventario.setTipoDocumentoReferencia(Integer.valueOf(e.getInvoiceTypeCode().getValor()));
                                inventario.setNumeroDocumentoReferencia(e.getId());
                            }
                            inventario.setCuiRelacionado(cui);
                            inventario.setObservaciones("PRUEBA");
                            iinventarioRepo.save(inventario);
                        }
                    }
                }

            }
        }
        return i;
    }
    public Integer notasCredito(String ruta) throws JAXBException {
        for (Map.Entry<String, String> entry : listaXml(ruta).entrySet()){
            if(entry.getKey().startsWith("NOTACREDITO")){
                StringReader content = new StringReader(entry.getValue());
                JAXBContext jaxbContext = JAXBContext.newInstance(NotaCredito.class);
                Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                NotaCredito e=(NotaCredito) jaxbUnmarshaller.unmarshal(content);
                //System.out.println(doc.getDocumentElement().getNodeName());
            }
        }
        return i;
    }
    public Integer notasDebito(String ruta) throws JAXBException {
        for (Map.Entry<String, String> entry : listaXml(ruta).entrySet()){
            if(entry.getKey().startsWith("NOTADEBITO")){
                StringReader content = new StringReader(entry.getValue());
                JAXBContext jaxbContext = JAXBContext.newInstance(NotaDebito.class);
                Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                NotaDebito e=(NotaDebito) jaxbUnmarshaller.unmarshal(content);
                //System.out.println(doc.getDocumentElement().getNodeName());
            }
        }
        return i;
    }
    public Integer boletasVenta(String ruta) throws JAXBException {
        for (Map.Entry<String, String> entry : listaXml(ruta).entrySet()){
            if(entry.getKey().startsWith("BOLETA")){
                StringReader content = new StringReader(entry.getValue());
                JAXBContext jaxbContext = JAXBContext.newInstance(BoletaVenta.class);
                Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                BoletaVenta e=(BoletaVenta) jaxbUnmarshaller.unmarshal(content);
                //System.out.println(doc.getDocumentElement().getNodeName());
            }
        }
        return i;
    }
}
