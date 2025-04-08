package com.example.service;

import java.util.List;

public interface SymbolService {
    /**
     * Gets a list of available trading symbols.
     * Implementations might cache this list.
     *
     * @return A sorted list of symbol names.
     */
    List<String> getAvailableSymbols();
}
