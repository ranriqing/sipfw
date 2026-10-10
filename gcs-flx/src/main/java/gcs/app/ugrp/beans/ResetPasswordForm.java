package gcs.app.ugrp.beans;

import lombok.Data;

@Data
public class ResetPasswordForm {
    private Long userId;
    private String newPassword;
    private String remark;
}
