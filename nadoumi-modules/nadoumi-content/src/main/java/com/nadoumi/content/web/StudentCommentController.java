package com.nadoumi.content.web;

import com.nadoumi.content.service.ArticleCommentService;
import com.nadoumi.content.web.request.CommentRequest;
import com.nadoumi.content.web.response.CommentNode;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Signed-in users comment on, and reply to comments of, a published article. */
@RestController
@RequestMapping("/api/student/news")
public class StudentCommentController {

    private final ArticleCommentService comments;

    public StudentCommentController(ArticleCommentService comments) {
        this.comments = comments;
    }

    @PostMapping("/{slug}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentNode post(@PathVariable String slug, @Valid @RequestBody CommentRequest req) {
        return comments.post(slug, req);
    }
}
