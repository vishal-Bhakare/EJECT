package com.edtech.coe.batch.service;

import java.util.Collection;
import java.util.Map;

public interface BatchLookupService {

    // Looks up batch names in one query. An id that does not exist is missing from the result.
    Map<Long, String> getBatchNames(Collection<Long> ids);
}