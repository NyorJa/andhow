package org.yarnandtail.andhow.property;

import java.util.List;
import org.yarnandtail.andhow.api.*;
import org.yarnandtail.andhow.valid.IntValidator;
import org.yarnandtail.andhow.valuetype.IntType;

/**
 * A Property that refers to an Integer value.
 * 
 * All the basic Java types use a three letter abv. to keep declaration lines
 * short, in the form of:  [Type]Prop
 * 
 * By default this uses the TrimToNullTrimmer, which removes all whitespace from
 * the value and ultimately null if the value is all whitespace.
 * 
 * @author eeverman
 */
public class IntProp extends PropertyBase<Integer> {
	
	public IntProp(
			Integer defaultValue, boolean required, String shortDesc, List<Validator<Integer>> validators,
			List<Name> aliases, PropertyType paramType, ValueType<Integer> valueType, Trimmer trimmer,
			String helpText) {
		
		super(defaultValue, required, shortDesc, validators, aliases, paramType, valueType, trimmer, helpText);
	}
	
	public static IntBuilder builder() {
		return new IntBuilder();
	}
	
	public static class IntBuilder extends PropertyBuilderBase<IntBuilder, IntProp, Integer> {

		public IntBuilder() {
			instance = this;
			valueType(IntType.instance());
			trimmer(TrimToNullTrimmer.instance());
		}

		@Override
		public IntProp build() {

			return new IntProp(_defaultValue, _nonNull, _desc, _validators,
				_aliases, PropertyType.SINGLE_NAME_VALUE, _valueType, _trimmer, _helpText);

		}
		
		public IntBuilder mustBeGreaterThan(int reference) {
			validation(new IntValidator.GreaterThan(reference));
			return instance;
		}
		
		public IntBuilder mustBeGreaterThanOrEqualTo(int reference) {
			validation(new IntValidator.GreaterThanOrEqualTo(reference));
			return instance;
		}
		
		public IntBuilder mustBeLessThan(int reference) {
			validation(new IntValidator.LessThan(reference));
			return instance;
		}
		
		public IntBuilder mustBeLessThanOrEqualTo(int reference) {
			validation(new IntValidator.LessThanOrEqualTo(reference));
			return instance;
		}

	}

}
