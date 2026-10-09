package com.nadoumi.content.web;

import com.nadoumi.content.service.ArticleEngagementService;
import com.nadoumi.content.web.response.LikeState;
import com.nadoumi.content.web.response.LikersPage;
import com.nadoumi.content.web.response.MyReactions;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Signed-in readers like articles and comments. PUT and DELETE are idempotent, so the client can
 * simply state the like it wants and retry safely.
 */
@RestController
@RequestMapping("/api/student/news/{slug}")
public class StudentReactionController {

    private final ArticleEngagementService engagement;

    public StudentReactionController(ArticleEngagementService engagement) {
        this.engagement = engagement;
    }

    @GetMapping("/reactions")
    public MyReactions mine(@PathVariable String slug) {
        return engagement.mine(slug);
    }

    @GetMapping("/likes")
    public LikersPage likers(@PathVariable String slug, @RequestParam(defaultValue = "20") int limit) {
        return engagement.likers(slug, limit);
    }

    @PutMapping("/like")
    public LikeState like(@PathVariable String slug) {
        return engagement.likeArticle(slug);
    }

    @DeleteMapping("/like")
    public LikeState unlike(@PathVariable String slug) {
        return engagement.unlikeArticle(slug);
    }

    @PutMapping("/comments/{commentId}/like")
    public LikeState likeComment(@PathVariable String slug, @PathVariable long commentId) {
        return engagement.likeComment(slug, commentId);
    }

    @DeleteMapping("/comments/{commentId}/like")
    public LikeState unlikeComment(@PathVariable String slug, @PathVariable long commentId) {
        return engagement.unlikeComment(slug, commentId);
    }
}
