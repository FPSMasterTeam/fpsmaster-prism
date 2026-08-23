package top.fpsmaster.prism.icon;

/** Stable resource keys shared by Nova's kebab-case and Edge's CamelCase module identities. */
public final class ModuleIcons {
    private ModuleIcons() {
    }

    public static String resource(String moduleId) {
        if (moduleId == null || moduleId.isEmpty()) {
            return "modules/client-settings";
        }
        StringBuilder normalized = new StringBuilder(moduleId.length() + 8);
        for (int i = 0; i < moduleId.length(); i++) {
            char current = moduleId.charAt(i);
            if (!Character.isLetterOrDigit(current)) {
                appendDash(normalized);
                continue;
            }
            if (Character.isUpperCase(current) && i > 0) {
                char previous = moduleId.charAt(i - 1);
                char next = i + 1 < moduleId.length() ? moduleId.charAt(i + 1) : 0;
                if (Character.isLowerCase(previous) || Character.isDigit(previous)
                        || (Character.isUpperCase(previous) && Character.isLowerCase(next))) {
                    appendDash(normalized);
                }
            }
            normalized.append(Character.toLowerCase(current));
        }
        return "modules/" + normalized;
    }

    private static void appendDash(StringBuilder text) {
        if (text.length() > 0 && text.charAt(text.length() - 1) != '-') {
            text.append('-');
        }
    }
}
