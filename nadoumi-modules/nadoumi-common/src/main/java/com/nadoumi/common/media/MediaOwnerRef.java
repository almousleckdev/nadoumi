package com.nadoumi.common.media;

/** Reference to the domain entity that owns a media asset. */
public record MediaOwnerRef(MediaOwnerKind kind, long id) {}
