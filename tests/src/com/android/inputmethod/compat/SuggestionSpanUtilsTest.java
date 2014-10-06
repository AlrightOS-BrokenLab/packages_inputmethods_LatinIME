/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.inputmethod.compat;

import android.annotation.TargetApi;
import android.os.Build;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.SuggestionSpan;

import com.android.inputmethod.latin.SuggestedWords;
import com.android.inputmethod.latin.SuggestedWords.SuggestedWordInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

@SmallTest
public class SuggestionSpanUtilsTest extends AndroidTestCase {

    /**
     * Helper method to create a dummy {@link SuggestedWordInfo}.
     *
     * @param kindAndFlags the kind and flags to be used to create {@link SuggestedWordInfo}.
     * @param word the word to be used to create {@link SuggestedWordInfo}.
     * @return a new instance of {@link SuggestedWordInfo}.
     */
    private static SuggestedWordInfo createWordInfo(final String word, final int kindAndFlags) {
        return new SuggestedWordInfo(word, 1 /* score */, kindAndFlags, null /* sourceDict */,
                SuggestedWordInfo.NOT_AN_INDEX /* indexOfTouchPointOfSecondWord */,
                SuggestedWordInfo.NOT_A_CONFIDENCE /* autoCommitFirstWordConfidence */);
    }

    private static void assertNotSuggestionSpan(final String expectedText,
            final CharSequence actualText) {
        assertTrue(TextUtils.equals(expectedText, actualText));
        if (!(actualText instanceof Spanned)) {
            return;
        }
        final Spanned spanned = (Spanned)actualText;
        final SuggestionSpan[] suggestionSpans = spanned.getSpans(0, spanned.length(),
                SuggestionSpan.class);
        assertEquals(0, suggestionSpans.length);
    }

    private static void assertSuggestionSpan(final String expectedText,
            final int reuiredSuggestionSpanFlags, final int requiredSpanFlags,
            final String[] expectedSuggestions,
            final CharSequence actualText) {
        assertTrue(TextUtils.equals(expectedText, actualText));
        assertTrue(actualText instanceof Spanned);
        final Spanned spanned = (Spanned)actualText;
        final SuggestionSpan[] suggestionSpans = spanned.getSpans(0, spanned.length(),
                SuggestionSpan.class);
        assertEquals(1, suggestionSpans.length);
        final SuggestionSpan suggestionSpan = suggestionSpans[0];
        if (reuiredSuggestionSpanFlags != 0) {
            assertTrue((suggestionSpan.getFlags() & reuiredSuggestionSpanFlags) != 0);
        }
        if (requiredSpanFlags != 0) {
            assertTrue((spanned.getSpanFlags(suggestionSpan) & requiredSpanFlags) != 0);
        }
        if (expectedSuggestions != null) {
            final String[] actualSuggestions = suggestionSpan.getSuggestions();
            assertEquals(expectedSuggestions.length, actualSuggestions.length);
            for (int i = 0; i < expectedSuggestions.length; ++i) {
                assertEquals(expectedSuggestions[i], actualSuggestions[i]);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    public void testGetTextWithAutoCorrectionIndicatorUnderline() {
        final String ORIGINAL_TEXT = "Hey!";
        final CharSequence text = SuggestionSpanUtils.getTextWithAutoCorrectionIndicatorUnderline(
                getContext(), ORIGINAL_TEXT);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            assertNotSuggestionSpan(ORIGINAL_TEXT, text);
            return;
        }

        assertSuggestionSpan(ORIGINAL_TEXT,
                SuggestionSpan.FLAG_AUTO_CORRECTION /* reuiredSuggestionSpanFlags */,
                Spanned.SPAN_COMPOSING | Spanned.SPAN_EXCLUSIVE_EXCLUSIVE /* requiredSpanFlags */,
                new String[]{}, text);
    }

    public void testGetTextWithSuggestionSpan() {
        final SuggestedWordInfo predicition1 =
                createWordInfo("Quality", SuggestedWordInfo.KIND_PREDICTION);
        final SuggestedWordInfo predicition2 =
                createWordInfo("Speed", SuggestedWordInfo.KIND_PREDICTION);
        final SuggestedWordInfo predicition3 =
                createWordInfo("Price", SuggestedWordInfo.KIND_PREDICTION);

        final SuggestedWordInfo typed =
                createWordInfo("Hey", SuggestedWordInfo.KIND_TYPED);

        final SuggestedWordInfo[] corrections =
                new SuggestedWordInfo[SuggestionSpan.SUGGESTIONS_MAX_SIZE * 2];
        for (int i = 0; i < corrections.length; ++i) {
            corrections[i] = createWordInfo("correction" + i, SuggestedWordInfo.KIND_CORRECTION);
        }

        // SuggestionSpan will not be attached when {@link SuggestedWords#INPUT_STYLE_PREDICTION}
        // is specified.
        {
            final SuggestedWords predictedWords = new SuggestedWords(
                    new ArrayList<>(Arrays.asList(predicition1, predicition2, predicition3)),
                    null /* rawSuggestions */,
                    false /* typedWordValid */,
                    false /* willAutoCorrect */,
                    false /* isObsoleteSuggestions */,
                    SuggestedWords.INPUT_STYLE_PREDICTION);
            final String PICKED_WORD = predicition2.mWord;
            assertNotSuggestionSpan(
                    PICKED_WORD,
                    SuggestionSpanUtils.getTextWithSuggestionSpan(getContext(), PICKED_WORD,
                            predictedWords));
        }

        final ArrayList<SuggestedWordInfo> suggestedWordList = new ArrayList<>();
        suggestedWordList.add(typed);
        suggestedWordList.add(predicition1);
        suggestedWordList.add(predicition2);
        suggestedWordList.add(predicition3);
        suggestedWordList.addAll(Arrays.asList(corrections));
        final SuggestedWords typedAndCollectedWords = new SuggestedWords(
                suggestedWordList,
                null /* rawSuggestions */,
                false /* typedWordValid */,
                false /* willAutoCorrect */,
                false /* isObsoleteSuggestions */,
                SuggestedWords.INPUT_STYLE_TYPING);

        for (final SuggestedWordInfo pickedWord : suggestedWordList) {
            final String PICKED_WORD = pickedWord.mWord;

            final ArrayList<String> expectedSuggestions = new ArrayList<>();
            for (SuggestedWordInfo suggestedWordInfo : suggestedWordList) {
                if (expectedSuggestions.size() >= SuggestionSpan.SUGGESTIONS_MAX_SIZE) {
                    break;
                }
                if (suggestedWordInfo.isKindOf(SuggestedWordInfo.KIND_PREDICTION)) {
                    // Currently predictions are not filled into SuggestionSpan.
                    continue;
                }
                final String suggestedWord = suggestedWordInfo.mWord;
                if (TextUtils.equals(PICKED_WORD, suggestedWord)) {
                    // Typed word itself is not added to SuggestionSpan.
                    continue;
                }
                expectedSuggestions.add(suggestedWord);
            }

            assertSuggestionSpan(
                    PICKED_WORD,
                    0 /* reuiredSuggestionSpanFlags */,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE /* requiredSpanFlags */,
                    expectedSuggestions.toArray(new String[expectedSuggestions.size()]),
                    SuggestionSpanUtils.getTextWithSuggestionSpan(getContext(), PICKED_WORD,
                            typedAndCollectedWords));
        }
    }
}
