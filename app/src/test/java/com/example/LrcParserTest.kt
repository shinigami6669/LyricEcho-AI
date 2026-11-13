package com.example

import com.example.data.remote.LrclibService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test
    fun testParseLrcStringWithMudFlowChemicals() {
        val lrclibService = LrclibService()
        val testLrc = """
            [00:00.37] Just another party where the people, dance alone
            [00:06.44] Everybody's trying to have some fun
            [00:50.34] Just a tiny point lost in the universe
            [00:55.10] Shaking my knees
            [00:57.06] Turn me on, oh my
            [00:58.81] I still have a body
        """.trimIndent()

        val parsed = lrclibService.parseLrcString(testLrc)

        assertEquals(6, parsed.size)
        assertEquals("Just another party where the people, dance alone", parsed[0].original)
        assertEquals(370L, parsed[0].timeMs)

        assertEquals("Just a tiny point lost in the universe", parsed[2].original)
        assertEquals(50340L, parsed[2].timeMs)

        assertEquals("Turn me on, oh my", parsed[4].original)
        assertEquals(57060L, parsed[4].timeMs)
    }
}
