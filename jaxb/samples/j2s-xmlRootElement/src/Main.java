/*
 * Copyright (c) 1997, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import address.PurchaseOrderType;
import address.USAddress;

public class Main {
    public static void main(String[] args) throws Exception {
        // A Ship to type
        USAddress shipto = new USAddress("Alice Smith", "123 Maple Street", 
        "Mill Valley", "CA", 90952);

        // A bill to type
        USAddress billto = new USAddress("Robert Smith", "8 Oak Avenue", 
        "Old Town", "PA", 95819);

        // A purchaseOrder
        PurchaseOrderType po = new PurchaseOrderType();
        po.billTo = billto;
        po.shipTo = shipto;

        // Demonstates shipping and billing data printed in the property
        // order defined by the propOrder annotation element in class 
        // USAddress.
        JAXBContext jc = JAXBContext.newInstance(PurchaseOrderType.class);
        Marshaller m = jc.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(po, System.out);
    }
}


