package org.ethelred.minecraft;

import java.util.function.Supplier;

public class Lazy {
    private final Supplier<String> supplier;

    public Lazy(Supplier<String> supplier) {
        this.supplier = supplier;
    }

    @Override
    public String toString() {
        return supplier.get();
    }
}
