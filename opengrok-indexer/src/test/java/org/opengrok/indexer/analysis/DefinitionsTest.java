/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * See LICENSE.txt included in this distribution for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */

/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
 * Portions Copyright (c) 2018, Chris Fraire <cfraire@me.com>.
 */

package org.opengrok.indexer.analysis;

import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author austvik
 */
public class DefinitionsTest {

    public DefinitionsTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getSymbols method, of class Definitions.
     */
    @Test
    public void getSymbols() {
        Definitions instance = new Definitions();
        Set<String> result = instance.getSymbols();
        assertNotNull(result);
        assertEquals(result.size(), 0);
        instance.addTag(1, "found", "", "", 0, 0);
        result = instance.getSymbols();
        assertNotNull(result);
        assertEquals(result.size(), 1);
    }

    /**
     * Test of hasSymbol method, of class Definitions.
     */
    @Test
    public void hasSymbol() {
        Definitions instance = new Definitions();
        instance.addTag(1, "found", "", "", 0, 0);
        assertFalse(instance.hasSymbol("notFound"));
        assertTrue(instance.hasSymbol("found"));
    }

    /**
     * Test of hasDefinitionAt method, of class Definitions.
     */
    @Test
    public void hasDefinitionAt() {
        Definitions instance = new Definitions();
        String[] type = new String[1];
        type[0] = "";
        instance.addTag(1, "found", "", "", 0, 0);
        assertFalse(instance.hasDefinitionAt("found", 0, type));
        assertTrue(instance.hasDefinitionAt("found", 1, type));
        assertFalse(instance.hasDefinitionAt("found", 2, type));
        assertFalse(instance.hasDefinitionAt("notFound", 0, type));
        assertFalse(instance.hasDefinitionAt("notFound", 1, type));
    }

    /**
     * Test of occurrences method, of class Definitions.
     */
    @Test
    public void occurrences() {
        Definitions instance = new Definitions();
        instance.addTag(1, "one", "", "", 0, 0);
        instance.addTag(1, "two", "", "", 0, 0);
        instance.addTag(3, "two", "", "", 0, 0);
        assertEquals(instance.occurrences("one"), 1);
        assertEquals(instance.occurrences("two"), 2);
        assertEquals(instance.occurrences("notFound"), 0);
    }

    /**
     * Test of numberOfSymbols method, of class Definitions.
     */
    @Test
    public void numberOfSymbols() {
        Definitions instance = new Definitions();
        assertEquals(instance.numberOfSymbols(), 0);
        instance.addTag(1, "one", "", "", 0, 0);
        assertEquals(instance.numberOfSymbols(), 1);
        instance.addTag(1, "two", "", "", 0, 0);
        instance.addTag(3, "two", "", "", 0, 0);
        assertEquals(instance.numberOfSymbols(), 2);
    }

    /**
     * Test of getTags method, of class Definitions.
     */
    @Test
    public void getTags() {
        Definitions instance = new Definitions();
        assertEquals(instance.getTags().size(), 0);
        instance.addTag(1, "one", "", "", 0, 0);
        assertEquals(instance.getTags().size(), 1);
        instance.addTag(1, "two", "", "", 0, 0);
        assertEquals(instance.getTags().size(), 2);
        instance.addTag(3, "two", "", "", 0, 0);
        assertEquals(instance.getTags().size(), 3);
    }

    /**
     * Test of addTag method, of class Definitions.
     */
    @Test
    public void addTag() {
        Definitions instance = new Definitions();
        assertEquals(instance.getTags().size(), 0);
        instance.addTag(1, "one", "", "", 0, 0);
        assertEquals(instance.getTags().size(), 1);
    }

    /**
     * Test of serialize method, of class Definitions.
     */
    @Test
    public void serialize() throws Exception {
        Definitions instance = new Definitions();
        instance.addTag(1, "one", "", "", 0, 0);
        byte[] serial = instance.serialize();
        Definitions instance2 = Definitions.deserialize(serial);
        assertEquals(instance.getTags().size(), instance2.getTags().size());
        assertEquals(instance.getSymbols().size(), instance2.getSymbols().size());
    }

}
