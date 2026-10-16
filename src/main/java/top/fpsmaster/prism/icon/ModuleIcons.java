package top.fpsmaster.prism.icon;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Stable resource keys shared by Nova's kebab-case and Edge's CamelCase module identities. */
public final class ModuleIcons {
    /** Module ids are a small fixed set; ClickGUI asks for every visible module every frame. */
    private static final Map<String, String> CACHE = new ConcurrentHashMap<String, String>();

    private ModuleIcons() {
    }

    public static String resource(String moduleId) {
        if (moduleId == null || moduleId.isEmpty()) {
            return "modules/client-settings";
        }
        String cached = CACHE.get(moduleId);
        if (cached != null) {
            return cached;
        }
        String resource = compute(moduleId);
        CACHE.put(moduleId, resource);
        return resource;
    }

    private static String compute(String moduleId) {
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
