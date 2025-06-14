/*
 * Copyright (c) 1997, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

import java.math.BigDecimal;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBElement;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;

// import java content classes generated by binding compiler
import primer.po.*;

/*
 * $Id: Main.java,v 1.2 2009-11-11 14:17:31 pavel_bucek Exp $
 */
 
public class Main {
    
    // This sample application demonstrates how to construct value classes
    // and create a java content tree from scratch and marshal it
    // to XML data
    
    public static void main( String[] args ) {
        // create an empty PurchaseOrder
        PurchaseOrderType po = new PurchaseOrderType();
        
        // set the required orderDate attribute
        po.setOrderDate( getDate() );
        
        // create shipTo USAddress object
        USAddress shipTo = createUSAddress( "Alice Smith",
                                            "123 Maple Street",
                                            "Cambridge",
                                            "MA",
                                            "12345" );
                                            
        // set the required shipTo address 
        po.setShipTo( shipTo );
        
        // create billTo USAddress object
        USAddress billTo = createUSAddress( "Robert Smith",
                                            "8 Oak Avenue",
                                            "Cambridge",
                                            "MA",
                                            "12345" );
        
        // set the requred billTo address
        po.setBillTo( billTo );
                                            
        // create an empty Items object
        Items items = new Items();
        
        // get a reference to the ItemType list
        List<Items.Item> itemList = items.getItem();
        
        // start adding ItemType objects into it
        itemList.add( createItem( "Nosferatu - Special Edition (1929)", 
                                  5, 
                                  new BigDecimal( "19.99" ), 
                                  null,
                                  null,
                                  "242-NO" ) );
        itemList.add( createItem( "The Mummy (1959)", 
                                  3, 
                                  new BigDecimal( "19.98" ), 
                                  null,
                                  null,
                                  "242-MU" ) );
        itemList.add( createItem( "Godzilla and Mothra: Battle for Earth/Godzilla vs. King Ghidora", 
                                  3, 
                                  new BigDecimal( "27.95" ), 
                                  null,
                                  null,
                                  "242-GZ" ) );
        
        // set the required Items list
        po.setItems( items );
       
        // create an element for marshalling
        JAXBElement<PurchaseOrderType> poElement = (new ObjectFactory()).createPurchaseOrder(po);

        // create a Marshaller and marshal to System.out
        JAXB.marshal( poElement, System.out );
    }
    
    public static USAddress createUSAddress(  String name, String street,
                                              String city, String state,
                                              String zip ) {
    
        // create an empty USAddress objects                                             
        USAddress address = new USAddress();
       
        // set properties on it
        address.setName( name );
        address.setStreet( street );
        address.setCity( city );
        address.setState( state );
        address.setZip( new BigDecimal( zip ) );
        
        // return it
        return address;
    }
    
    public static Items.Item createItem( String productName,
                                         int quantity,
                                         BigDecimal price,
                                         String comment,
                                         XMLGregorianCalendar shipDate,
                                         String partNum ) {
   
        // create an empty ItemType object
        Items.Item item = new Items.Item();
        
        // set properties on it
        item.setProductName( productName );
        item.setQuantity( quantity );
        item.setUSPrice( price );
        item.setComment( comment );
        item.setShipDate( shipDate );
        item.setPartNum( partNum );
        
        // return it
        return item;
    }
                                           
                                    
    private static XMLGregorianCalendar getDate() {
	try {
	    return DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar());
	} catch (DatatypeConfigurationException e) {
	    throw new Error(e);
	}
    }
}

