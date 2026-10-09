package com.nadoumi.communication.stream;

/**
 * The pub/sub wire payload: which user the event is for, the SSE event name ({@code message}, {@code delivered},
 * {@code read}, {@code presence}) and its JSON body. Bodies carry the data itself, so a client updates its state
 * from the event and does not have to re-fetch anything.
 */
public record StreamEvent(long userId, String type, String data) {
}
