package org.coffer.coffer2.application

import java.io.InputStream
import java.nio.file.Path

interface ImageStorageService {
    /**
     * Store an image and return the storage key
     */
    fun store(inputStream: InputStream, fileName: String, contentType: String): String

    /**
     * Retrieve an image by its storage key
     */
    fun retrieve(storageKey: String): Path

    /**
     * Delete an image by its storage key
     */
    fun delete(storageKey: String)

    /**
     * Check if an image exists
     */
    fun exists(storageKey: String): Boolean
}
