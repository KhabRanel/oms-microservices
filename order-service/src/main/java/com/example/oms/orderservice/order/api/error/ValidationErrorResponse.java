package com.example.oms.orderservice.order.api.error;

import java.util.List;

public class ValidationErrorResponse {

    private String error;
    private List<FieldValidationError> fields;

    public ValidationErrorResponse(String error, List<FieldValidationError> fields) {
        this.error = error;
        this.fields = fields;
    }

    public String getError() {
        return error;
    }

    public List<FieldValidationError> getFields() {
        return fields;
    }
}
