package com.snapshop.dto.response;

import java.time.Instant;

/** A CSV file waiting in the inbox. */
public record InboxFileResponse(String name, long size, Instant modifiedAt) {
}
