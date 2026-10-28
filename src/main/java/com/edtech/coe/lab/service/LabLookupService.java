package com.edtech.coe.lab.service;

import java.util.Collection;
import java.util.Map;

public interface LabLookupService {

    // Looks up lab names in one query. An id that does not exist is missing from the result.
    Map<Long, String> getLabNames(Collection<Long> ids);
}