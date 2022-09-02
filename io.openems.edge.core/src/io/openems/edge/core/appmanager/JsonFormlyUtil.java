package io.openems.edge.core.appmanager;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.JsonUtils.JsonObjectBuilder;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Source https://formly.dev/examples/introduction.
 */
public class JsonFormlyUtil {

	private JsonFormlyUtil() {
	}

	/**
	 * Creates a JsonObject Formly Checkbox Builder for the given enum.
	 *
	 * @param <T>      the type of the enum
	 * @param property the enum property
	 * @return a {@link CheckboxBuilder}
	 */
	public static <T extends Enum<T>> CheckboxBuilder buildCheckbox(T property) {
		return new CheckboxBuilder(property);
	}

	/**
	 * Creates a JsonObject Formly Input Builder for the given enum.
	 *
	 * @param <T>      the type of the enum
	 * @param property the enum property
	 * @return a {@link InputBuilder}
	 */
	public static <T extends Enum<T>> InputBuilder buildInput(T property) {
		return new InputBuilder(property);
	}

	/**
	 * Creates a JsonObject Formly Select Builder for the given enum.
	 *
	 * @param <T>      the type of the enum
	 * @param property the enum property
	 * @return a {@link SelectBuilder}
	 */
	public static <T extends Enum<T>> SelectBuilder buildSelect(T property) {
		return new SelectBuilder(property);
	}

	/**
	 * Creates a JsonObject Formly Repeat Builder for the given enum.
	 *
	 * @param <T>      the type of the enum
	 * @param property the enum property
	 * @return a {@link RepeatBuilder}
	 */
	public static <T extends Enum<T>> RepeatBuilder buildRepeat(T property) {
		return new RepeatBuilder(property);
	}

	/**
	 * A Builder for a Formly field.
	 *
	 * <pre>
	 * {
	 * 	"key": "key",
	 * 	"type": "input",
	 * 	"templateOptions": {
	 * 		"label": "label",
	 * 		"required": true
	 * 	},
	 * 	"expressionProperties": {
	 * 		"templateOptions.required": "model.PROPERTY"
	 * 	},
	 * 	"hideExpression": "!model.PROPERTY",
	 * 	"defaultValue": "defaultValue"
	 * }
	 * </pre>
	 *
	 */
	private abstract static class FormlyBuilder<T extends FormlyBuilder<T>> {

		protected final JsonObject jsonObject = new JsonObject();
		protected final JsonObject templateOptions = new JsonObject();
		private JsonObject expressionProperties = null;

		private <PROPERTY extends Enum<PROPERTY>> FormlyBuilder(PROPERTY property) {
			this.setKey(property.name());
			this.setType(this.getType());
			this.setLabel(property.name());
		}

		private FormlyBuilder(DefaultEnum property) {
			this.setKey(property.name());
			this.setType(this.getType());
			this.setDefaultValue(property.getDefaultValue());
			this.setLabel(property.name());
		}

		private final T setType(String type) {
			this.jsonObject.addProperty("type", type);
			return this.getSelf();
		}

		public final T setKey(String key) {
			if (key != null) {
				this.jsonObject.addProperty("key", key);
			} else if (this.jsonObject.has("key")) {
				this.jsonObject.remove("key");
			}
			return this.getSelf();
		}

		public final T setDefaultValue(String defaultValue) {
			if (defaultValue != null) {
				this.jsonObject.addProperty("defaultValue", defaultValue);
			} else if (this.jsonObject.has("defaultValue")) {
				this.jsonObject.remove("defaultValue");
			}

			return this.getSelf();
		}

		public final T setDefaultValue(Boolean defaultValue) {
			if (defaultValue != null) {
				this.jsonObject.addProperty("defaultValue", defaultValue);
			} else if (this.jsonObject.has("defaultValue")) {
				this.jsonObject.remove("defaultValue");
			}

			return this.getSelf();
		}

		public final T setDefaultValue(Number defaultValue) {
			if (defaultValue != null) {
				this.jsonObject.addProperty("defaultValue", defaultValue);
			} else if (this.jsonObject.has("defaultValue")) {
				this.jsonObject.remove("defaultValue");
			}

			return this.getSelf();
		}

		public final T setDefaultValue(JsonElement defaultValue) {
			if (defaultValue != null) {
				this.jsonObject.add("defaultValue", defaultValue);
			} else if (this.jsonObject.has("defaultValue")) {
				this.jsonObject.remove("defaultValue");
			}

			return this.getSelf();
		}

		public final T setDefaultValueWithStringSupplier(Supplier<String> supplieDefaultValue) {
			return this.setDefaultValue(supplieDefaultValue.get());
		}

		public final T setDefaultValueWithBooleanSupplier(Supplier<Boolean> supplieDefaultValue) {
			return this.setDefaultValue(supplieDefaultValue.get());
		}

