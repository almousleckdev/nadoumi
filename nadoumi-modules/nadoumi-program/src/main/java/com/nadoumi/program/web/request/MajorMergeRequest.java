package com.nadoumi.program.web.request;

import jakarta.validation.constraints.NotNull;

/** Merge the path major into {@code intoId}: its programme links move there and it is deleted. */
public record MajorMergeRequest(@NotNull Long intoId) {
}
