package com.sharedmemory.media;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Photos are encrypted at rest with AES-256-GCM (per the doc's privacy requirements) and stored
 * as opaque blobs on local disk for MVP; storageKey is a random id, never a guessable path.
 * Swappable for an S3 + SSE-KMS implementation later without touching callers (MediaStorageService).
 */
@Service
public class LocalEncryptedFileStorage implements MediaStorageService {

    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_BYTES = 12;

    private final Path storageRoot;
    private final SecretKeySpec key;
    private final SecureRandom secureRandom = new SecureRandom();

    public LocalEncryptedFileStorage(
            @Value("${app.media.storage-root}") String storageRoot,
            @Value("${app.media.master-key-base64}") String masterKeyBase64) {
        this.storageRoot = Path.of(storageRoot);
        try {
            Files.createDirectories(this.storageRoot);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create media storage directory", e);
        }
        this.key = new SecretKeySpec(Base64.getDecoder().decode(masterKeyBase64), "AES");
    }

    @Override
    public StoredFile store(byte[] plaintext) {
        byte[] iv = new byte[GCM_IV_BYTES];
        secureRandom.nextBytes(iv);
        byte[] ciphertext = crypt(Cipher.ENCRYPT_MODE, plaintext, iv);

        String storageKey = UUID.randomUUID().toString();
        try {
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv).put(ciphertext);
            Files.write(storageRoot.resolve(storageKey), buffer.array());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write encrypted media file", e);
        }
        return new StoredFile(storageKey, sha256Hex(plaintext));
    }

    @Override
    public byte[] retrieve(String storageKey) {
        byte[] fileBytes;
        try {
            fileBytes = Files.readAllBytes(storageRoot.resolve(storageKey));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read encrypted media file", e);
        }
        byte[] iv = new byte[GCM_IV_BYTES];
        byte[] ciphertext = new byte[fileBytes.length - GCM_IV_BYTES];
        System.arraycopy(fileBytes, 0, iv, 0, GCM_IV_BYTES);
        System.arraycopy(fileBytes, GCM_IV_BYTES, ciphertext, 0, ciphertext.length);
        return crypt(Cipher.DECRYPT_MODE, ciphertext, iv);
    }

    private byte[] crypt(int mode, byte[] input, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(mode, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return cipher.doFinal(input);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Media encryption/decryption failed", e);
        }
    }

    private String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Checksum computation failed", e);
        }
    }
}
