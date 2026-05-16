package com.deepnews.app.util;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Result 包装类单元测试。
 * 测试成功/失败路径、数据正确性、空值处理。
 */
public class ResultTest {

    @Test
    public void success_containsData() {
        Result<String> r = Result.success("hello");
        assertTrue(r.isSuccess());
        assertEquals("hello", r.getData());
        assertNull(r.getError());
    }

    @Test
    public void success_withNullData() {
        Result<String> r = Result.success(null);
        assertTrue(r.isSuccess());
        assertNull(r.getData());
    }

    @Test
    public void error_containsMessage() {
        Result<Integer> r = Result.error("something went wrong");
        assertFalse(r.isSuccess());
        assertEquals("something went wrong", r.getError());
        assertNull(r.getData());
    }

    @Test
    public void error_withEmptyMessage() {
        Result<Object> r = Result.error("");
        assertFalse(r.isSuccess());
        assertEquals("", r.getError());
    }

    @Test
    public void genericListType() {
        List<String> items = List.of("a", "b", "c");
        Result<List<String>> r = Result.success(items);
        assertTrue(r.isSuccess());
        assertEquals(3, r.getData().size());
        assertEquals("a", r.getData().get(0));
    }

    @Test
    public void multipleResultsIndependent() {
        Result<String> r1 = Result.success("ok");
        Result<String> r2 = Result.error("fail");
        assertTrue(r1.isSuccess());
        assertFalse(r2.isSuccess());
    }
}
