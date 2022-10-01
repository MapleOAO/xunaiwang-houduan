package com.yupi.lovefinder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.lovefinder.common.ErrorCode;
import com.yupi.lovefinder.exception.BusinessException;
import com.yupi.lovefinder.model.entity.Post;
import com.yupi.lovefinder.model.entity.PostThumb;
import com.yupi.lovefinder.model.entity.User;
import com.yupi.lovefinder.service.PostService;
import com.yupi.lovefinder.service.PostThumbService;
import com.yupi.lovefinder.mapper.PostThumbMapper;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author yupili
 * @description 针对表【post_thumb(帖子点赞记录)】的数据库操作Service实现
 * @createDate 2022-09-13 16:04:30
 */
@Service
public class PostThumbServiceImpl extends ServiceImpl<PostThumbMapper, PostThumb>
        implements PostThumbService {

    @Resource
    private PostService postService;

    /**
     * 点赞
     *
     * @param postId
     * @param loginUser
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int doThumb(long postId, User loginUser) {
        // 帖子是否存在
        Post post = postService.getById(postId);
        if (post == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 是否已点赞
        long userId = loginUser.getId();
        PostThumb postThumb = new PostThumb();
        postThumb.setUserId(userId);
        postThumb.setPostId(postId);
        QueryWrapper<PostThumb> postThumbQueryWrapper = new QueryWrapper<>(postThumb);
        boolean result;
        // 每个用户串行点赞
        synchronized (String.valueOf(userId).intern()) {
            long count = this.count(postThumbQueryWrapper);
            // 已点赞
            if (count > 0) {
                result = this.remove(postThumbQueryWrapper);
                if (result) {
                    // 帖子点赞数 - 1
                    result = postService.update()
                            .eq("id", postId)
                            .gt("thumbNum", 0)
                            .setSql("thumbNum = thumbNum - 1")
                            .update();
                    return result ? -1 : 0;
                } else {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR);
                }
            } else {
                // 未点赞
                result = this.save(postThumb);
                if (result) {
                    // 帖子点赞数 + 1
                    result = postService.update()
                            .eq("id", postId)
                            .setSql("thumbNum = thumbNum + 1")
                            .update();
                    return result ? 1 : 0;
                } else {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR);
                }
            }
        }
    }
}



