package com.ruoyi.common.utils.ip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.http.HttpUtils;

/**
 * 获取地址类
 * 
 * @author ruoyi
 */
public class AddressUtils
{
    private static final Logger log = LoggerFactory.getLogger(AddressUtils.class);

    // 未知地址
    public static final String UNKNOWN = "Unknown Location";

    public static String getRealAddressByIP(String ip)
    {
        // 内网不查询
        if (IpUtils.internalIp(ip))
        {
            return "Local Network";
        }
        if (RuoYiConfig.isAddressEnabled())
        {
            try
            {
                String rspStr = HttpUtils.sendGet("http://ip-api.com/json/" + ip, "", com.ruoyi.common.constant.Constants.UTF8);
                if (StringUtils.isEmpty(rspStr))
                {
                    log.error("failed to resolve geolocation {}", ip);
                    return UNKNOWN;
                }
                JSONObject obj = JSON.parseObject(rspStr);
                String country = obj.getString("country");
                String city = obj.getString("city");
                if (country != null && city != null) {
                    return String.format("%s, %s", city, country);
                } else {
                    return UNKNOWN;
                }
            }
            catch (Exception e)
            {
                log.error("failed to resolve geolocation {}", ip);
            }
        }
        return UNKNOWN;
    }
}
