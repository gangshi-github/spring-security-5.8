// Copyright (c) 2006 Damien Miller <djm@mindrot.org>
//
// Permission to use, copy, modify, and distribute this software for any
// purpose with or without fee is hereby granted, provided that the above
// copyright notice and this permission notice appear in all copies.
//
// THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
// WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
// MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
// ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
// WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
// ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
// OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

package org.springframework.security.crypto.bcrypt;

import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JUnit unit tests for BCrypt routines
 * @author Damien Miller
 */
public class BCryptTests {

	String test_vectors[][] = {
			{ "", "$2a$06$DCq7YPn5Rq63x1Lad4cll.",
					"$2a$06$DCq7YPn5Rq63x1Lad4cll.TV4S6ytwfsfvkgY8jIucDrjc8deX1s." },
			{ "", "$2a$08$HqWuK6/Ng6sg9gQzbLrgb.",
					"$2a$08$HqWuK6/Ng6sg9gQzbLrgb.Tl.ZHfXLhvt/SgVyWhQqgqcZ7ZuUtye" },
			{ "", "$2a$10$k1wbIrmNyFAPwPVPSVa/ze",
					"$2a$10$k1wbIrmNyFAPwPVPSVa/zecw2BCEnBwVS2GbrmgzxFUOqW9dk4TCW" },
			{ "", "$2a$12$k42ZFHFWqBp3vWli.nIn8u",
					"$2a$12$k42ZFHFWqBp3vWli.nIn8uYyIkbvYRvodzbfbK18SSsY.CsIQPlxO" },
			{ "a", "$2a$06$m0CrhHm10qJ3lXRY.5zDGO",
					"$2a$06$m0CrhHm10qJ3lXRY.5zDGO3rS2KdeeWLuGmsfGlMfOxih58VYVfxe" },
			{ "a", "$2a$08$cfcvVd2aQ8CMvoMpP2EBfe",
					"$2a$08$cfcvVd2aQ8CMvoMpP2EBfeodLEkkFJ9umNEfPD18.hUF62qqlC/V." },
			{ "a", "$2a$10$k87L/MF28Q673VKh8/cPi.",
					"$2a$10$k87L/MF28Q673VKh8/cPi.SUl7MU/rWuSiIDDFayrKk/1tBsSQu4u" },
			{ "a", "$2a$12$8NJH3LsPrANStV6XtBakCe",
					"$2a$12$8NJH3LsPrANStV6XtBakCez0cKHXVxmvxIlcz785vxAIZrihHZpeS" },
			{ "abc", "$2a$06$If6bvum7DFjUnE9p2uDeDu",
					"$2a$06$If6bvum7DFjUnE9p2uDeDu0YHzrHM6tf.iqN8.yx.jNN1ILEf7h0i" },
			{ "abc", "$2a$08$Ro0CUfOqk6cXEKf3dyaM7O",
					"$2a$08$Ro0CUfOqk6cXEKf3dyaM7OhSCvnwM9s4wIX9JeLapehKK5YdLxKcm" },
			{ "abc", "$2a$10$WvvTPHKwdBJ3uk0Z37EMR.",
					"$2a$10$WvvTPHKwdBJ3uk0Z37EMR.hLA2W6N9AEBhEgrAOljy2Ae5MtaSIUi" },
			{ "abc", "$2a$12$EXRkfkdmXn2gzds2SSitu.",
					"$2a$12$EXRkfkdmXn2gzds2SSitu.MW9.gAVqa9eLS1//RYtYCmB1eLHg.9q" },
			{ "abcdefghijklmnopqrstuvwxyz", "$2a$06$.rCVZVOThsIa97pEDOxvGu",
					"$2a$06$.rCVZVOThsIa97pEDOxvGuRRgzG64bvtJ0938xuqzv18d3ZpQhstC" },
			{ "abcdefghijklmnopqrstuvwxyz", "$2a$08$aTsUwsyowQuzRrDqFflhge",
					"$2a$08$aTsUwsyowQuzRrDqFflhgekJ8d9/7Z3GV3UcgvzQW3J5zMyrTvlz." },
			{ "abcdefghijklmnopqrstuvwxyz", "$2a$10$fVH8e28OQRj9tqiDXs1e1u",
					"$2a$10$fVH8e28OQRj9tqiDXs1e1uxpsjN0c7II7YPKXua2NAKYvM6iQk7dq" },
			{ "abcdefghijklmnopqrstuvwxyz", "$2a$12$D4G5f18o7aMMfwasBL7Gpu",
					"$2a$12$D4G5f18o7aMMfwasBL7GpuQWuP3pkrZrOAnqP.bmezbMng.QwJ/pG" },
			{ "~!@#$%^&*()      ~!@#$%^&*()PNBFRD", "$2a$06$fPIsBO8qRqkjj273rfaOI.",
					"$2a$06$fPIsBO8qRqkjj273rfaOI.HtSV9jLDpTbZn782DC6/t7qT67P6FfO" },
			{ "~!@#$%^&*()      ~!@#$%^&*()PNBFRD", "$2a$08$Eq2r4G/76Wv39MzSX262hu",
					"$2a$08$Eq2r4G/76Wv39MzSX262huzPz612MZiYHVUJe/OcOql2jo4.9UxTW" },
			{ "~!@#$%^&*()      ~!@#$%^&*()PNBFRD", "$2a$10$LgfYWkbzEvQ4JakH7rOvHe",
					"$2a$10$LgfYWkbzEvQ4JakH7rOvHe0y8pHKF9OaFgwUZ2q7W2FFZmZzJYlfS" },
			{ "~!@#$%^&*()      ~!@#$%^&*()PNBFRD", "$2a$12$WApznUOJfkEGSmYRfnkrPO",
					"$2a$12$WApznUOJfkEGSmYRfnkrPOr466oFDCaj4b6HY3EXGvfxm43seyhgC" } };

