package com.snapshop.service.importing;

/**
 * The group a row belongs to under the formula-word rule ("กลุ่ม R").
 *
 * @param label used only inside the file (R001…); never stored
 * @param name  the group name; the key that matches rule groups between import rounds
 */
public record RuleGroup(String label, String name) {
}
