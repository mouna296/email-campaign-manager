package com.untitled.ecm.core;

import com.untitled.ecm.constants.DakiyaStrings;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class DakiyaUtilsTest {
    @Test
    public void obfuscatedEmailShouldHandleNullOrEmpty() {
        assertTrue(DakiyaUtils.getObfuscatedEmail(null).length() == 0);
        assertTrue(DakiyaUtils.getObfuscatedEmail("").length() == 0);
    }

    @Test
    public void obfuscatedEmailShouldHandleInvalidEmail() {
        String invalidEmail = RandomStringUtils.randomAlphabetic(0, 50);
        assertTrue(invalidEmail.length() == DakiyaUtils.getObfuscatedEmail(invalidEmail).length());
        assertTrue(DakiyaUtils.getObfuscatedEmail("").length() == 0);
    }

    @Test
    public void obfuscatedEmailShouldReturnValidEmailIfValidEmailProvided() {
        String domain = "gmail.com";
        String validEmail = RandomStringUtils.randomAlphabetic(0, 20) + "@" + domain;
        String obfuscatedEmail = DakiyaUtils.getObfuscatedEmail(validEmail);
        assertTrue(obfuscatedEmail.endsWith(domain));
        assertTrue(obfuscatedEmail.contains("@"));
        assertTrue(obfuscatedEmail.length() == validEmail.length());
    }

    @Test
    public void obfuscatedEmailShouldHandleMultiple_at_() {
        String domain = "gmail.com";
        String validEmail = RandomStringUtils.randomAlphabetic(0, 10) + "@" + RandomStringUtils.randomAlphabetic(0, 10) + "@" + domain;
        String obfuscatedEmail = DakiyaUtils.getObfuscatedEmail(validEmail);
        assertTrue(obfuscatedEmail.contains("@"));
        assertTrue(obfuscatedEmail.length() == validEmail.length());
    }


    private String generateValidEmailStr() {
        final String emailUserString = RandomStringUtils.randomAlphanumeric(6, 20);
        final String domain = RandomStringUtils.randomAlphabetic(6, 20);
        return emailUserString + "@" + domain + ".com";

    }

    @Test
    public void onlyBlockedEmailsShouldBeDetected() {
        int count = 100;
        while (count > 0) {
            assertTrue(!DakiyaUtils.isBadEmail(generateValidEmailStr()));
            count--;
        }
    }

    @Test
    public void blockedEmailsShouldBeDetected() {
        assertTrue(DakiyaUtils.isBadEmail("xyz@gmail.com"));
        assertTrue(DakiyaUtils.isBadEmail("xyz@dh.com"));
        assertTrue(DakiyaUtils.isBadEmail("test@dh.com"));
        assertTrue(DakiyaUtils.isBadEmail("test@@yum.com"));
        assertTrue(DakiyaUtils.isBadEmail("lol@gmail.com"));
        assertTrue(DakiyaUtils.isBadEmail("correct@gamil.com"));

    }

    @Test
    public void nullAndEmptyShouldBeFiltered() {
        assertTrue(DakiyaUtils.isBadEmail(null));
        assertTrue(DakiyaUtils.isBadEmail("  "));
        assertTrue(DakiyaUtils.isBadEmail("test@@yum.com"));
    }

    @Test
    public void ensureGetRandomAlphaNumericStringWorks() {
        final Set<String> randomStrings = new HashSet<>();
        final int count = 10;
        for (int i = 0; i < count; i++) {
            randomStrings.add(DakiyaUtils.getRandomAlphaNumericString());
        }
        assertEquals(count, randomStrings.size());

        for (String str : randomStrings) {
            assertNotNull(str);
            assertTrue(!StringUtils.isWhitespace(str));
            assertTrue(!StringUtils.isEmpty(str));
            assertTrue(StringUtils.isAlphanumeric(str));
            assertEquals(DakiyaStrings.MIN_PASS_LENGHT, str.length());
        }

        final String str = DakiyaUtils.getRandomAlphaNumericString(100);
        assertEquals(100, str.length());
        assertTrue(StringUtils.isAlphanumeric(str));

    }

}
