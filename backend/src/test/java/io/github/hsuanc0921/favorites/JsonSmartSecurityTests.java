package io.github.hsuanc0921.favorites;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minidev.json.JSONValue;
import org.junit.jupiter.api.Test;

class JsonSmartSecurityTests {

	@Test
	void strictValidationRejectsSubstituteCharacterFollowedByTrailingData() {
		String validJson = "{\"a\":1}";
		String invalidJson = validJson + (char) 0x1A + "garbage";

		assertTrue(JSONValue.isValidJsonStrict(validJson));
		assertFalse(JSONValue.isValidJsonStrict(invalidJson));
	}
}
