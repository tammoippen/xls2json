package xls2json

import kotlin.test.Test
import kotlin.test.assertEquals

@Test
fun `rstrip of an empty list returns an empty list`() {
    assertEquals(listOf<Any?>(), rstrip(listOf<Any?>()))
}

@Test
fun `rstrip of an list of only null returns an empty list`() {
    assertEquals(listOf<Any?>(), rstrip(listOf<Any?>(null, null)))
}
