package com.naturalpad.hammerandplanks

import kotlin.test.Test
import kotlin.test.assertEquals

class LanguagesTest {
    @Test fun `francais du systeme donne fr`() = assertEquals("fr", Languages.resolve("fr-FR"))
    @Test fun `variante regionale ramenee a la langue`() = assertEquals("fr", Languages.resolve("fr-CA"))
    @Test fun `anglais donne en`() = assertEquals("en", Languages.resolve("en-US"))
    @Test fun `format java avec underscore et majuscules`() = assertEquals("fr", Languages.resolve("FR_be"))
    @Test fun `langue seule sans region`() = assertEquals("fr", Languages.resolve("fr"))
    @Test fun `espaces autour du tag`() = assertEquals("fr", Languages.resolve("  fr-FR "))
    @Test fun `langue non supportee retombe sur anglais`() = assertEquals("en", Languages.resolve("de-DE"))
    @Test fun `tag vide retombe sur anglais`() = assertEquals("en", Languages.resolve(""))
    @Test fun `tag nul retombe sur anglais`() = assertEquals("en", Languages.resolve(null))
    @Test fun `tag mal forme retombe sur anglais`() = assertEquals("en", Languages.resolve("--"))
}
