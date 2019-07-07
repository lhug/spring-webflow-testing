package de.lhug.webflowtester.helper;

import org.springframework.binding.message.MessageBuilder;
import org.springframework.binding.validation.ValidationContext;

public class BeanModelValidator {

    public void validate(BeanModel model, ValidationContext ctx) {
        int amount = model.getAmount();
        if (amount < 0) {
            ctx.getMessageContext().addMessage(new MessageBuilder().error().code("amount.tooLow").arg(amount).build());
        } else if (amount > 100) {
            ctx.getMessageContext().addMessage(new MessageBuilder().error().code("amount.tooHigh").arg(amount).build());
        }
    }
}
