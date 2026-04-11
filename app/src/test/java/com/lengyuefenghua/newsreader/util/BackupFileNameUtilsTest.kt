package com.lengyuefenghua.newsreader.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BackupFileNameUtilsTest {

    @Test
    fun `feeds backup file name should use yyyyMMdd format`() {
        assertEquals(
            "NewsReader_Feeds_Backup_20260411.json",
            buildFeedsBackupFileName(LocalDate.of(2026, 4, 11))
        )
    }

    @Test
    fun `data backup file name should use yyyyMMdd format`() {
        assertEquals(
            "NewsReader_Data_Backup_20260411.json",
            buildDataBackupFileName(LocalDate.of(2026, 4, 11))
        )
    }
}
