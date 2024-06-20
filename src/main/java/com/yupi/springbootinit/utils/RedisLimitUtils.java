package com.yupi.springbootinit.utils;


import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.exception.BusinessException;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class RedisLimitUtils {

    @Resource
    private  RedissonClient redissonClient;

    public  void doRateLimit(String key){
        //创建一个名称为user_limiter的限流器，每秒最多访问5次
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        rateLimiter.trySetRate(RateType.OVERALL,2,1, RateIntervalUnit.SECONDS);
        //每当来了一个才操作来了以后，请求一个令牌
        boolean canOp = rateLimiter.tryAcquire(1);
        if (!canOp){
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
        }

    }

}