		public final T isRequired(boolean isRequired) {
			if (isRequired) {
				this.templateOptions.addProperty("required", isRequired);
			} else if (this.templateOptions.has("required")) {
				this.templateOptions.remove("required");
			}
			return this.getSelf();
		}

		public final T setLabel(String label) {
			if (label != null) {
				this.templateOptions.addProperty("label", label);
			} else if (this.templateOptions.has("label")) {
				this.templateOptions.remove("label");
			}
			return this.getSelf();
		}

		public final T setDescription(String description) {
			this.templateOptions.addProperty("description", description);
			return this.getSelf();
		}

		/**
		 * Call a method on a FormlyBuilder if the expression is true.
		 *
		 * @param expression the expression
		 * @param consumer   allows a lambda function on {@link FormlyBuilder}
		 * @return the {@link JsonObjectBuilder}
		 */
		public T onlyIf(boolean expression, Consumer<T> consumer) {
			if (expression) {
				consumer.accept(this.getSelf());
			}
			return this.getSelf();
		}

		public final <PROPERTEY extends Enum<PROPERTEY>> T onlyShowIfChecked(PROPERTEY property) {
			this.getExpressionProperties().addProperty("templateOptions.required", "model." + property.name());
			this.jsonObject.addProperty("hideExpression", "!model." + property.name());
			return this.getSelf();
		}

		public final <PROPERTEY extends Enum<PROPERTEY>> T onlyShowIfNotChecked(PROPERTEY property) {
			this.getExpressionProperties().addProperty("templateOptions.required", "!model." + property.name());
			this.jsonObject.addProperty("hideExpression", "model." + property.name());
			return this.getSelf();
		}

		public JsonObject build() {
			this.jsonObject.add("templateOptions", this.templateOptions);
			if (this.expressionProperties != null && this.expressionProperties.size() > 0) {
				this.jsonObject.add("expressionProperties", this.expressionProperties);
			}
			return this.jsonObject;
		}

		protected abstract String getType();

		protected final JsonObject getExpressionProperties() {
			if (this.expressionProperties == null) {
				this.expressionProperties = new JsonObject();
			}
			return this.expressionProperties;
		}

		@SuppressWarnings("unchecked")
		private final T getSelf() {
			return (T) this;
		}

	}

	/**
	 * A Builder for a Formly Input.
	 *
	 * <pre>
	 * {
	 * 	"key": "key",
	 * 	"type": "input",
	 * 	"templateOptions": {
	 * 		"type": "number",
	 * 		"label": "label",
	 * 		"placeholder": "placeholder",
	 * 		"required": true,
	 * 		"min": 0,
	 * 		"max": 100,
	 * 		"minLenght": 6,
	 * 		"maxLenght": 18,
	 * 		"pattern": /(\d{1,3}\.){3}\d{1,3}/
	 * 	},
	 * 	"validation": {
	 * 		"messages": {
	 * 			"pattern": "Input is not a valid IP Address!",
	 * 		},
	 * 	},
	 * 	"expressionProperties": {
	 * 		"templateOptions.required": "model.PROPERTY"
	 * 	},
	 * 	"hideExpression": "!model.PROPERTY",
	 * 	"defaultValue": "defaultValue"
	 * }
	 * </pre>
	 *
	 */
	public static final class InputBuilder extends FormlyBuilder<InputBuilder> {

		public static enum Type {
			TEXT("text"), //
			PASSWORD("password"), //
			NUMBER("number"), //
			;

			private String formlyTypeName;

			private Type(String type) {
				this.formlyTypeName = type;
			}

			public String getFormlyTypeName() {
				return this.formlyTypeName;
			}
		}

		public static enum Validation {
			IP("(\\d{1,3}\\.){3}\\d{1,3}", "Input is not a valid IP Address!"), //
			;

			private String pattern;
			private String errorMsg;

			private Validation(String pattern, String errorMsg) {
				this.pattern = pattern;
				this.errorMsg = errorMsg;
			}

			public String getErrorMsg() {
				return this.errorMsg;
			}

			public String getPattern() {
				return this.pattern;
			}

		}

		private JsonObject validation = null;
		private Type type = Type.TEXT;

		private <PROPERTY extends Enum<PROPERTY>> InputBuilder(PROPERTY property) {
			super(property);
		}

		private InputBuilder(DefaultEnum property) {
			super(property);
		}

		/**
		 * Sets the type of the input.
		 *
		 * <p>
		 * Default: {@link Type#TEXT}
		 *
		 * @param type to be set
		 * @return this
		 */
		public InputBuilder setInputType(Type type) {
			this.type = type;
			return this;
		}

