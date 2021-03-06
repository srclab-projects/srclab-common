package test.java.xyz.srclab.common.lang;

import org.testng.Assert;
import org.testng.annotations.Test;
import xyz.srclab.common.lang.Ref;

public class RefTest {

    @Test
    public void testRef() {
        Ref<String> ref = Ref.with(null);
        Assert.assertEquals(ref.getOrElse("null"), "null");
        Assert.assertFalse(ref.isPresent());
        ref.set("123");
        Assert.assertEquals(ref.getOrElse("null"), "123");
        Assert.assertTrue(ref.isPresent());
    }
}