	/**
	 * Test method for 'BCrypt.hashPassword(String, String)'
	 */
	@Test
	public void hashPasswordIsOk() {
		for (int i = 0; i < test_vectors.length; i++) {
			String plain = test_vectors[i][0];
			String salt = test_vectors[i][1];
			String expected = test_vectors[i][2];
			String hashed = BCrypt.hashPassword(plain, salt);
			assertThat(expected).isEqualTo(hashed);
		}
	}

	/**
	 * Test method for 'BCrypt.generateSalt(int)'
	 */
	@Test
	public void generateSaltInt() {
		for (int i = 4; i <= 12; i++) {
			for (int j = 0; j < test_vectors.length; j += 4) {
				String plain = test_vectors[j][0];
				String salt = BCrypt.generateSalt(i);
				String hashed1 = BCrypt.hashPassword(plain, salt);
				String hashed2 = BCrypt.hashPassword(plain, hashed1);
				assertThat(hashed2).isEqualTo(hashed1);
			}
		}
	}

	/**
	 * Test method for 'BCrypt.generateSalt()'
	 */
	@Test
	public void generateSalt() {
		for (int i = 0; i < test_vectors.length; i += 4) {
			String plain = test_vectors[i][0];
			String salt = BCrypt.generateSalt();
			String hashed1 = BCrypt.hashPassword(plain, salt);
			String hashed2 = BCrypt.hashPassword(plain, hashed1);
			assertThat(hashed2).isEqualTo(hashed1);
		}
	}

	/**
	 * Test method for 'BCrypt.checkPassword(String, String)' expecting success
	 */
	@Test
	public void checkPasswordWithSuccess() {
		for (int i = 0; i < test_vectors.length; i++) {
			String plain = test_vectors[i][0];
			String expected = test_vectors[i][2];
			assertThat(BCrypt.checkPassword(plain, expected)).isTrue();
		}
	}

	/**
	 * Test method for 'BCrypt.checkPassword(String, String)' expecting failure
	 */
	@Test
	public void checkingPasswordFails() {
		for (int i = 0; i < test_vectors.length; i++) {
			int broken_index = (i + 4) % test_vectors.length;
			String plain = test_vectors[i][0];
			String expected = test_vectors[broken_index][2];
			assertThat(BCrypt.checkPassword(plain, expected)).isFalse();
		}
	}

	/**
	 * Test for correct hashing of non-US-ASCII passwords
	 */
	@Test
	public void testInternationalChars() {
		String pw1 = "ππππππππ";
		String pw2 = "????????";

		String h1 = BCrypt.hashPassword(pw1, BCrypt.generateSalt());
		assertThat(BCrypt.checkPassword(pw2, h1)).isFalse();

		String h2 = BCrypt.hashPassword(pw2, BCrypt.generateSalt());
		assertThat(BCrypt.checkPassword(pw1, h2)).isFalse();
	}

	@Test
	public void roundsForDoesNotOverflow() {
		assertThat(BCrypt.roundsForLogRounds(10)).isEqualTo(1024);
		assertThat(BCrypt.roundsForLogRounds(31)).isEqualTo(0x80000000L);
	}

