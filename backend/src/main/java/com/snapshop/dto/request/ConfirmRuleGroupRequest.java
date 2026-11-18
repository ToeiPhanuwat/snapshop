package com.snapshop.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Body of POST /api/admin/rule-groups/{code}/confirm.
 *
 * @param mergeTypeCodes the name groups (กลุ่ม E) the admin ticked to merge into the rule group; may be empty
 */
public record ConfirmRuleGroupRequest(@NotNull List<@NotNull String> mergeTypeCodes) {
}
