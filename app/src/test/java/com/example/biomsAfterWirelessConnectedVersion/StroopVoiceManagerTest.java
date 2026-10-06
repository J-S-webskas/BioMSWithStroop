package com.example.biomsAfterWirelessConnectedVersion;

import org.junit.Test;

import static org.junit.Assert.*;

public class StroopVoiceManagerTest {

    @Test
    public void testFindMatchingColorExact() {
        assertEquals("RED", StroopVoiceManager.findMatchingColor("RED"));
        assertEquals("BLUE", StroopVoiceManager.findMatchingColor("BLUE"));
        assertEquals("GREEN", StroopVoiceManager.findMatchingColor("GREEN"));
        assertEquals("YELLOW", StroopVoiceManager.findMatchingColor("YELLOW"));

        assertEquals("RED", StroopVoiceManager.findMatchingColor("red"));
        assertEquals("BLUE", StroopVoiceManager.findMatchingColor("blue"));
        assertEquals("GREEN", StroopVoiceManager.findMatchingColor("green"));
        assertEquals("YELLOW", StroopVoiceManager.findMatchingColor("yellow"));
    }

    @Test
    public void testFindMatchingColorSubstring() {
        assertEquals("RED", StroopVoiceManager.findMatchingColor("I think it is RED"));
        assertEquals("BLUE", StroopVoiceManager.findMatchingColor("It looks blue"));
        assertEquals("GREEN", StroopVoiceManager.findMatchingColor("The color is green today"));
        assertEquals("YELLOW", StroopVoiceManager.findMatchingColor("Definitely yellow"));
    }

    @Test
    public void testFindMatchingColorAliasesAndHomophones() {
        assertEquals("RED", StroopVoiceManager.findMatchingColor("read"));
        assertEquals("RED", StroopVoiceManager.findMatchingColor("rad"));
        assertEquals("BLUE", StroopVoiceManager.findMatchingColor("blew"));
        assertEquals("BLUE", StroopVoiceManager.findMatchingColor("blu"));
        assertEquals("GREEN", StroopVoiceManager.findMatchingColor("greene"));
        assertEquals("GREEN", StroopVoiceManager.findMatchingColor("grin"));
        assertEquals("YELLOW", StroopVoiceManager.findMatchingColor("yello"));
        assertEquals("YELLOW", StroopVoiceManager.findMatchingColor("yallow"));
    }

    @Test
    public void testFindMatchingColorFuzzy() {
        assertEquals("RED", StroopVoiceManager.findMatchingColor("rde"));
        assertEquals("BLUE", StroopVoiceManager.findMatchingColor("bloo"));
        assertEquals("GREEN", StroopVoiceManager.findMatchingColor("gren"));
        assertEquals("YELLOW", StroopVoiceManager.findMatchingColor("yelow"));
    }

    @Test
    public void testFindMatchingColorNoMatch() {
        assertNull(StroopVoiceManager.findMatchingColor(null));
        assertNull(StroopVoiceManager.findMatchingColor(""));
        assertNull(StroopVoiceManager.findMatchingColor("   "));
        assertNull(StroopVoiceManager.findMatchingColor("hello world"));
    }

    @Test
    public void testDamerauLevenshteinDistance() {
        assertEquals(0, StroopVoiceManager.getDamerauLevenshteinDistance("RED", "RED"));
        assertEquals(1, StroopVoiceManager.getDamerauLevenshteinDistance("RED", "RAD"));
        assertEquals(1, StroopVoiceManager.getDamerauLevenshteinDistance("RED", "RDE"));
        assertEquals(1, StroopVoiceManager.getDamerauLevenshteinDistance("BLUE", "BLU"));
        assertEquals(1, StroopVoiceManager.getDamerauLevenshteinDistance("GREEN", "GREN"));
        assertEquals(1, StroopVoiceManager.getDamerauLevenshteinDistance("YELLOW", "YELOW"));
    }
}