	@Test(expected = IllegalArgumentException.class)
	public void emptyByteArrayCannotBeEncoded() {
		BCrypt.encode_base64(new byte[0], 0, new StringBuilder());
	}

	@Test(expected = IllegalArgumentException.class)
	public void moreBytesThanInTheArrayCannotBeEncoded() {
		BCrypt.encode_base64(new byte[1], 2, new StringBuilder());
	}

	@Test(expected = IllegalArgumentException.class)
	public void decodingMustRequestMoreThanZeroBytes() {
		BCrypt.decode_base64("", 0);
	}

	private static String encode_base64(byte d[], int len)
			throws IllegalArgumentException {
		StringBuilder rs = new StringBuilder();
		BCrypt.encode_base64(d, len, rs);
		return rs.toString();
	}

	@Test
	public void testBase64EncodeSimpleByteArrays() {
		assertThat(encode_base64(new byte[] { 0 }, 1)).isEqualTo("..");
		assertThat(encode_base64(new byte[] { 0, 0 }, 2)).isEqualTo("...");
		assertThat(encode_base64(new byte[] { 0, 0 , 0 }, 3)).isEqualTo("....");
	}

	@Test
	public void decodingCharsOutsideAsciiGivesNoResults() {
		byte[] ba = BCrypt.decode_base64("ππππππππ", 1);
		assertThat(ba.length).isEqualTo(0);
	}

	@Test
	public void decodingStopsWithFirstInvalidCharacter() {
		assertThat(BCrypt.decode_base64("....", 1).length).isEqualTo(1);
		assertThat(BCrypt.decode_base64(" ....", 1).length).isEqualTo(0);
	}

	@Test
	public void decodingOnlyProvidesAvailableBytes() {
		assertThat(BCrypt.decode_base64("", 1).length).isEqualTo(0);
		assertThat(BCrypt.decode_base64("......", 3).length).isEqualTo(3);
		assertThat(BCrypt.decode_base64("......", 4).length).isEqualTo(4);
		assertThat(BCrypt.decode_base64("......", 5).length).isEqualTo(4);
	}

	/**
	 * Encode and decode each byte value in each position.
	 */
	@Test
	public void testBase64EncodeDecode() {
		byte[] ba = new byte[3];

		for (int b = 0; b <= 0xFF; b++) {
			for (int i = 0; i < ba.length; i++) {
				Arrays.fill(ba, (byte) 0);
				ba[i] = (byte) b;

				String s = encode_base64(ba, 3);
				assertThat(s.length()).isEqualTo(4);

				byte[] decoded = BCrypt.decode_base64(s, 3);
				assertThat(decoded).isEqualTo(ba);
			}
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void genSaltFailsWithTooFewRounds() {
		BCrypt.generateSalt(3);
	}

	@Test(expected = IllegalArgumentException.class)
	public void genSaltFailsWithTooManyRounds() {
		BCrypt.generateSalt(32);
	}

	@Test
	public void genSaltGeneratesCorrectSaltPrefix() {
		assertThat(BCrypt.generateSalt(4).startsWith("$2a$04$")).isTrue();
		assertThat(BCrypt.generateSalt(31).startsWith("$2a$31$")).isTrue();
	}

	@Test(expected = IllegalArgumentException.class)
	public void hashpwFailsWhenSaltIsNull() {
		BCrypt.hashPassword("password", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void hashpwFailsWhenSaltSpecifiesTooFewRounds() {
		BCrypt.hashPassword("password", "$2a$03$......................");
	}

	@Test(expected = IllegalArgumentException.class)
	public void hashpwFailsWhenSaltSpecifiesTooManyRounds() {
		BCrypt.hashPassword("password", "$2a$32$......................");
	}

	@Test(expected = IllegalArgumentException.class)
	public void saltLengthIsChecked() {
		BCrypt.hashPassword("", "");
	}

	@Test
	public void hashpwWorksWithOldRevision() {
		assertThat(BCrypt.hashPassword("password", "$2$05$......................")).isEqualTo(
				"$2$05$......................bvpG2UfzdyW/S0ny/4YyEZrmczoJfVm");
	}

	@Test
	public void equalsOnStringsIsCorrect() {
		assertThat(BCrypt.equalsNoEarlyReturn("", "")).isTrue();
		assertThat(BCrypt.equalsNoEarlyReturn("test", "test")).isTrue();

		assertThat(BCrypt.equalsNoEarlyReturn("test", "")).isFalse();
		assertThat(BCrypt.equalsNoEarlyReturn("", "test")).isFalse();

		assertThat(BCrypt.equalsNoEarlyReturn("test", "pass")).isFalse();
	}
}
