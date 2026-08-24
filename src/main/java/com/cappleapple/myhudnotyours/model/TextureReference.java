package com.cappleapple.myhudnotyours.model;

import java.util.Objects;

public final class TextureReference {
    public enum Kind { RESOURCE, IMPORTED }

    public Kind kind = Kind.RESOURCE;
    public String id = "";

    public TextureReference() {
    }

    public TextureReference(Kind kind, String id) {
        this.kind = Objects.requireNonNull(kind);
        this.id = Objects.requireNonNull(id);
    }

    public boolean assigned() {
        return id != null && !id.isBlank();
    }

    public String key() {
        return kind.name().toLowerCase() + ":" + id;
    }

    public TextureReference copy() {
        return new TextureReference(kind, id);
    }
}