		public InputBuilder setPlaceholder(String placeholder) {
			if (placeholder != null && !placeholder.isBlank()) {
				this.templateOptions.addProperty("placeholder", placeholder);
			} else if (this.templateOptions.has("placeholder")) {
				this.templateOptions.remove("placeholder");
			}
			return this;
		}

		/**
		 * Sets the min value of the input.
		 *
		 * @param min the min number that can be set
		 * @return this
		 * @throws IllegalArgumentException if the type is not set to number
		 */
		public InputBuilder setMin(int min) {
			if (this.type != Type.NUMBER) {
				throw new IllegalArgumentException("Value min can only be set on Number inputs!");
			}
			this.templateOptions.addProperty("min", min);
			return this;
		}

		/**
		 * Sets the max value of the input.
		 *
		 * @param max the max number that can be set
		 * @return this
		 * @throws IllegalArgumentException if the type is not set to number
		 */
		public InputBuilder setMax(int max) {
			if (this.type != Type.NUMBER) {
				throw new IllegalArgumentException("Value max can only be set on Number inputs!");
			}
			this.templateOptions.addProperty("max", max);
			return this;
		}

		/**
		 * Sets the minLength of the input.
		 *
		 * @param minLength the min length the input needs
		 * @return this
		 * @throws IllegalArgumentException if the type is not set to password or text
		 */
		public InputBuilder setMinLenght(int minLength) {
			if (this.type == Type.NUMBER) {
				throw new IllegalArgumentException("Value minLength can only be set on Password or Text inputs!");
			}
			this.templateOptions.addProperty("minLength", minLength);
			return this;
		}

		/**
		 * Sets the minLength of the input.
		 *
		 * @param maxLength the max length the input needs
		 * @return this
		 * @throws IllegalArgumentException if the type is not set to password or text
		 */
		public InputBuilder setMaxLenght(int maxLength) {
			if (this.type == Type.NUMBER) {
				throw new IllegalArgumentException("Value maxLength can only be set on Password or Text inputs!");
			}
			this.templateOptions.addProperty("maxLength", maxLength);
			return this;
		}

		/**
		 * Sets the validation of the Input.
		 * <p>
		 * e. g. to set the validation of an IP use {@link Validation#IP}
		 * </p>
		 *
		 * @param validation the validation to be set
		 * @return this
		 */
		public InputBuilder setValidation(Validation validation) {
			this.setPattern(validation.getPattern());
			this.setValidationMessage("pattern", validation.getErrorMsg());
			return this;
		}

		private InputBuilder setPattern(String pattern) {
			if (this.type != Type.TEXT) {
				throw new IllegalArgumentException("Pattern can only be set on Text inputs!");
			}
			this.templateOptions.addProperty("pattern", pattern);
			this.setValidationMessage("pattern", "Input is not a valid IP Address!");
			return this;
		}

		private InputBuilder setValidationMessage(String field, String msg) {
			var validatonObject = this.getValidation();
			var messages = validatonObject.get("messages");
			if (messages == null) {
				messages = new JsonObject();
				validatonObject.add("messages", messages);
			}
			JsonObject messagesObject;
			try {
				messagesObject = JsonUtils.getAsJsonObject(messages);
				if (msg == null) {
					messagesObject.remove(field);
				} else {
					messagesObject.addProperty(field, msg);
				}
			} catch (OpenemsNamedException e) {
				e.printStackTrace();
			}
			return this;
		}

		@Override
		protected String getType() {
			return "input";
		}

		@Override
		public JsonObject build() {
			if (this.type != Type.TEXT) {
				this.templateOptions.addProperty("type", this.type.getFormlyTypeName());
			}
			if (this.validation != null && this.validation.size() > 0) {
				this.jsonObject.add("validation", this.validation);
			}
			return super.build();
		}

		protected final JsonObject getValidation() {
			if (this.validation == null) {
				this.validation = new JsonObject();
			}
			return this.validation;
		}

	}

	/**
	 * A Builder for a Formly Checkbox.
	 *
	 * <pre>
	 * {
	 * 	"key": "key",
	 * 	"type": "checkbox",
	 * 	"templateOptions": {
	 * 		"label": "label",
	 * 		"required": true
	 * 	},
	 * 	"expressionProperties": {
	 * 		"templateOptions.required": "model.PROPERTY"
	 * 	},
	 * 	"hideExpression": "!model.PROPERTY",
	 * 	"defaultValue": "defaultValue"
	 * }
	 * </pre>
	 *
	 */
	public static final class CheckboxBuilder extends FormlyBuilder<CheckboxBuilder> {

		private <PROPERTY extends Enum<PROPERTY>> CheckboxBuilder(PROPERTY property) {
			super(property);
		}

		private CheckboxBuilder(DefaultEnum property) {
			super(property);
		}

		@Override
		protected String getType() {
			return "checkbox";
		}

	}

