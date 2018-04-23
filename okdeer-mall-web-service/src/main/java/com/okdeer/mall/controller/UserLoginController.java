package com.okdeer.mall.controller;

import com.okdeer.mall.entity.SysConfig;
import com.okdeer.mall.service.SysConfigService;
import commons.CommonError;
import commons.Constants;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UnknownAccountException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import utils.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Created by Administrator on 2018/4/21 0021.
 */
@RestController
@RequestMapping("/userLogin")
public class UserLoginController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserLoginController.class);
    // 设置用户session主键
    public static final String SESSION_USER = "session_user";

    @Autowired
    private SysConfigService sysConfigService;
    /**
     * 返回16位的code
     * @return
     */
    @RequestMapping(value = "/get/code", method = RequestMethod.POST)
    public JSONResult getCode(){
        try {
            String nowDate = DateUtil.getDateStr(DateUtil.strToDate(DateUtil.NowStr()), "yyyy-MM-dd");
            Md5 md5 = new Md5();
            String code = md5.getMD5ofStr(nowDate);
            if (StringUtils.isEmpty(code)) {
                return CommonError.FAIL_ERROR.toJSONResult("获取CODE");
            } else {
                code = StringUtils.subStringAdd(code, 16, "0");
                return JSONResult.success(code);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return CommonError.FAIL_ERROR.toJSONResult("获取CODE");
        }
    }


    /**
     * 用户注销退出登录
     * @param map
     * @return
     */
    @RequestMapping(value = "/logout", method = RequestMethod.POST)
    public JSONResult logOut(@RequestBody Map<Object, Object> map, HttpServletRequest request) {

        String userName = StringUtils.trim(map.get("author"));//用户的登录UserName

        if (StringUtils.isEmpty(userName)) {
            return JSONResult.fail("1001", "用户名或密码不能为空");
        }
        // 获取数据库存储的私钥信息
        SysConfig privateDTO = sysConfigService.findSysConfigByCode("MES_RSA_PRIVATE_KEY");
        if (privateDTO == null) {
            return CommonError.FAIL_ERROR.toJSONResult("获取系统配置失败");
        }

        try {
            userName = getDecrypt(userName, privateDTO.getValue());
        } catch (Exception ex) {
            return CommonError.FAIL_ERROR.toJSONResult("用户名密码报文解密失败");
        }
        //JSONResult result = purviewManager.logout(userName);
        // 删除session信息
        request.getSession().removeAttribute(SESSION_USER);
        return null;
    }

    /**
     * 根据前端传过来的密文进行解密
     *
     * @param message
     * @return
     */
    public String getDecrypt(String message, String privateCode) throws Exception {
        byte[] en_result = HexToBytes.HexString2Bytes(message);
        byte[] de_result = RSAUtils.decrypt(RSAUtils.getPrivateKey(privateCode), en_result);

        StringBuilder sb = new StringBuilder();
        sb.append(new String(de_result));
        String str = sb.reverse().toString();

        String rsa_result = StringUtils.trim(Escape.unescape(str));

        String nowDate = DateUtil.getDateStr(DateUtil.strToDate(DateUtil.NowStr()), "yyyy-MM-dd");
        Md5 md5 = new Md5();
        String code = md5.getMD5ofStr(nowDate);
        String key = StringUtils.subStringAdd(code, 16, "0");
        String result = StringUtils.trim(AESUtils.decryptData(rsa_result, key, key));
        return result;
    }
}