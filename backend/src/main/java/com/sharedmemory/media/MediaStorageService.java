package com.sharedmemory.media;

public interface MediaStorageService {
    StoredFile store(byte[] plaintext);

    byte[] retrieve(String storageKey);
}
