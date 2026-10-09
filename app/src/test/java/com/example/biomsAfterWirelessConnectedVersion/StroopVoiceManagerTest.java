package com.example.biomsAfterWirelessConnectedVersion;

import org.junit.Test;
import static org.junit.Assert.*;

public class StroopVoiceManagerTest {

    @Test
    public void testMatchColorWord_ExactMatch() {
        assertEquals("RED", StroopVoiceManager.matchColorWord("red"));
        assertEquals("BLUE", StroopVoiceManager.matchColorWord("BLUE"));
        assertEquals("GREEN", StroopVoiceManager.matchColorWord("green"));
        assertEquals("YELLOW", StroopVoiceManager.matchColorWord("Yellow"));
    }

    @Test
    public void testMatchColorWord_InSentence() {
        assertEquals("RED", StroopVoiceManager.matchColorWord("I think it is red."));
        assertEquals("BLUE", StroopVoiceManager.matchColorWord("the color is blue now"));
        assertEquals("GREEN", StroopVoiceManager.matchColorWord("green!"));
    }

    @Test
    public void testMatchColorWord_SubstringNoFalsePositive() {
        assertNull(StroopVoiceManager.matchColorWord("tired"));
        assertNull(StroopVoiceManager.matchColorWord("predict"));
        assertNull(StroopVoiceManager.matchColorWord("blueprint"));
        assertNull(StroopVoiceManager.matchColorWord("evergreen"));
    }

    @Test
    public void testMatchColorWord_InvalidAndEmptyInput() {
        assertNull(StroopVoiceManager.matchColorWord(null));
        assertNull(StroopVoiceManager.matchColorWord(""));
        assertNull(StroopVoiceManager.matchColorWord("   "));
        assertNull(StroopVoiceManager.matchColorWord("hello world 123"));
    }
}
