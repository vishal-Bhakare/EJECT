package com.edtech.coe.coe.service;

public interface CoeLockService {

    // Locks the COE's row until the surrounding transaction ends. Anyone else who asks for
    // the same lock waits. Fails with a 404 if the COE does not exist.
    void lock(Long coeId);
}