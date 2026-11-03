package com.tyron.common.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringSearch {

    // pattern is the string that we are searching for in the text.
    private final byte[] pattern;

    // badCharSkip[b] contains the distance between the last byte of pattern
    // and the rightmost occurrence of b in pattern. If b is not in pattern,
    // badCharSkip[b] is len(pattern).
    private final int[] badCharSkip = new int[256];

    private final int[] goodSuffixSkip;

    public StringSearch(String patternSting) {
        this.pattern = patternSting.getBytes();
        this.goodSuffixSkip = new int[pattern.length];

        // last is the index of the last character in the pattern.
        int last = pattern.length - 1;

        // Build bad character table.
        Arrays.fill(badCharSkip, pattern.length);
        for (int i = 0; i < last; i++) {
            badCharSkip[pattern[i] + 128] = last - i;
        }

        // Build good suffix table.
        int lastPrefix = last;
        for (int i = last; i >= 0; i--) {
            if (hasPrefix(pattern, new Slice(pattern, i + 1))) {
                lastPrefix = i + 1;
            }
            goodSuffixSkip[i] = lastPrefix + last - i;
        }
        for (int i = 0; i < last; i++) {
            int lenSuffix = longestCommonSuffix(pattern, new Slice(pattern, 1, i + 1));
            if (pattern[i - lenSuffix] != pattern[last - lenSuffix]) {
                goodSuffixSkip[last - lenSuffix] = lenSuffix + last - i;
            }
        }
    }

    public int next(String text) {
        return next(text.getBytes());
    }

    private int next(byte[] text) {
        return next(ByteBuffer.wrap(text));
    }

    private int next(ByteBuffer text) {
        return next(text, 0);
    }

    private int next(ByteBuffer text, int startingAfter) {
        int i = startingAfter + pattern.length - 1;
        while (i < text.limit()) {
            int j = pattern.length - 1;
            while (j >= 0 && text.get(i) == pattern[j]) {
                i--;
                j--;
            }
            if (j < 0) {
                return i + 1; // match
            }
            i += Math.max(badCharSkip[text.get(i) + 128], goodSuffixSkip[j]);
        }
        return -1;
    }

    private boolean hasPrefix(byte[] s, Slice prefix) {
        for (int i = 0; i < prefix.length(); i++) {
            if (s[i] != prefix.get(i)) {
                return false;
            }
        }
        return true;
    }


    private int longestCommonSuffix(byte[] a, Slice b) {
        int i = 0;
        for (; i < a.length && i < b.length(); i++) {
            if (a[a.length - 1 - i] != b.get(b.length() - 1 - i)) {
                break;
            }
        }
        return i;
    }

    public int nextWord(String text) {
        return nextWord(text.getBytes());
    }

    private int nextWord(byte[] text) {
        return nextWord(ByteBuffer.wrap(text));
    }

    private int nextWord(ByteBuffer text) {
        int i = 0;
        while (true) {
            i = next(text, i);
            if (i == -1) {
                return -1;
            }
            if (isWord(text, i)) {
                return i;
            }
            i++;
        }
    }

    public static int endOfLine(CharSequence contents, int cursor) {
        while (cursor < contents.length()) {
            char c = contents.charAt(cursor);
            if (c == '\r' || c == '\n') {
                break;
            }
            cursor++;
        }
        return cursor;
    }

    private boolean isWordChar(byte b) {
        char c = (char) (b + 128);
        return Character.isAlphabetic(c) || Character.isDigit(c) || c == '$' || c == '_';
    }

    private boolean startsWord(ByteBuffer text, int offset) {
        if (offset == 0) {
            return true;
        }
        return !isWordChar(text.get(offset - 1));
    }

    private boolean endsWord(ByteBuffer text, int offset) {
        if (offset + 1 >= text.limit()) {
            return true;
        }
        return !isWordChar(text.get(offset + 1));
    }

    private boolean isWord(ByteBuffer text, int offset) {
        return startsWord(text, offset) && endsWord(text, offset + pattern.length - 1);
    }


    public static boolean containsWord(Path java, String query) {
        StringSearch search = new StringSearch(query);
        try (FileChannel channel = FileChannel.open(java)) {
            int limit = Math.min((int) channel.size(), SEARCH_BUFFER.capacity());
            SEARCH_BUFFER.position(0);
            SEARCH_BUFFER.limit(limit);
            channel.read(SEARCH_BUFFER);
            SEARCH_BUFFER.position(0);
            return search.nextWord(SEARCH_BUFFER) != -1;
        } catch (NoSuchFileException e) {
            return false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean matchesPartialName(CharSequence candidate, CharSequence partialName) {
        if (partialName.length() == 1 && partialName.equals(".")) {
            return true;
        }
        if (candidate.length() < partialName.length()) {
            return false;
        }
        for (int i = 0; i < partialName.length(); i++) {
            if (candidate.charAt(i) != partialName.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public static boolean matchesPartialNameLowercase(CharSequence candidate,
                                                      CharSequence partialName) {
        if (partialName.length() == 1 && partialName.equals(".")) {
            return true;
        }
        if (candidate.length() < partialName.length()) {
            return false;
        }
        for (int i = 0; i < partialName.length(); i++) {
            if (Character.toLowerCase(candidate.charAt(i)) !=
                Character.toLowerCase(partialName.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static String packageName(File file) {
        Pattern packagePattern = Pattern.compile("package\\s+([a-zA_Z][.\\w]*+)(;)?");
        Pattern startOfClass = Pattern.compile("^[\\w ]*class +\\w+");
        try (BufferedReader lines = bufferedReader(file)) {
            for (String line = lines.readLine(); line != null; line = lines.readLine()) {
                if (startOfClass.matcher(line)
                        .find()) {
                    return "";
                }
                Matcher matchPackage = packagePattern.matcher(line);
                if (matchPackage.find()) {
                    String id = matchPackage.group(1);
                    return id;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return "";
    }

    public static String mostName(String name) {
        int lastDot = name.lastIndexOf('.');
        return lastDot == -1 ? "" : name.substring(0, lastDot);
    }

    public static String lastName(String name) {
        int i = name.lastIndexOf('.');
        if (i == -1) {
            return name;
        } else {
            return name.substring(i + 1);
        }
    }

    public static BufferedReader bufferedReader(File file) {
        try {
            return new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        } catch (FileNotFoundException e) {
            return new BufferedReader(new StringReader(""));
        }
    }

    public static double similarity(String s1, String s2) {
        String longer = s1, shorter = s2;
        if (s1.length() < s2.length()) {
            longer = s2;
            shorter = s1;
        }

        int longerLength = longer.length();
        if (longerLength == 0) {
            return 1.0;
        }
        return (longerLength - editDistance(longer, shorter)) / (double) longerLength;
    }

    public static int editDistance(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();

        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    costs[j] = j;
                } else {
                    if (j > 0) {
                        int newValue = costs[j - 1];
                        if (s1.charAt(i - 1) != s2.charAt(j - 1)) {
                            newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
                        }
                        costs[j - 1] = lastValue;
                        lastValue = newValue;
                    }
                }
            }
            if (i > 0) {
                costs[s2.length()] = lastValue;
            }
        }
        return costs[s2.length()];
    }

    public static boolean endsWithParen(CharSequence contents, int cursor) {
        while (cursor < contents.length()) {
            char c = contents.charAt(cursor);
            if (Character.isWhitespace(c)) {
                cursor++;
                continue;
            }
            return c == '(';
        }
        return false;
    }

    public static String partialIdentifier(String contents, int cursor) {
        int start = cursor;
        while (start > 0 && Character.isJavaIdentifierPart(contents.charAt(start - 1))) {
            start--;
        }
        return contents.substring(start, cursor);
    }

    public static String qualifiedPartialIdentifier(String contents, int cursor) {
        int start = cursor;
        while (start > 0) {
            char c = contents.charAt(start - 1);
            if (Character.isJavaIdentifierPart(c) || c == '.') {
                start--;
            } else {
                break;
            }
        }
        return contents.substring(start, cursor);
    }

    private static class Slice {
        private final byte[] target;
        private int from, until;

        int length() {
            return until - from;
        }

        byte get(int i) {
            return target[from + i];
        }

        Slice(byte[] target, int from) {
            this(target, from, target.length);
        }

        Slice(byte[] target, int from, int until) {
            this.target = target;
            this.from = from;
            this.until = until;
        }
    }

    private static final ByteBuffer SEARCH_BUFFER = ByteBuffer.allocateDirect(1024 * 1024);
}
