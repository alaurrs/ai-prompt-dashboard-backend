package com.sallyvnge.aipromptbackend.api.dto.thread;

import java.util.List;

public record PageDto<T>(List<T> items, String nextCursor) {
}
