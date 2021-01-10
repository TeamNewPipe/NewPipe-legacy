package org.schabi.newpipelegacy.settings

import android.content.SharedPreferences
import org.junit.Assert
import org.junit.Assume
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import org.schabi.newpipelegacy.settings.ContentSettingsManagerTest.ExportTest
import java.io.File
import java.io.ObjectInputStream
import java.util.zip.ZipFile

@RunWith(Suite::class)
@Suite.SuiteClasses(ExportTest::class)
class ContentSettingsManagerTest {

    @RunWith(MockitoJUnitRunner::class)
    class ExportTest {

        companion object {
            private lateinit var newpipeDb: File
            private lateinit var newpipeSettings: File

            @JvmStatic
            @BeforeClass
            fun setupFiles() {
                val dbPath = ExportTest::class.java.classLoader?.getResource("settings/newpipe.db")?.file
                val settingsPath = ExportTest::class.java.classLoader?.getResource("settings/newpipe.settings")?.path
                Assume.assumeNotNull(dbPath)
                Assume.assumeNotNull(settingsPath)

                newpipeDb = File(dbPath!!)
                newpipeSettings = File(settingsPath!!)
            }
        }

        private lateinit var preferences: SharedPreferences

        @Before
        fun setupMocks() {
            preferences = Mockito.mock(SharedPreferences::class.java, Mockito.withSettings().stubOnly())
        }

        @Test
        fun `The settings must be exported successfully in the correct format`() {
            val expectedPreferences = mapOf("such pref" to "much wow")
            `when`(preferences.all).thenReturn(expectedPreferences)

            val manager = ContentSettingsManager(newpipeDb, newpipeSettings)

            val output = File.createTempFile("newpipe_", "")
            manager.exportDatabase(preferences, output.absolutePath)

            val zipFile = ZipFile(output.absoluteFile)
            val entries = zipFile.entries().toList()
            Assert.assertEquals(2, entries.size)

            zipFile.getInputStream(entries.first { it.name == "newpipe.db" }).use { actual ->
                newpipeDb.inputStream().use { expected ->
                    Assert.assertEquals(expected.reader().readText(), actual.reader().readText())
                }
            }

            zipFile.getInputStream(entries.first { it.name == "newpipe.settings" }).use { actual ->
                val actualPreferences = ObjectInputStream(actual).readObject()
                Assert.assertEquals(expectedPreferences, actualPreferences)
            }
        }
    }
}
