package com.bugtrack.ui.model;

import java.time.LocalDateTime;

/**
 * A comment on a bug. Replies point to their parent comment.
 *
 * @param id         comment id
 * @param bugId      the bug this comment belongs to
 * @param parentId   id of the comment being replied to, or {@code null} for a top-level comment
 * @param authorName who wrote it
 * @param body       the text; words starting with {@code @} are mentions
 * @param createdAt  when it was posted
 */
public record CommentView(long id, long bugId, Long parentId, String authorName, String body,
                          LocalDateTime createdAt) {

    /**
     * @return {@code true} if this comment is a reply to another comment
     */
    public boolean isReply() {
        return parentId != null;
    }
}
