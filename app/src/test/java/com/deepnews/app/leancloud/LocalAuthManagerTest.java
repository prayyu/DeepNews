package com.deepnews.app.leancloud;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.Assert.*;

import android.content.SharedPreferences;

import org.json.JSONObject;

import java.lang.reflect.Method;

public class LocalAuthManagerTest {

    private LocalAuthManager manager;

    @Before
    public void setUp() throws Exception {
        manager = LocalAuthManager.getInstance();
    }

    @Test
    public void hashPassword_returnsConsistentHash() throws Exception {
        String hash1 = manager.hashPassword("hello123");
        String hash2 = manager.hashPassword("hello123");
        assertEquals("相同密码应生成相同哈希", hash1, hash2);
    }

    @Test
    public void hashPassword_differentPasswords_differentHashes() throws Exception {
        String hash1 = manager.hashPassword("password1");
        String hash2 = manager.hashPassword("password2");
        assertNotEquals("不同密码应生成不同哈希", hash1, hash2);
    }

    @Test
    public void hashPassword_returns64CharHexString() throws Exception {
        String hash = manager.hashPassword("test");
        assertEquals("SHA-256 应生成 64 字符十六进制串", 64, hash.length());
    }

    @Test
    public void hashPassword_emptyString() throws Exception {
        String hash = manager.hashPassword("");
        assertNotNull("空密码也应生成哈希", hash);
        assertEquals(64, hash.length());
    }

    @Test
    public void hashPassword_longPassword() throws Exception {
        String longPwd = "a" + "b".repeat(999) + "c";
        String hash = manager.hashPassword(longPwd);
        assertNotNull("长密码应正常生成哈希", hash);
        assertEquals(64, hash.length());
    }

    @Test
    public void hashPassword_unicodeCharacters() throws Exception {
        String hash = manager.hashPassword("密码123!@#中文");
        assertNotNull("含 Unicode 的密码应正常生成哈希", hash);
        assertEquals(64, hash.length());
    }
}
