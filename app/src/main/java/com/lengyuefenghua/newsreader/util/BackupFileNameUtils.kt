package com.lengyuefenghua.newsreader.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val backupDateFormatter: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE

internal fun buildFeedsBackupFileName(date: LocalDate = LocalDate.now()): String {
    val dateText = date.format(backupDateFormatter)
    return "NewsReader_Feeds_Backup_${dateText}.json"
}

internal fun buildDataBackupFileName(date: LocalDate = LocalDate.now()): String {
    val dateText = date.format(backupDateFormatter)
    return "NewsReader_Data_Backup_${dateText}.json"
}
