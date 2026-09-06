package com.pratone.app.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceCommandParserTest {

    @Test
    fun `play variants resolve to PLAY`() {
        listOf("play", "PLAY", "play music", "please play", "start music", "resume").forEach {
            assertEquals("failed for input: $it", VoiceCommand.PLAY, VoiceCommandParser.parse(it))
        }
    }

    @Test
    fun `pause variants resolve to PAUSE`() {
        listOf("pause", "pause the music", "stop the music").forEach {
            assertEquals("failed for input: $it", VoiceCommand.PAUSE, VoiceCommandParser.parse(it))
        }
    }

    @Test
    fun `next variants resolve to NEXT`() {
        listOf("next", "next song", "skip", "skip this song", "skip song").forEach {
            assertEquals("failed for input: $it", VoiceCommand.NEXT, VoiceCommandParser.parse(it))
        }
    }

    @Test
    fun `previous variants resolve to PREVIOUS`() {
        listOf("previous", "back", "go back", "last song").forEach {
            assertEquals("failed for input: $it", VoiceCommand.PREVIOUS, VoiceCommandParser.parse(it))
        }
    }

    @Test
    fun `bare stop resolves to STOP not PAUSE`() {
        assertEquals(VoiceCommand.STOP, VoiceCommandParser.parse("stop"))
    }

    @Test
    fun `shuffle and repeat`() {
        assertEquals(VoiceCommand.SHUFFLE, VoiceCommandParser.parse("shuffle"))
        assertEquals(VoiceCommand.REPEAT, VoiceCommandParser.parse("repeat"))
        assertEquals(VoiceCommand.REPEAT, VoiceCommandParser.parse("loop"))
    }

    @Test
    fun `unknown commands return UNKNOWN`() {
        listOf("what's the weather", "hello there", "banana", "").forEach {
            assertEquals("failed for input: $it", VoiceCommand.UNKNOWN, VoiceCommandParser.parse(it))
        }
    }

    @Test
    fun `empty recognition result returns UNKNOWN`() {
        assertEquals(VoiceCommand.UNKNOWN, VoiceCommandParser.parse(""))
        assertEquals(VoiceCommand.UNKNOWN, VoiceCommandParser.parse("   "))
    }

    @Test
    fun `punctuation is ignored`() {
        assertEquals(VoiceCommand.PLAY, VoiceCommandParser.parse("play!"))
        assertEquals(VoiceCommand.NEXT, VoiceCommandParser.parse("next, please."))
        assertEquals(VoiceCommand.PAUSE, VoiceCommandParser.parse("pause the music."))
    }

    @Test
    fun `extra whitespace is ignored`() {
        assertEquals(VoiceCommand.PLAY, VoiceCommandParser.parse("   play    "))
        assertEquals(VoiceCommand.NEXT, VoiceCommandParser.parse("next   song"))
    }

    @Test
    fun `case insensitivity`() {
        assertEquals(VoiceCommand.PLAY, VoiceCommandParser.parse("PlAy"))
        assertEquals(VoiceCommand.NEXT, VoiceCommandParser.parse("NEXT SONG"))
    }
}
