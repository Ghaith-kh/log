package com.bank.logging.masking;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class DataMasker {

    private final List<MaskingRule> rules;

    public DataMasker() {
        this.rules = initializeDefaultRules();
    }

    public DataMasker(List<MaskingRule> customRules) {
        this.rules = initializeDefaultRules();
        if (customRules != null) {
            this.rules.addAll(customRules);
        }
    }

    public String mask(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }
        String result = input;
        for (MaskingRule rule : rules) {
            result = rule.pattern().matcher(result).replaceAll(rule.replacement());
        }
        return result;
    }

    private List<MaskingRule> initializeDefaultRules() {
        List<MaskingRule> rules = new ArrayList<>();

        // PAN - Card numbers (Visa, Mastercard, etc.)
        rules.add(new MaskingRule(
            Pattern.compile("\\b([3-6]\\d{5})\\d{4,9}(\\d{4})\\b"),
            "$1******$2"
        ));

        // PAN with separators
        rules.add(new MaskingRule(
            Pattern.compile("\\b([3-6]\\d{3})[- ]?(\\d{4})[- ]?(\\d{4})[- ]?(\\d{4})\\b"),
            "$1-****-****-$4"
        ));

        // IBAN
        rules.add(new MaskingRule(
            Pattern.compile("\\b([A-Z]{2}\\d{2})[A-Z0-9]{8,26}([A-Z0-9]{4})\\b"),
            "$1************$2"
        ));

        // Email
        rules.add(new MaskingRule(
            Pattern.compile("\\b([a-zA-Z0-9])[a-zA-Z0-9._%+-]*@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})\\b"),
            "$1***@$2"
        ));

        // French phone
        rules.add(new MaskingRule(
            Pattern.compile("\\b(\\+?33|0)([1-9])(\\d{2})(\\d{2})(\\d{2})(\\d{2})\\b"),
            "$1$2******$6"
        ));

        // CVV in context
        rules.add(new MaskingRule(
            Pattern.compile("(?i)(cvv|cvc|cvn)[\":\\s]*(\\d{3,4})"),
            "$1:***"
        ));

        return rules;
    }

    public record MaskingRule(Pattern pattern, String replacement) {}
}
