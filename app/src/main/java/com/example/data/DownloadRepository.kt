package com.example.data

import kotlinx.coroutines.flow.Flow

class DownloadRepository(private val downloadDao: DownloadDao) {
    val allDownloads: Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()

    suspend fun insertDownload(download: DownloadEntity): Long {
        return downloadDao.insertDownload(download)
    }

    suspend fun deleteDownload(download: DownloadEntity) {
        downloadDao.deleteDownload(download)
    }

    suspend fun deleteDownloadById(id: Int) {
        downloadDao.deleteDownloadById(id)
    }

    suspend fun getDownloadById(id: Int): DownloadEntity? {
        return downloadDao.getDownloadById(id)
    }
}