	/**
	 * A Builder for a Formly Select.
	 *
	 * <pre>
	 * {
	 * 	"key": "key",
	 * 	"type": "select",
	 * 	"templateOptions": {
	 * 		"label": "label",
	 * 		"required": true,
	 * 		"multiple": true,
	 * 		"options": [
	 * 			{
	 * 				"label": "label",
	 * 				"value": "value"
	 * 			}, ...
	 * 		]
	 * 	},
	 * 	"expressionProperties": {
	 * 		"templateOptions.required": "model.PROPERTY"
	 * 	},
	 * 	"hideExpression": "!model.PROPERTY",
	 * 	"defaultValue": "defaultValue"
	 * }
	 * </pre>
	 *
	 */
	public static final class SelectBuilder extends FormlyBuilder<SelectBuilder> {

		public static final Function<OpenemsComponent, String> DEFAULT_COMPONENT_2_LABEL = t -> t.alias() == null
				|| t.alias().isEmpty() ? t.id() : t.id() + ": " + t.alias();
		public static final Function<OpenemsComponent, String> DEFAULT_COMPONENT_2_VALUE = OpenemsComponent::id;

		private <PROPERTY extends Enum<PROPERTY>> SelectBuilder(PROPERTY property) {
			super(property);
		}

		private SelectBuilder(DefaultEnum property) {
			super(property);
		}

		public SelectBuilder setOptions(JsonArray options) {
			this.templateOptions.add("options", options);
			return this;
		}

		/**
		 * Note the {@link Map#entry(Object, Object)} does not return a
		 * {@link Comparable} Object so the {@link Set} can not be a {@link TreeSet}.
		 *
		 * @param items the options
		 * @return this
		 */
		public SelectBuilder setOptions(Set<Entry<String, String>> items) {
			return this.setOptions(items, t -> t, t -> t);
		}

		public <T, C> SelectBuilder setOptions(Set<Entry<T, C>> items, Function<T, String> item2Label,
				Function<C, String> item2Value) {
			var options = JsonUtils.buildJsonArray();
			items.stream().forEach(t -> {
				options.add(JsonUtils.buildJsonObject() //
						.addProperty("label", item2Label.apply(t.getKey())) //
						.addProperty("value", item2Value.apply(t.getValue())) //
						.build());
			});
			return this.setOptions(options.build());
		}

		public SelectBuilder setOptions(List<String> items) {
			return this.setOptions(items, t -> t, t -> t);
		}

		public <T> SelectBuilder setOptions(List<? extends T> items, Function<T, String> item2Label,
				Function<T, String> item2Value) {
			var options = JsonUtils.buildJsonArray();
			for (var item : items) {
				options.add(JsonUtils.buildJsonObject() //
						.addProperty("label", item2Label.apply(item)) //
						.addProperty("value", item2Value.apply(item)) //
						.build());
			}
			return this.setOptions(options.build());
		}

		/**
		 * Sets if more than one options can be selected.
		 *
		 * @param isMulti if more options can be selected
		 * @return this
		 */
		public SelectBuilder isMulti(boolean isMulti) {
			if (isMulti) {
				this.templateOptions.addProperty("multiple", isMulti);
			} else if (this.templateOptions.has("multiple")) {
				this.templateOptions.remove("multiple");
			}
			return this;
		}

		@Override
		protected String getType() {
			return "select";
		}

	}

	/**
	 * A Builder for a Formly Checkbox.
	 *
	 * <pre>
	 * {
	 * 	"key": "key",
	 * 	"type": "repeat",
	 * 	"templateOptions": {
	 * 		"label": "label",
	 * 		"required": true
	 * 	},
	 * 	"expressionProperties": {
	 * 		"templateOptions.required": "model.PROPERTY"
	 * 	},
	 * 	"hideExpression": "!model.PROPERTY",
	 * 	"defaultValue": "defaultValue"
	 * }
	 * </pre>
	 *
	 */
	public static final class RepeatBuilder extends FormlyBuilder<RepeatBuilder> {

		private JsonObject fieldArray;

		private <PROPERTY extends Enum<PROPERTY>> RepeatBuilder(PROPERTY property) {
			super(property);
		}

		private RepeatBuilder(DefaultEnum property) {
			super(property);
		}

		public RepeatBuilder setAddText(String addText) {
			if (addText != null && !addText.isBlank()) {
				this.templateOptions.addProperty("addText", addText);
			} else if (this.templateOptions.has("addText")) {
				this.templateOptions.remove("addText");
			}
			return this;
		}

		public RepeatBuilder setFieldArray(JsonObject object) {
			this.fieldArray = object;
			return this;
		}

		@Override
		protected String getType() {
			return "repeat";
		}

		@Override
		public JsonObject build() {
			if (this.fieldArray != null) {
				this.jsonObject.add("fieldArray", this.fieldArray);
			}
			return super.build();
		}

	}

}
