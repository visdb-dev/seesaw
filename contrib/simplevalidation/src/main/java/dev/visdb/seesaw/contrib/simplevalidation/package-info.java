//package dev.visdb.seesaw.contrib.simplevalidation;

/**
 * This package makes
 * <a href="https://github.com/timboudreau/simplevalidation">Simple Validation</a>
 * available through the seesaw validation/decoration framework.
 * <p>
 * {@code SimpleValidation}
 * is a library for validation and decoration of swing components.
 * There are many builtin validators, see
 * {@link org.netbeans.validation.api.builtin.stringvalidation.StringValidators}.
 * For example URL_MUST_BE_VALID, EMAIL_ADDESS, REQUIRE_NON_NEGATIVE_NUMBER,
 * FILE_MUST_EXIST, disallowChars(char[] chars). Note that validators may be
 * chained, see
 * {@link org.netbeans.validation.api.ValidatorUtils#merge(org.netbeans.validation.api.Validator...) }.
 * <p>
 * Example
 * <p>
 * <img src="doc-files/SimpleValidation.png" alt="Simple Validation image"
 * style="display: inline-block; margin-left: 40px;"/>
 * <p>
 * Here is a complete example
 * {@snippet lang="java" class=SimpleValidation region=validation_example_import}
 * {@snippet lang="java" class=SimpleValidation region=validation_example}
 */

package dev.visdb.seesaw.contrib.simplevalidation;

//public class package_info {
//}

// vi: sw=2 ts=8