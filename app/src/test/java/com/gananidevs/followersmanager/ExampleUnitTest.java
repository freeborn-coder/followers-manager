package com.gananidevs.followersmanager;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void isCommaInsertionCorrect(){
        // Test if the insert comma function works properly

        int number = 132;
        String myNumberWithComma = "132";
        assertEquals(myNumberWithComma,Helper.insertCommas(number));

    }
}