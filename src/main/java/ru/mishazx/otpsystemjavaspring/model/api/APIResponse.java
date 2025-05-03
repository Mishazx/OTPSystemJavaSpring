package ru.mishazx.otpsystemjavaspring.model.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class APIResponse<T> {
    private String status;
    private String msg;
    private T data;
}
