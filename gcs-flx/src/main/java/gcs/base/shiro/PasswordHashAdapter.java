package gcs.base.shiro;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Data
@Component
public class PasswordHashAdapter {

    @Autowired
    private HashedCredentialsMatcher credentialMatcher;

    /**
     * 计算 password 的 hash 值
     * @param password  密码字符串
     * @param salt      Salt值(未启用)
     * @return
     */
    public String hash(CharSequence password, CharSequence salt) {
        return new SimpleHash(
            credentialMatcher.getHashAlgorithmName(), 
            password.toString(), 
            null, 
            credentialMatcher.getHashIterations()
        ).toHex();
    }
}
