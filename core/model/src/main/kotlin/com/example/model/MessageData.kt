package com.example.model

import java.time.Instant

class MessageData(
    var author: String,
    var receiver: String,
    var content: String = "",
    var timestamp: String = Instant.now().toEpochMilli().toString(),
    var image: String? = null,
    var file: String? = null,
    var nsfw: Boolean = false,
    var id: String = generateId((author + receiver + content + timestamp)),
) {
    override fun toString(): String {
        return "MessageData(id='$id', author='$author', receiver='$receiver', content='$content', timestamp='$timestamp', image='$image', file='$file', nsfw='$nsfw')"
    }
}