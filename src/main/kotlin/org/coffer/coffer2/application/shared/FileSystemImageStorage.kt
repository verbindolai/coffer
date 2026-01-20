package org.coffer.coffer2.application.shared

import org.coffer.coffer2.application.shared.ImageStorageService
import org.coffer.coffer2.application.shared.StorageProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import java.util.UUID

@Service
class FileSystemImageStorage(
    private val storageProperties: StorageProperties
) : ImageStorageService {

    private val logger = LoggerFactory.getLogger(FileSystemImageStorage::class.java)
    private val rootLocation: Path

    init {
        try {
            // Convert to absolute path to ensure consistent path comparisons
            rootLocation = Paths.get(storageProperties.basePath).toAbsolutePath().normalize()
            Files.createDirectories(rootLocation)
            logger.info("Initialized file storage at: ${rootLocation}")
        } catch (e: Exception) {
            throw kotlin.IllegalStateException("Could not initialize storage location", e)
        }
    }

    override fun store(inputStream: InputStream, fileName: String, contentType: String): String {
        if (!storageProperties.allowedContentTypes.contains(contentType)) {
            throw IllegalArgumentException("Content type $contentType is not allowed")
        }

        val extension = getFileExtension(fileName)
        val storageKey = generateStorageKey(extension)
        val destinationPath = resolveStoragePath(storageKey)

        // Create parent directories if they don't exist
        Files.createDirectories(destinationPath.parent)

        // Copy the file
        Files.copy(inputStream, destinationPath, StandardCopyOption.REPLACE_EXISTING)

        logger.debug("Stored file at: ${destinationPath.toAbsolutePath()}")
        return storageKey
    }

    override fun retrieve(storageKey: String): Path {
        val filePath = resolveStoragePath(storageKey)

        if (!Files.exists(filePath)) {
            throw NoSuchFileException(filePath.toFile(), reason = "File not found: $storageKey")
        }

        if (!Files.isReadable(filePath)) {
            throw AccessDeniedException(filePath.toFile(), reason = "Cannot read file: $storageKey")
        }

        return filePath
    }

    override fun delete(storageKey: String) {
        val filePath = resolveStoragePath(storageKey)

        if (Files.exists(filePath)) {
            Files.delete(filePath)
            logger.debug("Deleted file: ${filePath.toAbsolutePath()}")

            // Try to delete empty parent directories
            cleanupEmptyDirectories(filePath.parent)
        }
    }

    override fun exists(storageKey: String): Boolean {
        val filePath = resolveStoragePath(storageKey)
        return Files.exists(filePath)
    }

    private fun generateStorageKey(extension: String): String {
        val now = LocalDate.now()
        val year = now.year
        val month = now.monthValue.toString().padStart(2, '0')
        val uniqueId = UUID.randomUUID().toString()

        return "$year/$month/$uniqueId.$extension"
    }

    private fun resolveStoragePath(storageKey: String): Path {
        val path = rootLocation.resolve(storageKey).normalize()
        // Security check: ensure the path is still under the root location
        if (!path.startsWith(rootLocation)) {
            logger.error("Storage path $path is outside of the configured base path $rootLocation")
            throw SecurityException("Cannot store file outside designated directory")
        }

        return path
    }

    private fun getFileExtension(fileName: String): String {
        val lastDotIndex = fileName.lastIndexOf('.')
        return if (lastDotIndex > 0 && lastDotIndex < fileName.length - 1) {
            fileName.substring(lastDotIndex + 1).lowercase()
        } else {
            "bin"
        }
    }

    private fun cleanupEmptyDirectories(directory: Path) {
        try {
            // Only delete if directory is empty and is a subdirectory of root
            if (directory.startsWith(rootLocation) &&
                directory != rootLocation &&
                Files.isDirectory(directory) &&
                Files.list(directory).use { it.count() == 0L }
            ) {
                Files.delete(directory)
                logger.debug("Cleaned up empty directory: ${directory.toAbsolutePath()}")

                // Recursively clean up parent directories
                cleanupEmptyDirectories(directory.parent)
            }
        } catch (e: Exception) {
            // Ignore errors during cleanup
            logger.trace("Could not clean up directory: ${directory.toAbsolutePath()}", e)
        }
    }
}